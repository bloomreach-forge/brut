package org.bloomreach.forge.brut.common.repository.utils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.LinkedList;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;

/**
 * Utility methods for computing and setting {@code hippo:paths} properties on JCR nodes.
 * Used by both the component-test and resources modules to avoid duplicating this logic.
 */
public final class HippoPathUtils {

    private HippoPathUtils() {
    }

    /**
     * Collects the ancestor identifiers of {@code node} up to (but not including) {@code rootNode}.
     * The list is ordered from the immediate parent to the root.
     */
    public static LinkedList<String> getPathsForNode(Node node, Node rootNode) throws RepositoryException {
        LinkedList<String> paths = new LinkedList<>();
        Node parentNode = node;
        do {
            parentNode = parentNode.getParent();
            paths.add(parentNode.getIdentifier());
        } while (!parentNode.isSame(rootNode));
        return paths;
    }

    /**
     * Recursively sets {@code hippo:paths} on {@code node} and all its descendants,
     * skipping {@code hippo:handle} subtrees (handled via {@link #setHandleHippoPaths}) and
     * {@code hippotranslation:translations} subtrees.
     */
    @SuppressWarnings("unchecked")
    public static void calculateHippoPaths(Node node, LinkedList<String> paths) throws RepositoryException {
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

    public static void setHippoPath(Node node, LinkedList<String> paths) throws RepositoryException {
        node.setProperty(HIPPO_PATHS, paths.toArray(new String[0]));
    }

    public static void setHandleHippoPaths(Node handle, LinkedList<String> paths) throws RepositoryException {
        paths.add(0, handle.getIdentifier());
        for (NodeIterator nodes = handle.getNodes(handle.getName()); nodes.hasNext(); ) {
            Node subnode = nodes.nextNode();
            paths.add(0, subnode.getIdentifier());
            setHippoPath(subnode, paths);
            paths.remove(0);
        }
    }
}
