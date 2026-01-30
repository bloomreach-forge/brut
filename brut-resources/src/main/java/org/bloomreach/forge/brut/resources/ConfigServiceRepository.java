package org.bloomreach.forge.brut.resources;

import org.bloomreach.forge.brut.common.repository.AbstractBrutRepository;
import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.bloomreach.forge.brut.resources.bootstrap.BootstrapContext;
import org.bloomreach.forge.brut.resources.bootstrap.ConfigServiceBootstrapStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public class ConfigServiceRepository extends AbstractBrutRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigServiceRepository.class);
    private static final String HST_CONTENT_PROPERTY = "hst:content";

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
        super();

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

        LOG.info("ConfigServiceRepository initialized for project: {}", this.projectNamespace);
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
            LOG.debug("Initializing ConfigServiceRepository for project: {}", projectNamespace);

            session = this.login(new SimpleCredentials("admin", "admin".toCharArray()));

            currentStep = "CND registration";
            try {
                registerCnds(session, cndResourcesPatterns);
            } catch (Exception e) {
                throw new RepositoryException(buildStepFailureMessage(currentStep, cndResourcesPatterns,
                        "Ensure CND files exist at specified patterns and are valid CND syntax"), e);
            }

            currentStep = "ConfigService HST bootstrap";
            try {
                List<Path> moduleDescriptors = ProjectDiscovery.discoverRepositoryModuleDescriptors(
                    Paths.get(System.getProperty("user.dir")),
                    additionalRepositoryModules
                );

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
            } catch (Exception e) {
                throw new RepositoryException(buildStepFailureMessage(currentStep, yamlResourcesPatterns,
                        "Check that hcm-module.yaml and HST configuration YAML files exist and are valid"), e);
            }

            currentStep = "YAML resource import";
            try {
                importYamlResources(session, yamlResourcesPatterns);
            } catch (Exception e) {
                throw new RepositoryException(buildStepFailureMessage(currentStep, yamlResourcesPatterns,
                        "Verify YAML files are well-formed and contain valid JCR content structure"), e);
            }

            currentStep = "hippo paths recalculation";
            try {
                if (session.getRootNode().hasNode("content")) {
                    recalculateHippoPaths("/content");
                }
            } catch (Exception e) {
                LOG.warn("Path recalculation failed (non-fatal): {}", e.getMessage());
            }

            currentStep = "content path setup";
            ensureContentPaths(session);

            LOG.info("ConfigServiceRepository initialized for project: {}", projectNamespace);

        } catch (RepositoryException e) {
            LOG.error("ConfigServiceRepository failed during {}: {}", currentStep, e.getMessage());
            throw new RuntimeException("ConfigServiceRepository initialization failed during " + currentStep, e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    private String buildStepFailureMessage(String step, List<String> patterns, String suggestion) {
        return String.format(
            "Step failed: %s%n%n" +
            "Patterns attempted:%n  %s%n%n" +
            "To fix:%n  %s",
            step,
            String.join("\n  ", patterns),
            suggestion
        );
    }

    private void importYamlResources(Session session, List<String> yamlResourcePatterns) throws RepositoryException {
        try {
            int totalResources = 0;
            for (String yamlResourcePattern : yamlResourcePatterns) {
                Resource[] resources = resolveResourcePattern(yamlResourcePattern);
                LOG.debug("Pattern '{}' matched {} resource(s)", yamlResourcePattern, resources.length);
                for (Resource resource : resources) {
                    try {
                        LOG.debug("Importing YAML: {}", resource.getFilename());
                        ImporterUtils.importYaml(resource.getURL(), session.getRootNode(),
                                "", "hippostd:folder");
                        totalResources++;
                    } catch (Exception e) {
                        throw new RepositoryException(
                            String.format("Failed to import YAML resource: %s%nPattern: %s%nCause: %s",
                                    resource.getFilename(), yamlResourcePattern, e.getMessage()), e);
                    }
                }
            }
            session.save();
            LOG.debug("Successfully imported {} YAML resource(s)", totalResources);
        } catch (RepositoryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RepositoryException("Unexpected error during YAML import: " + ex.getMessage(), ex);
        }
    }

    /**
     * Ensures that hst:content paths in HST site configurations exist in JCR.
     * Creates stub folders for any missing content paths to support getSiteContentBaseBean().
     */
    private void ensureContentPaths(Session session) {
        try {
            // Check project-specific HST root and fallback to /hst:hst
            String projectHstRoot = "/hst:" + projectNamespace;
            String[] hstRoots = {projectHstRoot, "/hst:hst"};
            for (String hstRoot : hstRoots) {
                if (session.nodeExists(hstRoot)) {
                    ensureContentPathsUnder(session, session.getNode(hstRoot));
                }
            }
        } catch (RepositoryException e) {
            LOG.debug("Content path setup skipped: {}", e.getMessage());
        }
    }

    private void ensureContentPathsUnder(Session session, Node node) throws RepositoryException {
        if (node.hasProperty(HST_CONTENT_PROPERTY)) {
            Property contentProp = node.getProperty(HST_CONTENT_PROPERTY);
            String contentPath = contentProp.getString();
            if (contentPath != null && !contentPath.isEmpty()) {
                String absolutePath = contentPath.startsWith("/") ? contentPath : "/" + contentPath;
                if (!session.nodeExists(absolutePath)) {
                    ensureContentPath(session, absolutePath);
                    LOG.info("Created stub content folder at '{}' for HST config '{}'",
                            absolutePath, node.getPath());
                }
            }
        }
        // Recurse into child nodes
        for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
            ensureContentPathsUnder(session, children.nextNode());
        }
    }

    /**
     * Creates a stub content folder at the given path, ensuring all parent folders exist.
     */
    private void ensureContentPath(Session session, String path) throws RepositoryException {
        String[] segments = path.substring(1).split("/");
        Node current = session.getRootNode();

        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if (current.hasNode(segment)) {
                current = current.getNode(segment);
            } else {
                current = current.addNode(segment, "hippostd:folder");
                if (current.canAddMixin("mix:referenceable")) {
                    current.addMixin("mix:referenceable");
                }
            }
        }
        session.save();
    }
}
