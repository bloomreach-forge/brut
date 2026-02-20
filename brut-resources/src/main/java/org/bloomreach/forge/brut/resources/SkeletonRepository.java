package org.bloomreach.forge.brut.resources;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.bloomreach.forge.brut.common.repository.AbstractBrutRepository;
import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
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
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SkeletonRepository extends AbstractBrutRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SkeletonRepository.class);
    private static final String HST_CONTENT_PROPERTY = "hst:content";

    private final List<String> cndResourcesPatterns;
    private final List<String> yamlResourcesPatterns;

    public SkeletonRepository(List<String> cndResourcesPatterns, List<String> contributedCndResourcesPatterns,
                              List<String> yamlResourcesPatterns, List<String> contributedYamlResourcesPatterns)
            throws RepositoryException, IOException {
        super();
        this.cndResourcesPatterns = new java.util.ArrayList<>(cndResourcesPatterns);
        this.cndResourcesPatterns.addAll(contributedCndResourcesPatterns);

        this.yamlResourcesPatterns = new java.util.ArrayList<>(yamlResourcesPatterns);
        this.yamlResourcesPatterns.addAll(contributedYamlResourcesPatterns);
    }

    public void init() {
        Session session = null;
        try {
            session = this.login(new SimpleCredentials("admin", "admin".toCharArray()));
            registerCnds(session, cndResourcesPatterns);
            importYamlResources(session, yamlResourcesPatterns);
            if (session.getRootNode().hasNode("content")) {
                recalculateHippoPaths("/content");
            } else {
                LOG.debug("Skipping /content path recalculation: node does not exist");
            }
            ensureContentPaths(session);
        } catch (RepositoryException e) {
            LOG.error("Repository initialization failed", e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    /**
     * Ensures that hst:content paths in HST site configurations exist in JCR.
     * Creates stub folders for any missing content paths to support getSiteContentBaseBean().
     * Checks {@code /hst:hst} and the project-specific root resolved via {@link ProjectDiscovery}.
     */
    private void ensureContentPaths(Session session) {
        try {
            Set<String> candidates = new LinkedHashSet<>();
            candidates.add("/hst:hst");
            String projectRoot = ProjectDiscovery.resolveHstRoot(Paths.get(System.getProperty("user.dir")));
            if (projectRoot != null && !"/hst:hst".equals(projectRoot)) {
                candidates.add(projectRoot);
            }
            for (String hstRoot : candidates) {
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
}
