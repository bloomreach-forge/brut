package org.bloomreach.forge.brut.resources;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.bloomreach.forge.brut.resources.bootstrap.BootstrapContext;
import org.bloomreach.forge.brut.resources.bootstrap.ConfigServiceBootstrapStrategy;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;

/**
 * JCR repository that uses brXM's production ConfigService for HST bootstrap.
 * <p>
 * This repository leverages brXM's ConfigurationConfigService
 * to initialize HST configuration, providing production-identical structure without
 * manual JCR node construction.
 * <p>
 * <strong>Requirements:</strong>
 * <ul>
 *   <li>hcm-module.yaml must exist in test resources (META-INF/hcm-module.yaml)</li>
 *   <li>HCM config YAML files defining HST structure (hst:hst, sites, configurations, etc.)</li>
 * </ul>
 * <p>
 * <strong>Benefits over SkeletonRepository:</strong>
 * <ul>
 *   <li>Production parity - uses same bootstrap code as real brXM</li>
 *   <li>Zero maintenance - brXM structure changes propagate automatically</li>
 *   <li>Leverage internals - ConfigService handles UUIDs, refs, node ordering, etc.</li>
 *   <li>Future-proof - no manual updates when brXM changes</li>
 * </ul>
 *
 * @see SkeletonRepository
 * @see ConfigServiceBootstrapStrategy
 * @since 5.2.0
 */
public class ConfigServiceRepository extends BrxmTestingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceRepository.class);

    private final List<String> cndResourcesPatterns;
    private final List<String> yamlResourcesPatterns;
    private final String projectNamespace;
    private final ConfigServiceBootstrapStrategy bootstrapStrategy;
    private List<String> additionalRepositoryModules = Collections.emptyList();

    /**
     * Creates repository with ConfigService bootstrap.
     *
     * @param cndResourcesPatterns CND patterns from framework
     * @param contributedCndResourcesPatterns CND patterns from user tests
     * @param yamlResourcesPatterns YAML patterns from framework
     * @param contributedYamlResourcesPatterns YAML patterns from user tests
     * @param projectNamespace project namespace (e.g., "myproject")
     */
    public ConfigServiceRepository(List<String> cndResourcesPatterns, List<String> contributedCndResourcesPatterns,
                                   List<String> yamlResourcesPatterns, List<String> contributedYamlResourcesPatterns,
                                   String projectNamespace)
            throws RepositoryException, IOException {
        // Merge patterns
        List<String> allCndPatterns = new ArrayList<>(cndResourcesPatterns);
        allCndPatterns.addAll(contributedCndResourcesPatterns);
        this.cndResourcesPatterns = allCndPatterns;

        List<String> allYamlPatterns = new ArrayList<>(yamlResourcesPatterns);
        allYamlPatterns.addAll(contributedYamlResourcesPatterns);
        this.yamlResourcesPatterns = allYamlPatterns;

        if (projectNamespace != null && !projectNamespace.isBlank()) {
            this.projectNamespace = projectNamespace;
        } else {
            this.projectNamespace = ProjectDiscovery.resolveProjectNamespace(
                Paths.get(System.getProperty("user.dir"))
            );
        }
        this.bootstrapStrategy = new ConfigServiceBootstrapStrategy();

        LOGGER.info("ConfigServiceRepository initialized for project: {}", projectNamespace);
    }

    public void setAdditionalRepositoryModules(List<String> additionalRepositoryModules) {
        this.additionalRepositoryModules = additionalRepositoryModules != null
            ? List.copyOf(additionalRepositoryModules)
            : Collections.emptyList();
    }

    /**
     * Initializes repository with ConfigService bootstrap.
     * <p>
     * Steps:
     * <ol>
     *   <li>Register CND node types</li>
     *   <li>Bootstrap HST structure via ConfigService (production code path)</li>
     *   <li>Import YAML content</li>
     *   <li>Recalculate hippo paths</li>
     * </ol>
     */
    public void init() {
        Session session = null;
        String currentStep = "initialization";
        try {
            LOGGER.info("========================================");
            LOGGER.info("Initializing ConfigServiceRepository");
            LOGGER.info("Project Namespace: {}", projectNamespace);
            LOGGER.info("CND Patterns: {}", cndResourcesPatterns);
            LOGGER.info("YAML Patterns: {}", yamlResourcesPatterns);
            if (!additionalRepositoryModules.isEmpty()) {
                LOGGER.info("Additional Repository Modules: {}", additionalRepositoryModules);
            }
            LOGGER.info("========================================");

            session = this.login(new SimpleCredentials("admin", "admin".toCharArray()));

            // Step 1: Register CNDs (parent method)
            currentStep = "CND registration";
            LOGGER.info("Step 1: Registering CND node types");
            try {
                registerCnds(session, cndResourcesPatterns);
                LOGGER.info("Step 1 complete - {} CND patterns registered", cndResourcesPatterns.size());
            } catch (Exception e) {
                throw new RepositoryException(buildStepFailureMessage(currentStep, cndResourcesPatterns,
                        "Ensure CND files exist at specified patterns and are valid CND syntax"), e);
            }

            // Step 2: Bootstrap HST structure using ConfigService
            currentStep = "ConfigService HST bootstrap";
            LOGGER.info("Step 2: Bootstrapping HST via ConfigService");
            try {
                List<Path> moduleDescriptors = ProjectDiscovery.discoverRepositoryModuleDescriptors(
                    Paths.get(System.getProperty("user.dir")),
                    additionalRepositoryModules
                );
                LOGGER.info("Discovered {} HCM module descriptor(s)", moduleDescriptors.size());

                if (moduleDescriptors.isEmpty()) {
                    throw new RepositoryException(
                        "No HCM module descriptors found (hcm-module.yaml).\n\n" +
                        "To fix:\n" +
                        "  1. Create META-INF/hcm-module.yaml in your test resources\n" +
                        "  2. Ensure it contains valid HCM module configuration\n" +
                        "  3. Check that test resources are on the classpath"
                    );
                }

                BootstrapContext context = new BootstrapContext(
                    cndResourcesPatterns,
                    yamlResourcesPatterns,
                    Collections.emptyList(),
                    moduleDescriptors,
                    Thread.currentThread().getContextClassLoader()
                );
                bootstrapStrategy.initializeHstStructure(session, projectNamespace, context);
                LOGGER.info("Step 2 complete - HST structure created");
            } catch (Exception e) {
                throw new RepositoryException(buildStepFailureMessage(currentStep, yamlResourcesPatterns,
                        "Check that hcm-module.yaml and HST configuration YAML files exist and are valid"), e);
            }

            // Step 3: Import YAML content (parent method)
            currentStep = "YAML resource import";
            LOGGER.info("Step 3: Importing YAML resources");
            try {
                importYamlResources(session, yamlResourcesPatterns);
                LOGGER.info("Step 3 complete - {} YAML patterns imported", yamlResourcesPatterns.size());
            } catch (Exception e) {
                throw new RepositoryException(buildStepFailureMessage(currentStep, yamlResourcesPatterns,
                        "Verify YAML files are well-formed and contain valid JCR content structure"), e);
            }

            // Step 4: Recalculate hippo paths (parent method)
            currentStep = "hippo paths recalculation";
            LOGGER.info("Step 4: Recalculating hippo paths");
            try {
                recalculateHippoPathsIfExists("/content", session);
                LOGGER.info("Step 4 complete");
            } catch (Exception e) {
                LOGGER.warn("Path recalculation failed (non-fatal): {}", e.getMessage());
            }

            LOGGER.info("========================================");
            LOGGER.info("ConfigServiceRepository initialization COMPLETE");
            LOGGER.info("========================================");

        } catch (RepositoryException e) {
            LOGGER.error("========================================");
            LOGGER.error("ConfigServiceRepository initialization FAILED");
            LOGGER.error("Failed during: {}", currentStep);
            LOGGER.error("========================================", e);
            throw new RuntimeException("ConfigServiceRepository initialization failed during " + currentStep +
                    ". See error details above.", e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    private String buildStepFailureMessage(String step, List<String> patterns, String suggestion) {
        return String.format(
            "Step failed: %s\n\n" +
            "Patterns attempted:\n  %s\n\n" +
            "To fix:\n  %s",
            step,
            String.join("\n  ", patterns),
            suggestion
        );
    }

    /**
     * Helper to import YAML resources (mirrors parent private method).
     */
    private void importYamlResources(Session session, List<String> yamlResourcePatterns) throws RepositoryException {
        try {
            int totalResources = 0;
            for (String yamlResourcePattern : yamlResourcePatterns) {
                Resource[] resources = resolveResourcePattern(yamlResourcePattern);
                LOGGER.debug("Pattern '{}' matched {} resource(s)", yamlResourcePattern, resources.length);
                for (Resource resource : resources) {
                    try {
                        LOGGER.debug("Importing YAML: {}", resource.getFilename());
                        ImporterUtils.importYaml(resource.getURL(), session.getRootNode(),
                                "", "hippostd:folder");
                        totalResources++;
                    } catch (Exception e) {
                        throw new RepositoryException(
                            String.format("Failed to import YAML resource: %s\nPattern: %s\nCause: %s",
                                    resource.getFilename(), yamlResourcePattern, e.getMessage()), e);
                    }
                }
            }
            session.save();
            LOGGER.debug("Successfully imported {} YAML resource(s)", totalResources);
        } catch (RepositoryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RepositoryException("Unexpected error during YAML import: " + ex.getMessage(), ex);
        }
    }

    /**
     * Helper to register CNDs (from SkeletonRepository).
     */
    private void registerCnds(Session session, List<String> cndResourcesPatterns) throws RepositoryException {
        for (String cndResourcePattern : cndResourcesPatterns) {
            registerNamespaces(session, resolveResourcePattern(cndResourcePattern));
        }
    }

    /**
     * Helper to register namespaces from CND resources (from SkeletonRepository).
     */
    private void registerNamespaces(Session session, Resource[] cndResources) throws RepositoryException {
        for (Resource cndResource : cndResources) {
            try {
                LOGGER.debug("Registering CND: {}", cndResource.getFilename());
                NodeType[] nodeTypes = CndImporter.registerNodeTypes(new InputStreamReader(cndResource.getInputStream()), session);
                LOGGER.debug("Registered {} node type(s) from {}", nodeTypes.length, cndResource.getFilename());
                for (NodeType nt : nodeTypes) {
                    LOGGER.debug("  - {}", nt.getName());
                }
            } catch (Exception e) {
                throw new RepositoryException(
                    String.format("Failed to register CND file: %s\nCause: %s\n\n" +
                            "To fix:\n  Verify CND file syntax is valid and readable",
                            cndResource.getFilename(), e.getMessage()), e);
            }
        }
    }

    /**
     * Helper to resolve resource patterns (mirrors parent private method).
     */
    private Resource[] resolveResourcePattern(String pattern) throws RepositoryException {
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
            return resolver.getResources(pattern);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Helper to recalculate paths only if node exists (from SkeletonRepository).
     */
    private void recalculateHippoPathsIfExists(String absolutePath, Session session) {
        try {
            if (session.getRootNode().hasNode(absolutePath.substring(1))) {
                recalculateHippoPaths(absolutePath);
                LOGGER.info("Recalculated hippo paths for: {}", absolutePath);
            } else {
                LOGGER.debug("Skipping path recalculation: {} node does not exist", absolutePath);
            }
        } catch (RepositoryException e) {
            LOGGER.warn("Failed to check if {} exists: {}", absolutePath, e.getMessage());
        }
    }

    /**
     * Recalculates hippo paths (from SkeletonRepository).
     */
    private void recalculateHippoPaths(String absolutePath) {
        Session session = null;
        try {
            session = this.login(new SimpleCredentials("admin", "admin".toCharArray()));
            Node rootNode = session.getRootNode();
            Node node = rootNode.getNode(absolutePath.substring(1));
            calculateHippoPaths(node, getPathsForNode(node, rootNode));
            session.save();
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    private LinkedList<String> getPathsForNode(Node node, Node rootNode) throws RepositoryException {
        LinkedList<String> paths = new LinkedList<>();
        Node parentNode = node;
        do {
            parentNode = parentNode.getParent();
            paths.add(parentNode.getIdentifier());
        } while (!parentNode.isSame(rootNode));
        return paths;
    }

    @SuppressWarnings("unchecked")
    private void calculateHippoPaths(Node node, LinkedList<String> paths) throws RepositoryException {
        paths.add(0, node.getIdentifier());
        setHippoPath(node, paths);
        for (NodeIterator nodes = node.getNodes(); nodes.hasNext(); ) {
            Node subnode = nodes.nextNode();
            if (!subnode.isNodeType("hippo:handle")) {
                if (!subnode.isNodeType("hippotranslation:translations")) {
                    calculateHippoPaths(subnode, (LinkedList<String>) paths.clone());
                }
            } else {
                setHandleHippoPaths(subnode, (LinkedList<String>) paths.clone());
            }
        }
    }

    private void setHippoPath(Node node, LinkedList<String> paths) throws RepositoryException {
        node.setProperty(HIPPO_PATHS, paths.toArray(new String[0]));
    }

    private void setHandleHippoPaths(Node handle, LinkedList<String> paths) throws RepositoryException {
        paths.add(0, handle.getIdentifier());
        for (NodeIterator nodes = handle.getNodes(handle.getName()); nodes.hasNext(); ) {
            Node subnode = nodes.nextNode();
            paths.add(0, subnode.getIdentifier());
            setHippoPath(subnode, paths);
            paths.remove(0);
        }
    }
}
