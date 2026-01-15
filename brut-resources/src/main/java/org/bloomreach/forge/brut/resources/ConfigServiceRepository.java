package org.bloomreach.forge.brut.resources;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
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

        this.projectNamespace = projectNamespace;
        this.bootstrapStrategy = new ConfigServiceBootstrapStrategy();

        LOGGER.info("ConfigServiceRepository initialized for project: {}", projectNamespace);
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
        try {
            LOGGER.info("========================================");
            LOGGER.info("Initializing ConfigServiceRepository");
            LOGGER.info("Project Namespace: {}", projectNamespace);
            LOGGER.info("========================================");

            session = this.login(new SimpleCredentials("admin", "admin".toCharArray()));

            // Step 1: Register CNDs (parent method)
            LOGGER.info("Step 1: Registering CND node types");
            registerCnds(session, cndResourcesPatterns);
            LOGGER.info("Step 1 complete");

            // Step 2: Bootstrap HST structure using ConfigService
            LOGGER.info("Step 2: Bootstrapping HST via ConfigService");
            BootstrapContext context = new BootstrapContext(
                cndResourcesPatterns,
                yamlResourcesPatterns,
                Collections.emptyList(),
                Thread.currentThread().getContextClassLoader()
            );
            bootstrapStrategy.initializeHstStructure(session, projectNamespace, context);
            LOGGER.info("Step 2 complete");

            // Step 3: Import YAML content (parent method)
            LOGGER.info("Step 3: Importing YAML resources");
            importYamlResources(session, yamlResourcesPatterns);
            LOGGER.info("Step 3 complete");

            // Step 4: Recalculate hippo paths (parent method)
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
            LOGGER.error("========================================", e);
            throw new RuntimeException("ConfigService bootstrap failed", e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    /**
     * Helper to import YAML resources (mirrors parent private method).
     */
    private void importYamlResources(Session session, List<String> yamlResourcePatterns) throws RepositoryException {
        try {
            for (String yamlResourcePattern : yamlResourcePatterns) {
                Resource[] resources = resolveResourcePattern(yamlResourcePattern);
                for (Resource resource : resources) {
                    ImporterUtils.importYaml(resource.getURL(), session.getRootNode(),
                            "", "hippostd:folder");
                }
            }
            session.save();
        } catch (Exception ex) {
            throw new RepositoryException(ex);
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
                NodeType[] nodeTypes = CndImporter.registerNodeTypes(new InputStreamReader(cndResource.getInputStream()), session);
                for (NodeType nt : nodeTypes) {
                    LOGGER.debug("Registered: {}", nt.getName());
                }
            } catch (Exception e) {
                throw new RepositoryException(e);
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
            Resource[] resources = resolver.getResources(pattern);
            for (Resource resource : resources) {
                LOGGER.debug("RESOURCE: {}", resource.getFilename());
            }
            return resources;
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
