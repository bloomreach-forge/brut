package org.bloomreach.forge.brut.resources.bootstrap;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.bloomreach.forge.brut.common.project.ProjectSettings;
import org.bloomreach.forge.brut.resources.diagnostics.ConfigurationDiagnostics;
import org.bloomreach.forge.brut.resources.diagnostics.DiagnosticResult;
import org.bloomreach.forge.brut.resources.diagnostics.DiagnosticSeverity;
import org.onehippo.cm.engine.ConfigurationConfigService;
import org.onehippo.cm.engine.JcrContentProcessor;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.exceptions.MissingDependencyException;
import org.onehippo.cm.model.parser.ModuleReader;
import org.onehippo.cm.model.source.ContentSource;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bootstrap strategy using brXM's ConfigurationConfigService.
 * <p>
 * This strategy uses the same configuration service that brXM uses in production,
 * ensuring test environments match production structure exactly. This eliminates
 * manual JCR node construction and reduces maintenance burden.
 * <p>
 * <strong>Note:</strong> This strategy uses reflection to access package-private methods
 * in ConfigurationConfigService. This is necessary because brXM does not expose these
 * methods publicly, but is consistent with BRUT's existing use of reflection elsewhere.
 * <p>
 * <strong>Requirements:</strong>
 * <ul>
 *   <li>hcm-module.yaml must exist on classpath (META-INF/hcm-module.yaml)</li>
 *   <li>Module descriptor must reference hcm-config and hcm-content sources</li>
 * </ul>
 * <p>
 * <strong>Benefits over manual construction:</strong>
 * <ul>
 *   <li>Automatic sync with brXM changes - no manual updates needed</li>
 *   <li>Uses production bootstrap flow - same as real CMS</li>
 *   <li>Eliminates custom YAML parsers - leverages brXM's parser</li>
 *   <li>Version resilient - brXM changes propagate automatically</li>
 * </ul>
 *
 * @see ManualBootstrapStrategy
 * @since 5.2.0
 */
public class ConfigServiceBootstrapStrategy implements JcrBootstrapStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigServiceBootstrapStrategy.class);
    private static final JcrContentProcessor CONTENT_PROCESSOR = new JcrContentProcessor();

    /**
     * JVM-scoped cache of built ConfigurationModels keyed by a fingerprint of the module
     * descriptor paths and their last-modified timestamps.
     * <p>
     * The ConfigurationModelImpl is immutable after {@code build()}, so it is safe to share
     * across test classes within the same JVM run. This eliminates repeated YAML parsing and
     * model construction when multiple test classes share the same HCM modules.
     * <p>
     * Call {@link #clearModelCache()} in tests that modify module descriptors on disk.
     */
    static final ConcurrentHashMap<String, LoadedModules> MODEL_CACHE = new ConcurrentHashMap<>();

    /**
     * Clears the model cache. Intended for test isolation when module descriptors change.
     */
    static void clearModelCache() {
        MODEL_CACHE.clear();
    }
    private static final String HCM_MODULE_DESCRIPTOR = "META-INF/hcm-module.yaml";
    private static final String MINIMAL_FRAMEWORK_MODULE_RESOURCE =
        "org/bloomreach/forge/brut/resources/config-service/minimal-framework/hcm-module.yaml";
    private static final String MINIMAL_SELECTION_CND_RESOURCE =
        "org/bloomreach/forge/brut/resources/config-service/minimal-framework/hcm-config/namespaces/selection.cnd";
    private static final String FRONTEND_CONFIG_ROOT = "/hippo:configuration/hippo:frontend";
    private static final String MODULES_CONFIG_ROOT = "/hippo:configuration/hippo:modules";
    private static final String TRANSLATIONS_CONFIG_ROOT = "/hippo:configuration/hippo:translations";
    private static final String ALLOWED_CONFIG_ROOTS_PROPERTY = "brut.configservice.allowedConfigRoots";
    private static final String ALLOWED_CONTENT_SOURCE_ROOTS_PROPERTY =
        "brut.configservice.allowedContentSourceRoots";
    private static final String PRUNE_FRONTEND_CONFIG_PROPERTY = "brut.configservice.pruneFrontendConfig";
    private static final String PRUNE_CONFIG_ROOTS_PROPERTY = "brut.configservice.pruneConfigRoots";
    private static final int MAX_ITEM_EXISTS_RETRIES = 10;
    private static final int MAX_STUB_RETRIES = 5;
    private static final String STUB_MISSING_NAMESPACES_PROPERTY = "brut.configservice.stubMissingNamespaces";
    private static final String STUB_MISSING_NODE_TYPES_PROPERTY = "brut.configservice.stubMissingNodeTypes";
    private static final List<String> DEFAULT_ALLOWED_CONFIG_ROOTS = List.of(
        "/hst:",
        "/content",
        "/webfiles",
        "/hippo:namespaces"
    );
    private static final List<String> DEFAULT_ALLOWED_CONTENT_SOURCE_ROOTS = List.of(
        "content",
        "hst"
    );
    private static final List<String> DEFAULT_SAFE_ROOT_PREFIXES = List.of(
        "/hst:",
        "/content",
        "/webfiles",
        "/hippo:namespaces"
    );
    private static final List<String> DEFAULT_STUB_GROUPS = List.of(
        "hippo-cms",
        "hippo-repository",
        "hippo-services",
        "hippo-essentials"
    );

    @Override
    public void initializeHstStructure(Session session, String projectNamespace,
                                      BootstrapContext context) throws RepositoryException {
        LOG.debug("ConfigService bootstrap starting for project: {}", projectNamespace);

        try {
            LoadedModules loadedModules = loadModulesExplicitly(context);
            ConfigurationModelImpl configModel = loadedModules.model;
            List<ModuleImpl> modules = loadedModules.modules;

            int moduleCount = 0;
            for (Object ignored : configModel.getModules()) {
                moduleCount++;
            }
            LOG.debug("Loaded {} HCM module(s)", moduleCount);
            if (moduleCount == 0) {
                throw new RepositoryException("No HCM modules found for ConfigService bootstrap. " +
                    "Ensure hcm-module.yaml is available on the classpath or filesystem.");
            }

            ConfigurationModelImpl baseline;
            try (ConfigurationModelImpl emptyModel = new ConfigurationModelImpl()) {
                baseline = emptyModel.build();
            }

            ConfigurationConfigService configService = new ConfigurationConfigService();

            ConfigServiceReflectionBridge.invokeApplyNamespacesAndNodeTypes(configService, baseline, configModel, session);

            // Proactively refresh before computing the delta: the Jackrabbit repository.xml
            // bootstraps /hippo:configuration/* on startup, so there may be pending read-ahead
            // state that would cause the delta engine to see those nodes as "new" and throw
            // ItemExistsException. session.refresh(false) discards any uncommitted state and
            // ensures the delta engine sees the real repository state. The retry loop in
            // applyConfigDeltaWithRetries is a reactive fallback for any remaining conflicts.
            session.refresh(false);
            ConfigServiceReflectionBridge.applyConfigDeltaWithRetries(configService, baseline, configModel, session);

            ensureMandatoryConfigChildren(session);
            ensureSelectionNamespace(session);
            importContentDefinitions(modules, session);
            ensureProjectSpecificHstRoot(session, projectNamespace);
            ensurePreviewMounts(session, projectNamespace);
            logWorkspaceDiagnostics(session, projectNamespace);
            ensureMissingStateSummaries(session);
            session.save();

            LOG.info("ConfigService bootstrap completed for project: {}", projectNamespace);

        } catch (Exception e) {
            LOG.error("ConfigService bootstrap failed for project: {}", projectNamespace, e);

            // Add diagnostic logging
            DiagnosticResult diagnostic = ConfigurationDiagnostics.diagnoseConfigurationError(e);
            if (diagnostic.severity() == DiagnosticSeverity.ERROR) {
                LOG.error("\n{}", diagnostic);
            } else if (diagnostic.severity() == DiagnosticSeverity.WARNING) {
                LOG.warn("\n{}", diagnostic);
            }

            throw new RepositoryException("Failed to bootstrap using ConfigurationConfigService", e);
        }
    }

    private void ensureMissingStateSummaries(Session session) throws RepositoryException {
        if (!session.nodeExists("/hippo:namespaces")) {
            return;
        }

        Node root = session.getRootNode();
        addStateSummaryRecursively(root);
    }

    private void addStateSummaryRecursively(Node node) throws RepositoryException {
        if (!node.hasProperty("hippostd:stateSummary") && requiresStateSummary(node)) {
            node.setProperty("hippostd:stateSummary", "");
        }

        for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
            addStateSummaryRecursively(it.nextNode());
        }
    }

    private boolean requiresStateSummary(Node node) throws RepositoryException {
        if (hasMandatoryStateSummary(node.getPrimaryNodeType())) {
            return true;
        }
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if (hasMandatoryStateSummary(mixin)) {
                return true;
            }
        }
        return false;
    }

    private void ensurePreviewMounts(Session session, String projectNamespace) throws RepositoryException {
        if (session == null || projectNamespace == null || projectNamespace.isBlank()) {
            return;
        }
        String projectHostsPath = "/hst:" + projectNamespace + "/hst:hosts";
        if (!session.nodeExists(projectHostsPath)) {
            return;
        }
        Node hosts = session.getNode(projectHostsPath);
        int updated = markPreviewMountsRecursively(hosts);
        if (updated > 0) {
            LOG.info("Marked {} mount(s) as preview under {}", updated, projectHostsPath);
        }
    }

    private int markPreviewMountsRecursively(Node node) throws RepositoryException {
        int updated = 0;
        if (node.isNodeType("hst:mount")) {
            String currentType = node.hasProperty("hst:type")
                ? node.getProperty("hst:type").getString()
                : null;
            if (!"preview".equals(currentType)) {
                node.setProperty("hst:type", "preview");
                updated++;
            }
        }
        for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
            updated += markPreviewMountsRecursively(it.nextNode());
        }
        return updated;
    }

    private boolean hasMandatoryStateSummary(NodeType nodeType) {
        for (PropertyDefinition def : nodeType.getPropertyDefinitions()) {
            if (def.isMandatory() && "hippostd:stateSummary".equals(def.getName())) {
                return true;
            }
        }
        return false;
    }

    private void logWorkspaceDiagnostics(Session session, String projectNamespace) throws RepositoryException {
        if (session == null || projectNamespace == null || projectNamespace.isBlank()) {
            return;
        }
        String[] roots = {"/hst:hst", "/hst:" + projectNamespace};
        String base = "/hst:configurations/" + projectNamespace + "/hst:workspace";
        String[] paths = {
            base + "/hst:containers/homepage/main",
            base + "/hst:sitemenus/main",
            base + "/hst:channel/hst:channelinfo"
        };

        for (String root : roots) {
            for (String suffix : paths) {
                String fullPath = root + suffix;
                if (!session.nodeExists(fullPath)) {
                    LOG.debug("Workspace diagnostic: missing {}", fullPath);
                    continue;
                }
                Node node = session.getNode(fullPath);
                LOG.debug("Workspace diagnostic: {} (children: {})", fullPath, countChildNodes(node));
            }
        }
    }

    private int countChildNodes(Node node) throws RepositoryException {
        if (node == null) {
            return 0;
        }
        int count = 0;
        for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
            it.nextNode();
            count++;
        }
        return count;
    }

    private void ensureMandatoryConfigChildren(Session session) throws RepositoryException {
        if (!session.nodeExists("/hippo:configuration")) {
            return;
        }

        Node config = session.getNode("/hippo:configuration");
        ensureChildNode(config, "hippo:derivatives", "hipposys:derivativesfolder");
        ensureChildNode(config, "hippo:queries", "hipposys:queryfolder");
        ensureChildNode(config, "hippo:workflows", "hipposys:workflowfolder");
    }

    private void ensureChildNode(Node parent, String name, String primaryType) throws RepositoryException {
        if (!parent.hasNode(name)) {
            parent.addNode(name, primaryType);
            LOG.debug("Injected missing node {} under {}", name, parent.getPath());
        }
    }

    /**
     * Creates project-specific HST root (e.g., /hst:myproject) that mirrors /hst:hst structure.
     * <p>
     * In brXM, mount points reference paths like /hst:myproject/hst:sites/myproject while the
     * actual configuration lives under /hst:hst. This method creates the project-specific root
     * so mount point resolution works correctly.
     */
    private void ensureProjectSpecificHstRoot(Session session, String projectNamespace) throws RepositoryException {
        if (projectNamespace == null || projectNamespace.isBlank()) {
            return;
        }

        String projectHstRoot = "/hst:" + projectNamespace;
        String standardHstRoot = "/hst:hst";

        if (!session.nodeExists(standardHstRoot)) {
            LOG.warn("Standard HST root {} not found, cannot create project-specific root", standardHstRoot);
            return;
        }

        Node rootNode = session.getRootNode();
        Node projectRoot;
        if (session.nodeExists(projectHstRoot)) {
            projectRoot = session.getNode(projectHstRoot);
            LOG.info("Project HST root {} already exists; syncing from {}", projectHstRoot, standardHstRoot);
        } else {
            LOG.info("Creating project-specific HST root: {}", projectHstRoot);
            projectRoot = rootNode.addNode("hst:" + projectNamespace, "hst:hst");
        }

        Node standardRoot = session.getNode(standardHstRoot);

        JcrNodeSynchronizer.syncProjectSite(standardRoot, projectRoot, projectHstRoot);
        JcrNodeSynchronizer.syncProjectConfiguration(standardRoot, projectRoot, projectHstRoot);
        JcrNodeSynchronizer.syncProjectHosts(standardRoot, projectRoot, projectHstRoot);

        LOG.info("Project-specific HST root {} synced successfully", projectHstRoot);
    }

    @Override
    public boolean canHandle(BootstrapContext context) {
        if (context.getModuleDescriptors() != null && !context.getModuleDescriptors().isEmpty()) {
            LOG.debug("Module descriptors provided - ConfigService strategy can handle");
            return true;
        }

        try {
            Enumeration<URL> resources = context.getClassLoader().getResources(HCM_MODULE_DESCRIPTOR);
            boolean found = resources.hasMoreElements();

            if (found) {
                LOG.debug("Found hcm-module.yaml on classpath - ConfigService strategy can handle");
            } else {
                LOG.debug("No hcm-module.yaml found on classpath - ConfigService strategy cannot handle");
            }

            return found;
        } catch (IOException e) {
            LOG.warn("Error checking for hcm-module.yaml on classpath", e);
            return false;
        }
    }

    /**
     * Loads test HCM modules EXPLICITLY by path, avoiding classpath scanning.
     * <p>
     * This is the correct approach for test environments because:
     * <ul>
     *   <li>No classpath scanning = no framework module discovery</li>
     *   <li>No dependency conflicts from framework modules</li>
     *   <li>Explicit control over what loads</li>
     *   <li>Still uses ConfigService for production-identical JCR structure</li>
     * </ul>
     * <p>
     * We find test modules in target/test-classes or build/resources/test directories
     * and load them directly using ModuleReader, then let ConfigService write them to JCR.
     *
     * @param classLoader classloader to find test resource directories
     * @return ConfigurationModel with only test modules loaded
     * @throws Exception if error loading modules
     */
    private LoadedModules loadModulesExplicitly(BootstrapContext context) throws Exception {
        List<Path> moduleDescriptors = collectModuleDescriptors(context);

        if (moduleDescriptors.isEmpty()) {
            LOG.warn("No HCM modules found - returning empty model");
            return new LoadedModules(new ConfigurationModelImpl().build(), List.of());
        }

        String cacheKey = computeModelCacheKey(moduleDescriptors);
        LoadedModules cached = MODEL_CACHE.get(cacheKey);
        if (cached != null) {
            LOG.debug("ConfigurationModel cache hit ({} module(s)); skipping YAML parse and model build",
                moduleDescriptors.size());
            return cached;
        }

        LOG.info("Loading {} module(s) explicitly using ModuleReader (no classpath scanning)...",
            moduleDescriptors.size());

        Path startPath = Paths.get(System.getProperty("user.dir"));
        ProjectSettings settings = ProjectDiscovery.loadProjectSettings(startPath).orElse(null);
        String projectSiteName = stripHstRootPrefix(ProjectDiscovery.resolveHstRoot(startPath));
        String repositoryDataModule = settings != null && settings.getRepositoryDataModule() != null
            ? settings.getRepositoryDataModule()
            : "repository-data";
        Set<String> siteModuleNames = resolveSiteModuleNames(settings);

        ModuleReader moduleReader = new ModuleReader();
        List<ModuleImpl> modules = new ArrayList<>();

        addMinimalFrameworkModule(moduleReader, modules, context.getClassLoader());

        for (Path moduleDescriptor : moduleDescriptors) {
            if (!Files.exists(moduleDescriptor)) {
                LOG.warn("  Module descriptor not found: {}", moduleDescriptor);
                continue;
            }

            LOG.debug("  Loading module from: {}", moduleDescriptor);
            String moduleName = resolveRepositoryModuleName(moduleDescriptor, repositoryDataModule);
            String siteName = siteModuleNames.contains(moduleName) ? projectSiteName : null;
            if (siteName != null) {
                LOG.debug("  Using site '{}' for module at {}", siteName, moduleDescriptor);
            }
            ModuleImpl module = moduleReader.read(moduleDescriptor, false, siteName, null).getModule();
            modules.add(module);
            LOG.info("  Loaded module from: {}", moduleDescriptor);
        }

        List<String> allowedRoots = resolveAllowedConfigRoots();
        int filteredDefinitions = filterConfigDefinitionsByAllowedRoots(modules, allowedRoots);
        if (filteredDefinitions > 0) {
            LOG.info("Removed {} config definition(s) outside allowed roots {}. " +
                    "Set -D{} to adjust or '*' to disable filtering.",
                filteredDefinitions, allowedRoots, ALLOWED_CONFIG_ROOTS_PROPERTY);
        }

        ConfigurationModelImpl model = new ConfigurationModelImpl();
        Set<String> stubGroups = collectStubGroups(modules);
        addStubGroups(model, stubGroups);
        modules.forEach(model::addModule);

        LOG.debug("Building configuration model from {} module(s)...", modules.size());
        ConfigurationModelImpl builtModel = buildModelOnce(model, modules, stubGroups);

        LOG.debug("Successfully built model with explicit modules only (framework modules NOT scanned)");
        LoadedModules result = new LoadedModules(builtModel, List.copyOf(modules));
        MODEL_CACHE.put(cacheKey, result);
        return result;
    }

    private String computeModelCacheKey(List<Path> descriptors) {
        StringBuilder key = new StringBuilder();
        List<Path> sorted = new ArrayList<>(descriptors);
        sorted.sort(Comparator.naturalOrder());
        for (Path p : sorted) {
            Path abs = p.toAbsolutePath().normalize();
            long lastModified = 0L;
            try {
                lastModified = Files.getLastModifiedTime(abs).toMillis();
            } catch (IOException ignored) {
                // use 0 if unavailable
            }
            if (key.length() > 0) {
                key.append('|');
            }
            key.append(abs).append('@').append(lastModified);
        }
        return key.toString();
    }

    private Set<String> resolveSiteModuleNames(ProjectSettings settings) {
        Set<String> siteModules = new HashSet<>();
        if (settings != null) {
            String siteModule = settings.getSiteModule();
            if (siteModule != null && !siteModule.isBlank()) {
                siteModules.add(siteModule);
            }
            String developmentModule = settings.getDevelopmentSubModule();
            if (developmentModule != null && !developmentModule.isBlank()) {
                siteModules.add("site-" + developmentModule);
            }
        }
        siteModules.add("site");
        siteModules.add("site-development");
        return siteModules;
    }

    private void addMinimalFrameworkModule(ModuleReader moduleReader,
                                           List<ModuleImpl> modules,
                                           ClassLoader classLoader) throws Exception {
        if (moduleReader == null || modules == null) {
            return;
        }
        Path modulePath = resolveFrameworkModulePath(classLoader);
        if (modulePath == null) {
            LOG.debug("No minimal framework module resource found on classpath");
            return;
        }
        LOG.debug("Loading minimal framework module from: {}", modulePath);
        LOG.debug("Embedded minimal framework config is injected to satisfy core node types. " +
            "Consider defining required primary types in project config for full parity.");
        ModuleImpl module = moduleReader.read(modulePath, false).getModule();
        modules.add(module);
    }

    private Path resolveFrameworkModulePath(ClassLoader classLoader) throws Exception {
        ClassLoader effectiveLoader = classLoader != null ? classLoader : getClass().getClassLoader();
        URL resource = effectiveLoader.getResource(MINIMAL_FRAMEWORK_MODULE_RESOURCE);
        if (resource == null) {
            return null;
        }
        if ("file".equals(resource.getProtocol())) {
            return Paths.get(resource.toURI());
        }
        if ("jar".equals(resource.getProtocol())) {
            return copyFrameworkModuleFromJar(resource);
        }
        return null;
    }

    private Path copyFrameworkModuleFromJar(URL resource) throws Exception {
        String url = resource.toString();
        int separator = url.indexOf("!/");
        if (separator < 0) {
            return null;
        }
        URI jarUri = URI.create(url.substring(0, separator));
        String entryPath = url.substring(separator + 2);
        Path tempDir = Files.createTempDirectory("brut-minimal-framework");
        tempDir.toFile().deleteOnExit();
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, java.util.Map.of())) {
            Path sourceRoot = fs.getPath(entryPath).getParent();
            if (sourceRoot == null) {
                return null;
            }
            copyDirectory(sourceRoot, tempDir);
        }
        return tempDir.resolve("hcm-module.yaml");
    }

    private void copyDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        try (var stream = Files.walk(sourceRoot)) {
            for (Path source : (Iterable<Path>) stream::iterator) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative.toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    target.toFile().deleteOnExit();
                }
            }
        }
    }

    private String stripHstRootPrefix(String hstRoot) {
        if (hstRoot == null || hstRoot.isBlank()) {
            return hstRoot;
        }
        if (hstRoot.startsWith("/hst:")) {
            return hstRoot.substring("/hst:".length());
        }
        if (hstRoot.startsWith("hst:")) {
            return hstRoot.substring("hst:".length());
        }
        return hstRoot;
    }

    private String resolveRepositoryModuleName(Path moduleDescriptor, String repositoryDataModule) {
        if (moduleDescriptor == null || repositoryDataModule == null || repositoryDataModule.isBlank()) {
            return null;
        }
        Path normalized = moduleDescriptor.toAbsolutePath().normalize();
        for (int i = 0; i < normalized.getNameCount(); i++) {
            String segment = normalized.getName(i).toString();
            if (repositoryDataModule.equals(segment) && i + 1 < normalized.getNameCount()) {
                return normalized.getName(i + 1).toString();
            }
        }
        return null;
    }

    private List<Path> collectModuleDescriptors(BootstrapContext context) throws Exception {
        List<Path> descriptors = new ArrayList<>();
        if (context.getModuleDescriptors() != null) {
            for (Path path : context.getModuleDescriptors()) {
                if (path != null) {
                    descriptors.add(path.toAbsolutePath().normalize());
                }
            }
        }

        // If explicit module descriptors were provided, use only those (exclusive filter)
        if (!descriptors.isEmpty()) {
            LOG.info("Using {} explicitly specified module descriptor(s)", descriptors.size());
            return descriptors;
        }

        // Fall back to classpath scanning only if no explicit modules provided
        ClassLoader classLoader = context.getClassLoader();
        LOG.debug("Finding test module descriptors (target/test-classes or build/resources/test)...");

        Enumeration<URL> resources = classLoader.getResources(HCM_MODULE_DESCRIPTOR);
        while (resources.hasMoreElements()) {
            URL moduleUrl = resources.nextElement();
            if (moduleUrl.getProtocol().equals("file") &&
                (moduleUrl.getPath().contains("/target/test-classes/") ||
                 moduleUrl.getPath().contains("/build/resources/test/"))) {
                Path moduleDescriptorPath = Paths.get(moduleUrl.toURI());
                descriptors.add(moduleDescriptorPath.toAbsolutePath().normalize());
                LOG.info("Found test module descriptor at: {}", moduleDescriptorPath);
            } else {
                LOG.debug("Skipping framework/JAR module: {}", moduleUrl);
            }
        }

        return descriptors.stream().distinct().toList();
    }

    private Set<String> collectStubGroups(List<ModuleImpl> modules) {
        Set<String> stubGroups = new HashSet<>(DEFAULT_STUB_GROUPS);
        for (ModuleImpl module : modules) {
            if (module == null || module.getProject() == null) {
                continue;
            }
            GroupImpl group = module.getProject().getGroup();
            if (group == null) {
                continue;
            }
            if (group.getAfter() != null) {
                stubGroups.addAll(group.getAfter());
            }
        }

        return stubGroups;
    }

    private void addStubGroups(ConfigurationModelImpl model, Set<String> stubGroups) {
        if (stubGroups == null || stubGroups.isEmpty()) {
            return;
        }
        for (String groupName : stubGroups) {
            if (groupName == null || groupName.isBlank()) {
                continue;
            }
            GroupImpl group = new GroupImpl(groupName);
            ProjectImpl project = group.addProject(groupName + "-stub");
            project.addModule(groupName + "-stub");
            model.addGroup(group);
            LOG.debug("Added stub group '{}' to satisfy dependency ordering", groupName);
        }
    }

    private ConfigurationModelImpl buildModelOnce(ConfigurationModelImpl model,
                                                  List<ModuleImpl> modules,
                                                  Set<String> stubGroups) {
        try {
            return model.build();
        } catch (MissingDependencyException e) {
            MissingDependencyInfo info = parseMissingDependency(e.getMessage());
            if (info == null || info.missing == null) {
                throw e;
            }
            if (info.type == MissingDependencyType.GROUP) {
                if (stubGroups.contains(info.missing)) {
                    throw e;
                }
                LOG.warn("Missing group dependency '{}' detected; retrying with stub", info.missing);
                Set<String> nextStubs = new HashSet<>(stubGroups);
                nextStubs.add(info.missing);
                ConfigurationModelImpl retry = new ConfigurationModelImpl();
                addStubGroups(retry, nextStubs);
                modules.forEach(retry::addModule);
                return retry.build();
            }

            if (info.type == MissingDependencyType.MODULE) {
                ModuleImpl owner = findModuleByName(modules, info.owner);
                if (owner == null) {
                    throw e;
                }
                LOG.warn("Missing module dependency '{}' detected; retrying with stub in project '{}'",
                    info.missing, owner.getProject().getName());
                ConfigurationModelImpl retry = new ConfigurationModelImpl();
                addStubGroups(retry, stubGroups);
                ProjectImpl ownerProject = owner.getProject();
                ModuleImpl stubModule = ensureStubModule(ownerProject, info.missing);
                if (stubModule != null) {
                    retry.addModule(stubModule);
                }
                for (ModuleImpl module : modules) {
                    if (module != null && module.getProject() != ownerProject) {
                        retry.addModule(module);
                    }
                }
                return retry.build();
            }

            throw e;
        } catch (IllegalStateException e) {
            String unreachableRoot = extractUnreachableRoot(e.getMessage());
            String pruneRoot = resolvePruneRoot(unreachableRoot);
            if (pruneRoot != null && pruneConfigDefinitions(modules, pruneRoot)) {
                LOG.warn("Skipped config definitions under {} to allow delivery-tier tests to run. " +
                        "Set -D{}=false to disable pruning or -D{} to override roots.",
                    pruneRoot, PRUNE_FRONTEND_CONFIG_PROPERTY, PRUNE_CONFIG_ROOTS_PROPERTY);
                ConfigurationModelImpl retry = new ConfigurationModelImpl();
                addStubGroups(retry, stubGroups);
                modules.forEach(retry::addModule);
                return buildModelOnce(retry, modules, stubGroups);
            }
            throw e;
        }
    }

    private List<String> resolveAllowedConfigRoots() {
        String value = System.getProperty(ALLOWED_CONFIG_ROOTS_PROPERTY);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_ALLOWED_CONFIG_ROOTS;
        }
        String[] parts = value.split(",");
        List<String> roots = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                roots.add(trimmed);
            }
        }
        return roots.isEmpty() ? DEFAULT_ALLOWED_CONFIG_ROOTS : roots;
    }

    private int filterConfigDefinitionsByAllowedRoots(List<ModuleImpl> modules, List<String> allowedRoots) {
        if (modules == null || modules.isEmpty() || allowedRoots == null || allowedRoots.isEmpty()) {
            return 0;
        }
        if (allowedRoots.contains("*")) {
            return 0;
        }
        int removed = 0;
        for (ModuleImpl module : modules) {
            if (module == null) {
                continue;
            }
            for (ConfigSourceImpl source : module.getConfigSources()) {
                List<ConfigDefinitionImpl> definitions = new ArrayList<>(source.getConfigDefinitions());
                for (ConfigDefinitionImpl definition : definitions) {
                    if (definition == null || definition.getRootPath() == null) {
                        continue;
                    }
                    String rootPath = definition.getRootPath().toString();
                    if (!isAllowedConfigRoot(rootPath, allowedRoots)) {
                        source.removeDefinition(definition);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    private boolean isAllowedConfigRoot(String rootPath, List<String> allowedRoots) {
        if (rootPath == null) {
            return false;
        }
        for (String prefix : allowedRoots) {
            if (prefix == null || prefix.isBlank()) {
                continue;
            }
            if (rootPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldPruneFrontendConfig() {
        String value = System.getProperty(PRUNE_FRONTEND_CONFIG_PROPERTY);
        return value == null || Boolean.parseBoolean(value);
    }

    private String resolvePruneRoot(String unreachableRoot) {
        if (!shouldPruneFrontendConfig() || unreachableRoot == null) {
            return null;
        }
        for (String root : resolvePruneConfigRoots()) {
            if ("*".equals(root)) {
                return unreachableRoot;
            }
            if (unreachableRoot.startsWith(root)) {
                return root;
            }
        }
        if (!isExplicitPruneConfigRootsSet() && !isSafeRootPrefix(unreachableRoot)) {
            return unreachableRoot;
        }
        return null;
    }

    private boolean isExplicitPruneConfigRootsSet() {
        return System.getProperty(PRUNE_CONFIG_ROOTS_PROPERTY) != null;
    }

    private boolean isSafeRootPrefix(String root) {
        if (root == null) {
            return false;
        }
        for (String prefix : DEFAULT_SAFE_ROOT_PREFIXES) {
            if (root.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private List<String> resolvePruneConfigRoots() {
        String value = System.getProperty(PRUNE_CONFIG_ROOTS_PROPERTY);
        if (value == null) {
            return List.of(FRONTEND_CONFIG_ROOT, MODULES_CONFIG_ROOT, TRANSLATIONS_CONFIG_ROOT);
        }
        String[] parts = value.split(",");
        List<String> roots = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                roots.add(trimmed);
            }
        }
        return roots;
    }

    private String extractUnreachableRoot(String message) {
        if (message == null) {
            return null;
        }
        String marker = "unreachable node '";
        int start = message.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = message.indexOf('\'', start);
        if (end < 0) {
            return null;
        }
        return message.substring(start, end).trim();
    }

    private boolean pruneConfigDefinitions(List<ModuleImpl> modules, String rootPrefix) {
        if (modules == null || modules.isEmpty() || rootPrefix == null || rootPrefix.isBlank()) {
            return false;
        }
        int removed = 0;
        for (ModuleImpl module : modules) {
            if (module == null) {
                continue;
            }
            for (ConfigSourceImpl source : module.getConfigSources()) {
                List<ConfigDefinitionImpl> definitions = new ArrayList<>(source.getConfigDefinitions());
                for (ConfigDefinitionImpl definition : definitions) {
                    if (definition == null || definition.getRootPath() == null) {
                        continue;
                    }
                    String rootPath = definition.getRootPath().toString();
                    if (rootPath != null && rootPath.startsWith(rootPrefix)) {
                        source.removeDefinition(definition);
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            LOG.debug("Removed {} config definition(s) under {}", removed, rootPrefix);
        }
        return removed > 0;
    }

    private String extractMissingDependency(String message) {
        if (message == null) {
            return null;
        }
        int lastQuote = message.lastIndexOf('\'');
        if (lastQuote <= 0) {
            return null;
        }
        int prevQuote = message.lastIndexOf('\'', lastQuote - 1);
        if (prevQuote < 0) {
            return null;
        }
        return message.substring(prevQuote + 1, lastQuote).trim();
    }

    private MissingDependencyInfo parseMissingDependency(String message) {
        if (message == null) {
            return null;
        }
        if (message.startsWith("Group '")) {
            String missing = extractMissingDependency(message);
            return new MissingDependencyInfo(MissingDependencyType.GROUP, null, missing);
        }
        if (message.startsWith("Module '")) {
            int ownerStart = message.indexOf('\'') + 1;
            int ownerEnd = message.indexOf('\'', ownerStart);
            if (ownerStart <= 0 || ownerEnd <= ownerStart) {
                return null;
            }
            String owner = message.substring(ownerStart, ownerEnd);
            String missing = extractMissingDependency(message);
            return new MissingDependencyInfo(MissingDependencyType.MODULE, owner, missing);
        }
        return null;
    }

    private ModuleImpl findModuleByName(List<ModuleImpl> modules, String name) {
        if (modules == null || name == null) {
            return null;
        }
        for (ModuleImpl module : modules) {
            if (module != null && name.equals(module.getName())) {
                return module;
            }
        }
        return null;
    }

    private ModuleImpl ensureStubModule(ProjectImpl project, String moduleName) {
        if (project == null || moduleName == null || moduleName.isBlank()) {
            return null;
        }
        ModuleImpl existing = project.getModule(moduleName);
        if (existing != null) {
            return existing;
        }
        return project.addModule(moduleName);
    }

    private void importContentDefinitions(List<ModuleImpl> modules, Session session) throws RepositoryException {
        if (modules == null || modules.isEmpty()) {
            LOG.debug("No modules available for HCM content import");
            return;
        }

        int contentSources = 0;
        int moduleCount = 0;
        int removedSources = 0;
        List<String> allowedRoots = resolveAllowedContentSourceRoots();
        boolean allowAllContentSources = allowedRoots.contains("*");

        boolean stubNamespaces = Boolean.parseBoolean(
            System.getProperty(STUB_MISSING_NAMESPACES_PROPERTY, "true"));
        boolean stubNodeTypes = Boolean.parseBoolean(
            System.getProperty(STUB_MISSING_NODE_TYPES_PROPERTY, "true"));
        Set<String> stubbedNamespaces = new HashSet<>();
        Set<String> stubbedNodeTypes = new HashSet<>();

        for (ModuleImpl module : modules) {
            if (module == null || module.getContentSources() == null || module.getContentSources().isEmpty()) {
                continue;
            }
            moduleCount++;
            List<ContentSource> orderedSources = new ArrayList<>(module.getContentSources());
            orderedSources.sort(Comparator.comparingInt(this::contentPathDepth)
                .thenComparing(this::contentPathForSort));
            for (var contentSource : orderedSources) {
                if (contentSource == null) {
                    continue;
                }
                if (!allowAllContentSources && !isAllowedContentSource(contentSource, allowedRoots)) {
                    removedSources++;
                    continue;
                }
                ContentDefinitionImpl contentDefinition =
                    (ContentDefinitionImpl) contentSource.getContentDefinition();
                if (contentDefinition == null || contentDefinition.getNode() == null) {
                    continue;
                }
                contentSources++;
                importContentWithStubbing(contentDefinition, session, module.getName(),
                    stubNamespaces, stubNodeTypes, stubbedNamespaces, stubbedNodeTypes);
            }
        }

        if (contentSources > 0) {
            LOG.info("Imported {} HCM content source(s) from {} module(s)", contentSources, moduleCount);
        } else {
            LOG.debug("No HCM content sources found to import");
        }
        if (removedSources > 0) {
            LOG.info("Skipped {} HCM content source(s) outside allowed roots {}. " +
                    "Set -D{} to adjust or '*' to disable filtering.",
                removedSources, allowedRoots, ALLOWED_CONTENT_SOURCE_ROOTS_PROPERTY);
        }
    }

    private void importContentWithStubbing(ContentDefinitionImpl contentDef,
                                           Session session,
                                           String moduleName,
                                           boolean stubNamespaces,
                                           boolean stubNodeTypes,
                                           Set<String> stubbedNamespaces,
                                           Set<String> stubbedNodeTypes) throws RepositoryException {
        for (int attempt = 0; attempt < MAX_STUB_RETRIES; attempt++) {
            try {
                CONTENT_PROCESSOR.apply(contentDef.getNode(), ActionType.RELOAD, session);
                return;
            } catch (Exception e) {
                if (stubNamespaces) {
                    NamespaceException nsEx = findCause(e, NamespaceException.class);
                    if (nsEx != null) {
                        String prefix = RuntimeTypeStubber.extractNamespacePrefix(nsEx.getMessage());
                        if (prefix != null && stubbedNamespaces.add(prefix)) {
                            RuntimeTypeStubber.registerStubNamespace(session, prefix);
                            continue;
                        }
                    }
                }

                if (stubNodeTypes) {
                    NoSuchNodeTypeException ntEx = findCause(e, NoSuchNodeTypeException.class);
                    if (ntEx != null) {
                        String nodeType = RuntimeTypeStubber.extractNodeType(ntEx.getMessage());
                        if (nodeType != null && stubbedNodeTypes.add(nodeType)) {
                            RuntimeTypeStubber.registerStubNodeType(session, nodeType, stubbedNamespaces);
                            continue;
                        }
                    }
                }

                throw new RepositoryException(
                    "Failed to import HCM content for module '" + moduleName + "'", e);
            }
        }
        throw new RepositoryException(
            "Exceeded max stub retries importing content for module '" + moduleName + "'");
    }

    private <T extends Throwable> T findCause(Throwable error, Class<T> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private int contentPathDepth(ContentSource contentSource) {
        String path = contentPathForSort(contentSource);
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return 0;
        }
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }

    private String contentPathForSort(ContentSource contentSource) {
        if (contentSource == null || contentSource.getContentDefinition() == null) {
            return "";
        }
        var node = contentSource.getContentDefinition().getNode();
        if (node == null) {
            return "";
        }
        String path = node.getPath();
        return path == null ? "" : path;
    }

    private List<String> resolveAllowedContentSourceRoots() {
        String value = System.getProperty(ALLOWED_CONTENT_SOURCE_ROOTS_PROPERTY);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_ALLOWED_CONTENT_SOURCE_ROOTS;
        }
        String[] parts = value.split(",");
        List<String> roots = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                roots.add(trimmed);
            }
        }
        return roots.isEmpty() ? DEFAULT_ALLOWED_CONTENT_SOURCE_ROOTS : roots;
    }

    private boolean isAllowedContentSource(ContentSource contentSource, List<String> allowedRoots) {
        if (contentSource == null || allowedRoots == null || allowedRoots.isEmpty()) {
            return true;
        }
        if (allowedRoots.contains("*")) {
            return true;
        }
        String sourcePath = normalizeContentSourcePath(contentSource.getPath());
        String folderPath = normalizeContentSourcePath(contentSource.getFolderPath());
        String originPath = sourcePath.isEmpty() && folderPath.isEmpty()
            ? normalizeContentSourcePath(contentSource.getOrigin())
            : "";

        for (String root : allowedRoots) {
            String normalizedRoot = normalizeContentSourcePath(root);
            if (normalizedRoot.isEmpty()) {
                continue;
            }
            if (matchesContentRoot(sourcePath, normalizedRoot)
                || matchesContentRoot(folderPath, normalizedRoot)
                || matchesContentRoot(originPath, normalizedRoot)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeContentSourcePath(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean matchesContentRoot(String candidate, String root) {
        if (candidate == null || candidate.isEmpty() || root == null || root.isEmpty()) {
            return false;
        }
        if (candidate.equals(root) || candidate.startsWith(root + "/")) {
            return true;
        }
        String segment = "/" + root + "/";
        if (candidate.contains(segment)) {
            return true;
        }
        return candidate.endsWith("/" + root);
    }

    private void ensureSelectionNamespace(Session session) throws RepositoryException {
        if (session == null) {
            return;
        }
        try {
            session.getNamespaceURI("selection");
            return;
        } catch (NamespaceException ignored) {
            // Register minimal selection namespace and node types if missing.
        }

        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(MINIMAL_SELECTION_CND_RESOURCE)) {
            if (stream == null) {
                LOG.warn("Selection CND resource not found: {}", MINIMAL_SELECTION_CND_RESOURCE);
                return;
            }
            CndImporter.registerNodeTypes(new InputStreamReader(stream, StandardCharsets.UTF_8), session);
            LOG.info("Registered selection namespace from minimal framework CND");
        } catch (Exception e) {
            throw new RepositoryException("Failed to register selection namespace CND", e);
        }
    }

    private enum MissingDependencyType {
        GROUP,
        MODULE
    }

    private static final class LoadedModules {
        private final ConfigurationModelImpl model;
        private final List<ModuleImpl> modules;

        private LoadedModules(ConfigurationModelImpl model, List<ModuleImpl> modules) {
            this.model = model;
            this.modules = modules;
        }
    }

    private static final class MissingDependencyInfo {
        private final MissingDependencyType type;
        private final String owner;
        private final String missing;

        private MissingDependencyInfo(MissingDependencyType type, String owner, String missing) {
            this.type = type;
            this.owner = owner;
            this.missing = missing;
        }
    }
}
