package org.bloomreach.forge.brut.resources.bootstrap;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.bloomreach.forge.brut.common.project.ProjectSettings;
import org.onehippo.cm.engine.ConfigurationConfigService;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.exceptions.MissingDependencyException;
import org.onehippo.cm.model.parser.ModuleReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final String HCM_MODULE_DESCRIPTOR = "META-INF/hcm-module.yaml";
    private static final String MINIMAL_FRAMEWORK_MODULE_RESOURCE =
        "org/bloomreach/forge/brut/resources/config-service/minimal-framework/hcm-module.yaml";
    private static final List<String> DEFAULT_STUB_GROUPS = List.of(
        "hippo-cms",
        "hippo-repository",
        "hippo-services",
        "hippo-essentials"
    );

    @Override
    public void initializeHstStructure(Session session, String projectNamespace,
                                      BootstrapContext context) throws RepositoryException {
        LOG.info("========================================");
        LOG.info("Using ConfigurationConfigService bootstrap strategy");
        LOG.info("Project Namespace: {}", projectNamespace);
        LOG.info("========================================");

        try {
            // Load test modules EXPLICITLY by path (NO classpath scanning)
            // This avoids discovering framework modules that have dependency conflicts
            LOG.info("Loading test modules explicitly (no classpath scanning)...");
            ConfigurationModelImpl configModel = loadModulesExplicitly(context);

            int moduleCount = 0;
            for (Object ignored : configModel.getModules()) {
                moduleCount++;
            }
            LOG.info("Successfully loaded {} test module(s) explicitly", moduleCount);
            if (moduleCount == 0) {
                throw new RepositoryException("No HCM modules found for ConfigService bootstrap. " +
                    "Ensure hcm-module.yaml is available on the classpath or filesystem.");
            }

            // Create empty baseline (first boot scenario)
            LOG.debug("Creating empty baseline for first-boot scenario");
            ConfigurationModelImpl baseline;
            try (ConfigurationModelImpl emptyModel = new ConfigurationModelImpl()) {
                baseline = emptyModel.build();
            }

            // Apply configuration via ConfigService using reflection
            LOG.debug("Instantiating ConfigurationConfigService");
            ConfigurationConfigService configService = new ConfigurationConfigService();

            LOG.info("Applying namespaces and node types via reflection...");
            invokeApplyNamespacesAndNodeTypes(configService, baseline, configModel, session);

            LOG.info("Computing and writing configuration delta via reflection...");
            invokeComputeAndWriteDelta(configService, baseline, configModel, session, true);

            ensureMissingStateSummaries(session);
            ensureMandatoryConfigChildren(session);
            session.save();

            LOG.info("========================================");
            LOG.info("ConfigurationConfigService bootstrap completed successfully");
            LOG.info("HST configuration structure created via production code path");
            LOG.info("========================================");

        } catch (Exception e) {
            LOG.error("========================================");
            LOG.error("ConfigurationConfigService bootstrap FAILED");
            LOG.error("========================================", e);
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

    private boolean hasMandatoryStateSummary(NodeType nodeType) {
        for (PropertyDefinition def : nodeType.getPropertyDefinitions()) {
            if (def.isMandatory() && "hippostd:stateSummary".equals(def.getName())) {
                return true;
            }
        }
        return false;
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
            LOG.warn("Injected missing node {} under {}", name, parent.getPath());
        }
    }

    /**
     * Invokes package-private applyNamespacesAndNodeTypes method via reflection.
     * <p>
     * Method signature: void applyNamespacesAndNodeTypes(ConfigurationModel baseline,
     *                                                     ConfigurationModel update,
     *                                                     Session session)
     */
    private void invokeApplyNamespacesAndNodeTypes(ConfigurationConfigService configService,
                                                   ConfigurationModelImpl baseline,
                                                   ConfigurationModelImpl update,
                                                   Session session) throws Exception {
        try {
            Method method = ConfigurationConfigService.class.getDeclaredMethod(
                "applyNamespacesAndNodeTypes",
                org.onehippo.cm.model.ConfigurationModel.class,
                org.onehippo.cm.model.ConfigurationModel.class,
                Session.class
            );
            method.setAccessible(true);
            method.invoke(configService, baseline, update, session);
            method.setAccessible(false);

            LOG.debug("Successfully invoked applyNamespacesAndNodeTypes via reflection");
        } catch (Exception e) {
            LOG.error("Failed to invoke applyNamespacesAndNodeTypes via reflection", e);
            throw new RepositoryException("Reflection invocation failed for applyNamespacesAndNodeTypes", e);
        }
    }

    /**
     * Invokes package-private computeAndWriteDelta method via reflection.
     * <p>
     * Method signature: void computeAndWriteDelta(ConfigurationModel baseline,
     *                                             ConfigurationModel update,
     *                                             Session session,
     *                                             boolean forceApply)
     */
    private void invokeComputeAndWriteDelta(ConfigurationConfigService configService,
                                           ConfigurationModelImpl baseline,
                                           ConfigurationModelImpl update,
                                           Session session,
                                           boolean forceApply) throws Exception {
        try {
            Method method = ConfigurationConfigService.class.getDeclaredMethod(
                "computeAndWriteDelta",
                org.onehippo.cm.model.ConfigurationModel.class,
                org.onehippo.cm.model.ConfigurationModel.class,
                Session.class,
                boolean.class
            );
            method.setAccessible(true);
            method.invoke(configService, baseline, update, session, forceApply);
            method.setAccessible(false);

            LOG.debug("Successfully invoked computeAndWriteDelta via reflection");
        } catch (Exception e) {
            LOG.error("Failed to invoke computeAndWriteDelta via reflection", e);
            throw new RepositoryException("Reflection invocation failed for computeAndWriteDelta", e);
        }
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
    private ConfigurationModelImpl loadModulesExplicitly(BootstrapContext context) throws Exception {
        List<Path> moduleDescriptors = collectModuleDescriptors(context);

        if (moduleDescriptors.isEmpty()) {
            LOG.warn("No HCM modules found - returning empty model");
            return new ConfigurationModelImpl().build();
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

            LOG.info("  Loading module from: {}", moduleDescriptor);
            String moduleName = resolveRepositoryModuleName(moduleDescriptor, repositoryDataModule);
            String siteName = siteModuleNames.contains(moduleName) ? projectSiteName : null;
            if (siteName != null) {
                LOG.debug("  Using site '{}' for module at {}", siteName, moduleDescriptor);
            }
            ModuleImpl module = moduleReader.read(moduleDescriptor, false, siteName, null).getModule();
            modules.add(module);
            LOG.info("  ✓ Loaded module from: {}", moduleDescriptor);
        }

        ConfigurationModelImpl model = new ConfigurationModelImpl();
        Set<String> stubGroups = collectStubGroups(modules);
        addStubGroups(model, stubGroups);
        modules.forEach(model::addModule);

        LOG.info("Building configuration model from {} module(s)...", modules.size());
        ConfigurationModelImpl builtModel = buildModelOnce(model, modules, stubGroups);

        LOG.info("✓ Successfully built model with explicit modules only (framework modules NOT scanned)");
        return builtModel;
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
        LOG.info("Loading minimal framework module from: {}", modulePath);
        LOG.warn("Embedded minimal framework config is injected to satisfy core node types. " +
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

        ClassLoader classLoader = context.getClassLoader();
        LOG.info("Finding test module descriptors (target/test-classes or build/resources/test)...");

        Enumeration<URL> resources = classLoader.getResources(HCM_MODULE_DESCRIPTOR);
        while (resources.hasMoreElements()) {
            URL moduleUrl = resources.nextElement();
            if (moduleUrl.getProtocol().equals("file") &&
                (moduleUrl.getPath().contains("/target/test-classes/") ||
                 moduleUrl.getPath().contains("/build/resources/test/"))) {
                Path moduleDescriptorPath = Paths.get(moduleUrl.toURI());
                descriptors.add(moduleDescriptorPath.toAbsolutePath().normalize());
                LOG.info("✓ Found test module descriptor at: {}", moduleDescriptorPath);
            } else {
                LOG.debug("✗ Skipping framework/JAR module: {}", moduleUrl);
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
            LOG.info("Added stub group '{}' to satisfy dependency ordering", groupName);
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
        }
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

    private enum MissingDependencyType {
        GROUP,
        MODULE
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
