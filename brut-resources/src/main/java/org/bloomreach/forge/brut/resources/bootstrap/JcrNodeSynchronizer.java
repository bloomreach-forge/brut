package org.bloomreach.forge.brut.resources.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles JCR node synchronization between source and target trees.
 * <p>
 * Provides recursive node copying and synchronization with support for:
 * <ul>
 *   <li>Property copying (single and multi-valued)</li>
 *   <li>Mixin type preservation</li>
 *   <li>Child node ordering for orderable node types</li>
 *   <li>Primary type mismatch handling (replace vs sync)</li>
 * </ul>
 *
 * @since 5.2.0
 */
public final class JcrNodeSynchronizer {

    private static final Logger LOG = LoggerFactory.getLogger(JcrNodeSynchronizer.class);

    private JcrNodeSynchronizer() {
        // Utility class
    }

    /**
     * Synchronizes HST sites from standard root to project-specific root.
     *
     * @param standardRoot   the source /hst:hst node
     * @param projectRoot    the target /hst:projectname node
     * @param projectHstRoot the project root path (for logging)
     * @throws RepositoryException if sync fails
     */
    public static void syncProjectSite(Node standardRoot,
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
            LOG.debug("Synced {} site(s) at {}/hst:sites", orderedChildren.size(), projectHstRoot);
        }
    }

    /**
     * Synchronizes HST configurations from standard root to project-specific root.
     *
     * @param standardRoot   the source /hst:hst node
     * @param projectRoot    the target /hst:projectname node
     * @param projectHstRoot the project root path (for logging)
     * @throws RepositoryException if sync fails
     */
    public static void syncProjectConfiguration(Node standardRoot,
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
            LOG.debug("Synced {} configuration(s) at {}/hst:configurations", orderedChildren.size(), projectHstRoot);
        }
    }

    /**
     * Synchronizes HST hosts from standard root to project-specific root.
     *
     * @param standardRoot   the source /hst:hst node
     * @param projectRoot    the target /hst:projectname node
     * @param projectHstRoot the project root path (for logging)
     * @throws RepositoryException if sync fails
     */
    public static void syncProjectHosts(Node standardRoot,
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
        LOG.debug("Synced project hosts at {}/hst:hosts", projectHstRoot);
    }

    /**
     * Gets or creates a child node with the given name and primary type.
     *
     * @param parent      the parent node
     * @param name        the child node name
     * @param primaryType the primary type to use if creating
     * @return the existing or newly created child node
     * @throws RepositoryException if operation fails
     */
    public static Node getOrAddChild(Node parent, String name, String primaryType) throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        }
        return parent.addNode(name, primaryType);
    }

    /**
     * Synchronizes a source node to a target node recursively.
     * <p>
     * Copies all properties, mixins, and children from source to target.
     * Handles primary type mismatches by replacing the target node.
     *
     * @param source the source node
     * @param target the target node
     * @throws RepositoryException if sync fails
     */
    public static void syncNodeRecursively(Node source, Node target) throws RepositoryException {
        for (NodeType mixin : source.getMixinNodeTypes()) {
            if (!target.isNodeType(mixin.getName())) {
                target.addMixin(mixin.getName());
            }
        }

        for (PropertyIterator props = source.getProperties(); props.hasNext(); ) {
            Property prop = props.nextProperty();
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

    /**
     * Copies a source node recursively to a parent node.
     *
     * @param source the source node to copy
     * @param parent the parent node to copy into
     * @throws RepositoryException if copy fails
     */
    public static void copyNodeRecursively(Node source, Node parent) throws RepositoryException {
        Node copy = parent.addNode(source.getName(), source.getPrimaryNodeType().getName());

        for (NodeType mixin : source.getMixinNodeTypes()) {
            copy.addMixin(mixin.getName());
        }

        for (PropertyIterator props = source.getProperties(); props.hasNext(); ) {
            Property prop = props.nextProperty();
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
}
