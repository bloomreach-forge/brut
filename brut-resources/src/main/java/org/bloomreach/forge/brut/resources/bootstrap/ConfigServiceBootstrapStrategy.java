package org.bloomreach.forge.brut.resources.bootstrap;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.bloomreach.forge.brut.common.project.ProjectSettings;
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
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
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
        LOG.info("========================================");
        LOG.info("Using ConfigurationConfigService bootstrap strategy");
        LOG.info("Project Namespace: {}", projectNamespace);
        LOG.info("========================================");

        try {
            // Load test modules EXPLICITLY by path (NO classpath scanning)
            // This avoids discovering framework modules that have dependency conflicts
            LOG.info("Loading test modules explicitly (no classpath scanning)...");
            LoadedModules loadedModules = loadModulesExplicitly(context);
            ConfigurationModelImpl configModel = loadedModules.model;
            List<ModuleImpl> modules = loadedModules.modules;

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
            applyConfigDeltaWithRetries(configService, baseline, configModel, session);

            ensureMandatoryConfigChildren(session);
            ensureSelectionNamespace(session);
            importContentDefinitions(modules, session);
            ensureProjectSpecificHstRoot(session, projectNamespace);
            ensurePreviewMounts(session, projectNamespace);
            logWorkspaceDiagnostics(session, projectNamespace);
            ensureMissingStateSummaries(session);
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
                    LOG.info("Workspace diagnostic: missing {}", fullPath);
                    continue;
                }
                Node node = session.getNode(fullPath);
                LOG.info("Workspace diagnostic: {} (children: {})", fullPath, countChildNodes(node));
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
            LOG.warn("Injected missing node {} under {}", name, parent.getPath());
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

        syncProjectSite(standardRoot, projectRoot, projectHstRoot);
        syncProjectConfiguration(standardRoot, projectRoot, projectHstRoot);
        syncProjectHosts(standardRoot, projectRoot, projectHstRoot);

        LOG.info("Project-specific HST root {} synced successfully", projectHstRoot);
    }

    private void copySiteNode(Node source, Node parent, String name) throws RepositoryException {
        Node site = parent.addNode(name, source.getPrimaryNodeType().getName());

        for (javax.jcr.PropertyIterator props = source.getProperties(); props.hasNext(); ) {
            javax.jcr.Property prop = props.nextProperty();
            if (!prop.getDefinition().isProtected()) {
                if (prop.isMultiple()) {
                    site.setProperty(prop.getName(), prop.getValues());
                } else {
                    site.setProperty(prop.getName(), prop.getValue());
                }
            }
        }

        for (NodeIterator children = source.getNodes(); children.hasNext(); ) {
            Node child = children.nextNode();
            copyNodeRecursively(child, site);
        }
    }

    private void copyConfigurationNode(Node source, Node parent, String name) throws RepositoryException {
        Node config = parent.addNode(name, source.getPrimaryNodeType().getName());

        for (javax.jcr.PropertyIterator props = source.getProperties(); props.hasNext(); ) {
            javax.jcr.Property prop = props.nextProperty();
            if (!prop.getDefinition().isProtected()) {
                if (prop.isMultiple()) {
                    config.setProperty(prop.getName(), prop.getValues());
                } else {
                    config.setProperty(prop.getName(), prop.getValue());
                }
            }
        }

        for (NodeIterator children = source.getNodes(); children.hasNext(); ) {
            Node child = children.nextNode();
            copyNodeRecursively(child, config);
        }
    }

    private void syncProjectSite(Node standardRoot,
                                 Node projectRoot,
                                 String projectHstRoot) throws RepositoryException {
        if (!standardRoot.hasNode("hst:sites")) {
            return;
        }
        Node standardSites = standardRoot.getNode("hst:sites");
        Node projectSites = getOrAddChild(projectRoot, "hst:sites", "hst:sites");

        List<String> orderedChildren = new ArrayList<>();
        for (NodeIterator children = standardSites.getNodes(); children.hasNext(); ) {
            Node sourceSite = children.nextNode();
            orderedChildren.add(sourceSite.getName());
            if (projectSites.hasNode(sourceSite.getName())) {
                Node targetSite = projectSites.getNode(sourceSite.getName());
                if (!sourceSite.getPrimaryNodeType().getName().equals(targetSite.getPrimaryNodeType().getName())) {
                    LOG.warn("Replacing site {} due to primary type mismatch (source {}, target {})",
                        sourceSite.getPath(),
                        sourceSite.getPrimaryNodeType().getName(),
                        targetSite.getPrimaryNodeType().getName());
                    targetSite.remove();
                    copyNodeRecursively(sourceSite, projectSites);
                    continue;
                }
                syncNodeRecursively(sourceSite, targetSite);
            } else {
                copyNodeRecursively(sourceSite, projectSites);
            }
        }

        if (projectSites.getPrimaryNodeType().hasOrderableChildNodes()) {
            for (String childName : orderedChildren) {
                if (projectSites.hasNode(childName)) {
                    projectSites.orderBefore(childName, null);
                }
            }
        }

        if (!orderedChildren.isEmpty()) {
            LOG.info("Synced {} site(s) at {}/hst:sites", orderedChildren.size(), projectHstRoot);
        }
    }

    private void syncProjectConfiguration(Node standardRoot,
                                          Node projectRoot,
                                          String projectHstRoot) throws RepositoryException {
        if (!standardRoot.hasNode("hst:configurations")) {
            return;
        }
        Node standardConfigs = standardRoot.getNode("hst:configurations");
        Node projectConfigs = getOrAddChild(projectRoot, "hst:configurations", "hst:configurations");

        List<String> orderedChildren = new ArrayList<>();
        for (NodeIterator children = standardConfigs.getNodes(); children.hasNext(); ) {
            Node sourceConfig = children.nextNode();
            orderedChildren.add(sourceConfig.getName());
            if (projectConfigs.hasNode(sourceConfig.getName())) {
                Node targetConfig = projectConfigs.getNode(sourceConfig.getName());
                if (!sourceConfig.getPrimaryNodeType().getName().equals(targetConfig.getPrimaryNodeType().getName())) {
                    LOG.warn("Replacing configuration {} due to primary type mismatch (source {}, target {})",
                        sourceConfig.getPath(),
                        sourceConfig.getPrimaryNodeType().getName(),
                        targetConfig.getPrimaryNodeType().getName());
                    targetConfig.remove();
                    copyNodeRecursively(sourceConfig, projectConfigs);
                    continue;
                }
                syncNodeRecursively(sourceConfig, targetConfig);
            } else {
                copyNodeRecursively(sourceConfig, projectConfigs);
            }
        }

        if (projectConfigs.getPrimaryNodeType().hasOrderableChildNodes()) {
            for (String childName : orderedChildren) {
                if (projectConfigs.hasNode(childName)) {
                    projectConfigs.orderBefore(childName, null);
                }
            }
        }

        if (!orderedChildren.isEmpty()) {
            LOG.info("Synced {} configuration(s) at {}/hst:configurations", orderedChildren.size(), projectHstRoot);
        }
    }

    private void syncProjectHosts(Node standardRoot,
                                  Node projectRoot,
                                  String projectHstRoot) throws RepositoryException {
        if (!standardRoot.hasNode("hst:hosts")) {
            return;
        }
        Node sourceHosts = standardRoot.getNode("hst:hosts");
        if (projectRoot.hasNode("hst:hosts")) {
            syncNodeRecursively(sourceHosts, projectRoot.getNode("hst:hosts"));
        } else {
            copyNodeRecursively(sourceHosts, projectRoot);
        }
        LOG.info("Synced project hosts at {}/hst:hosts", projectHstRoot);
    }

    private Node getOrAddChild(Node parent, String name, String primaryType) throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        }
        return parent.addNode(name, primaryType);
    }

    private void syncNodeRecursively(Node source, Node target) throws RepositoryException {
        for (NodeType mixin : source.getMixinNodeTypes()) {
            if (!target.isNodeType(mixin.getName())) {
                target.addMixin(mixin.getName());
            }
        }

        for (javax.jcr.PropertyIterator props = source.getProperties(); props.hasNext(); ) {
            javax.jcr.Property prop = props.nextProperty();
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            if (prop.isMultiple()) {
                target.setProperty(prop.getName(), prop.getValues());
            } else {
                target.setProperty(prop.getName(), prop.getValue());
            }
        }

        List<String> orderedChildren = new ArrayList<>();
        for (NodeIterator children = source.getNodes(); children.hasNext(); ) {
            Node child = children.nextNode();
            orderedChildren.add(child.getName());
            if (target.hasNode(child.getName())) {
                Node targetChild = target.getNode(child.getName());
                if (!child.getPrimaryNodeType().getName().equals(targetChild.getPrimaryNodeType().getName())) {
                    LOG.warn("Replacing {} due to primary type mismatch (source {}, target {})",
                        child.getPath(),
                        child.getPrimaryNodeType().getName(),
                        targetChild.getPrimaryNodeType().getName());
                    Node parent = targetChild.getParent();
                    targetChild.remove();
                    copyNodeRecursively(child, parent);
                    continue;
                }
                syncNodeRecursively(child, targetChild);
            } else {
                copyNodeRecursively(child, target);
            }
        }

        if (target.getPrimaryNodeType().hasOrderableChildNodes()) {
            for (String childName : orderedChildren) {
                if (target.hasNode(childName)) {
                    target.orderBefore(childName, null);
                }
            }
        }
    }

    private void copyNodeRecursively(Node source, Node parent) throws RepositoryException {
        Node copy = parent.addNode(source.getName(), source.getPrimaryNodeType().getName());

        for (NodeType mixin : source.getMixinNodeTypes()) {
            copy.addMixin(mixin.getName());
        }

        for (javax.jcr.PropertyIterator props = source.getProperties(); props.hasNext(); ) {
            javax.jcr.Property prop = props.nextProperty();
            if (!prop.getDefinition().isProtected()) {
                if (prop.isMultiple()) {
                    copy.setProperty(prop.getName(), prop.getValues());
                } else {
                    copy.setProperty(prop.getName(), prop.getValue());
                }
            }
        }

        for (NodeIterator children = source.getNodes(); children.hasNext(); ) {
            Node child = children.nextNode();
            copyNodeRecursively(child, copy);
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

    private void applyConfigDeltaWithRetries(ConfigurationConfigService configService,
                                             ConfigurationModelImpl baseline,
                                             ConfigurationModelImpl update,
                                             Session session) throws RepositoryException {
        Set<String> removedPaths = new HashSet<>();
        int attempts = 0;
        while (true) {
            try {
                invokeComputeAndWriteDelta(configService, baseline, update, session, true);
                return;
            } catch (RepositoryException e) {
                String existingPath = extractItemExistsPath(e);
                if (existingPath == null || removedPaths.contains(existingPath) || attempts >= MAX_ITEM_EXISTS_RETRIES) {
                    throw e;
                }
                session.refresh(false);
                if (!removeExistingNode(session, existingPath)) {
                    throw e;
                }
                removedPaths.add(existingPath);
                attempts++;
                LOG.warn("Removed existing node {} after ItemExistsException; retrying ConfigService delta.", existingPath);
            } catch (Exception e) {
                throw new RepositoryException("Failed to compute configuration delta", e);
            }
        }
    }

    private String extractItemExistsPath(Throwable error) {
        ItemExistsException itemExists = findItemExistsException(error);
        if (itemExists == null) {
            return null;
        }
        String message = itemExists.getMessage();
        if (message == null) {
            return null;
        }
        String marker = "exists:";
        int start = message.indexOf(marker);
        if (start < 0) {
            return null;
        }
        String path = message.substring(start + marker.length()).trim();
        return path.isEmpty() ? null : path;
    }

    private ItemExistsException findItemExistsException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ItemExistsException) {
                return (ItemExistsException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean removeExistingNode(Session session, String path) throws RepositoryException {
        if (path == null || !path.startsWith("/") || !session.nodeExists(path)) {
            return false;
        }
        Node node = session.getNode(path);
        if (node.getDepth() == 0) {
            return false;
        }
        node.remove();
        return true;
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

        List<String> allowedRoots = resolveAllowedConfigRoots();
        int filteredDefinitions = filterConfigDefinitionsByAllowedRoots(modules, allowedRoots);
        if (filteredDefinitions > 0) {
            LOG.warn("Removed {} config definition(s) outside allowed roots {}. " +
                    "Set -D{} to adjust or '*' to disable filtering.",
                filteredDefinitions, allowedRoots, ALLOWED_CONFIG_ROOTS_PROPERTY);
        }

        ConfigurationModelImpl model = new ConfigurationModelImpl();
        Set<String> stubGroups = collectStubGroups(modules);
        addStubGroups(model, stubGroups);
        modules.forEach(model::addModule);

        LOG.info("Building configuration model from {} module(s)...", modules.size());
        ConfigurationModelImpl builtModel = buildModelOnce(model, modules, stubGroups);

        LOG.info("✓ Successfully built model with explicit modules only (framework modules NOT scanned)");
        return new LoadedModules(builtModel, List.copyOf(modules));
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

        // If explicit module descriptors were provided, use only those (exclusive filter)
        if (!descriptors.isEmpty()) {
            LOG.info("Using {} explicitly specified module descriptor(s)", descriptors.size());
            return descriptors;
        }

        // Fall back to classpath scanning only if no explicit modules provided
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
            LOG.warn("Removed {} config definition(s) under {}", removed, rootPrefix);
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
            LOG.warn("Skipped {} HCM content source(s) outside allowed roots {}. " +
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
                        String prefix = extractNamespacePrefix(nsEx.getMessage());
                        if (prefix != null && stubbedNamespaces.add(prefix)) {
                            registerStubNamespace(session, prefix);
                            continue;
                        }
                    }
                }

                if (stubNodeTypes) {
                    NoSuchNodeTypeException ntEx = findCause(e, NoSuchNodeTypeException.class);
                    if (ntEx != null) {
                        String nodeType = extractNodeType(ntEx.getMessage());
                        if (nodeType != null && stubbedNodeTypes.add(nodeType)) {
                            registerStubNodeType(session, nodeType, stubbedNamespaces);
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

    private String extractNamespacePrefix(String message) {
        if (message == null) {
            return null;
        }
        int colonIdx = message.indexOf(':');
        if (colonIdx > 0 && colonIdx < 50) {
            return message.substring(0, colonIdx).trim();
        }
        return null;
    }

    private void registerStubNamespace(Session session, String prefix) throws RepositoryException {
        String uri = "urn:brut:stub:" + prefix;
        try {
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
            LOG.warn("Stubbed missing namespace '{}' -> '{}' for delivery-tier tests. " +
                "Disable with -D{}=false", prefix, uri, STUB_MISSING_NAMESPACES_PROPERTY);
        } catch (NamespaceException e) {
            LOG.debug("Namespace '{}' already registered or conflict: {}", prefix, e.getMessage());
        }
    }

    private String extractNodeType(String message) {
        if (message == null) {
            return null;
        }

        // Handle expanded URI format: {http://...}localName
        int braceStart = message.indexOf('{');
        int braceEnd = message.indexOf('}', braceStart + 1);
        if (braceStart >= 0 && braceEnd > braceStart) {
            String uri = message.substring(braceStart + 1, braceEnd);
            int localEnd = braceEnd + 1;
            while (localEnd < message.length() && !Character.isWhitespace(message.charAt(localEnd))) {
                localEnd++;
            }
            String localName = message.substring(braceEnd + 1, localEnd);
            if (!localName.isEmpty() && isValidNCName(localName)) {
                return "{" + uri + "}" + localName;
            }
        }

        // Handle prefix:localName format
        int colonIdx = message.indexOf(':');
        if (colonIdx > 0) {
            int start = message.lastIndexOf(' ', colonIdx);
            String candidate = message.substring(start < 0 ? 0 : start + 1).trim();
            if (candidate.contains(":")) {
                int end = candidate.indexOf(' ');
                String nodeType = end > 0 ? candidate.substring(0, end) : candidate;
                if (isValidPrefixedNodeTypeName(nodeType)) {
                    return nodeType;
                }
            }
        }
        return null;
    }

    private boolean isValidPrefixedNodeTypeName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        int colonIdx = name.indexOf(':');
        if (colonIdx <= 0 || colonIdx == name.length() - 1) {
            return false;
        }
        String prefix = name.substring(0, colonIdx);
        String localName = name.substring(colonIdx + 1);
        return isValidNCName(prefix) && isValidNCName(localName);
    }

    private boolean isValidNCName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private void registerStubNodeType(Session session, String nodeType, Set<String> stubbedNamespaces)
            throws RepositoryException {
        String prefix;
        String uri;
        String localName;
        String qualifiedName;

        // Handle expanded URI format: {uri}localName
        if (nodeType.startsWith("{")) {
            int braceEnd = nodeType.indexOf('}');
            if (braceEnd <= 1) {
                return;
            }
            uri = nodeType.substring(1, braceEnd);
            localName = nodeType.substring(braceEnd + 1);
            prefix = derivePrefixFromUri(uri);
            qualifiedName = prefix + ":" + localName;
        } else {
            // Handle prefix:localName format
            int colonIdx = nodeType.indexOf(':');
            if (colonIdx <= 0) {
                return;
            }
            prefix = nodeType.substring(0, colonIdx);
            localName = nodeType.substring(colonIdx + 1);
            uri = "urn:brut:stub:" + prefix;
            qualifiedName = nodeType;
        }

        try {
            session.getNamespaceURI(prefix);
        } catch (NamespaceException e) {
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
            stubbedNamespaces.add(prefix);
        }

        String cnd = String.format(
            "<%s='%s'>\n[%s] > nt:base\n  - * (UNDEFINED) multiple\n  + * (nt:base) = nt:base sns",
            prefix, uri, qualifiedName);

        try {
            CndImporter.registerNodeTypes(new StringReader(cnd), session);
            LOG.warn("Stubbed missing node type '{}' for delivery-tier tests. " +
                "Disable with -D{}=false", qualifiedName, STUB_MISSING_NODE_TYPES_PROPERTY);
        } catch (Exception e) {
            LOG.debug("Failed to register stub node type '{}': {}", qualifiedName, e.getMessage());
        }
    }

    private String derivePrefixFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "stub";
        }
        // Extract meaningful segment from URI like http://www.onehippo.org/jcr/hippofacnav/nt/1.0.1
        // Try to find a segment that looks like a namespace prefix
        String[] segments = uri.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];
            if (segment.isEmpty() || segment.matches("\\d+\\.\\d+.*") || "nt".equals(segment)) {
                continue;
            }
            if (isValidNCName(segment) && segment.length() <= 20) {
                return segment;
            }
        }
        return "stub" + Math.abs(uri.hashCode() % 10000);
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
