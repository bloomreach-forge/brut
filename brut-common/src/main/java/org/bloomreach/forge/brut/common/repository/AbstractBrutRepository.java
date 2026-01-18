/*
 * Copyright 2024 Bloomreach, Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.brut.common.repository;

import org.apache.jackrabbit.commons.cnd.CndImporter;
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
import java.util.LinkedList;
import java.util.List;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;

/**
 * Abstract base class for BRUT repository implementations.
 * Provides common CND registration, namespace handling, and hippo path calculation methods.
 */
public abstract class AbstractBrutRepository extends BrxmTestingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBrutRepository.class);

    protected AbstractBrutRepository() throws RepositoryException, IOException {
        super();
    }

    /**
     * Registers CND node types from the given patterns.
     */
    protected void registerCnds(Session session, List<String> cndResourcesPatterns) throws RepositoryException {
        for (String cndResourcePattern : cndResourcesPatterns) {
            registerNamespaces(session, resolveResourcePattern(cndResourcePattern));
        }
    }

    /**
     * Registers namespaces from CND resources.
     */
    protected void registerNamespaces(Session session, Resource[] cndResources) throws RepositoryException {
        for (Resource cndResource : cndResources) {
            try {
                LOG.debug("Registering CND: {}", cndResource.getFilename());
                NodeType[] nodeTypes = CndImporter.registerNodeTypes(
                        new InputStreamReader(cndResource.getInputStream()), session);
                LOG.debug("Registered {} node type(s) from {}", nodeTypes.length, cndResource.getFilename());
                for (NodeType nt : nodeTypes) {
                    LOG.debug("  - {}", nt.getName());
                }
            } catch (Exception e) {
                throw new RepositoryException(
                        String.format("Failed to register CND file: %s%nCause: %s",
                                cndResource.getFilename(), e.getMessage()), e);
            }
        }
    }

    /**
     * Resolves resources matching the given classpath pattern.
     */
    protected Resource[] resolveResourcePattern(String pattern) throws RepositoryException {
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
            Resource[] resources = resolver.getResources(pattern);
            for (Resource resource : resources) {
                LOG.debug("RESOURCE: {}", resource.getFilename());
            }
            return resources;
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Recalculates hippo paths for HstQuery support.
     *
     * @param absolutePath absolute JCR path (e.g., "/content")
     */
    protected void recalculateHippoPaths(String absolutePath) {
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

    /**
     * Safely recalculates hippo paths only if the node exists.
     */
    protected void recalculateHippoPathsIfExists(String absolutePath) {
        Session session = null;
        try {
            session = this.login(new SimpleCredentials("admin", "admin".toCharArray()));
            if (session.getRootNode().hasNode(absolutePath.substring(1))) {
                recalculateHippoPaths(absolutePath);
                LOG.debug("Recalculated hippo paths for: {}", absolutePath);
            } else {
                LOG.debug("Skipping path recalculation: {} node does not exist", absolutePath);
            }
        } catch (RepositoryException e) {
            LOG.warn("Failed to check if {} exists: {}", absolutePath, e.getMessage());
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    protected LinkedList<String> getPathsForNode(Node node, Node rootNode) throws RepositoryException {
        LinkedList<String> paths = new LinkedList<>();
        Node parentNode = node;
        do {
            parentNode = parentNode.getParent();
            paths.add(parentNode.getIdentifier());
        } while (!parentNode.isSame(rootNode));
        return paths;
    }

    @SuppressWarnings("unchecked")
    protected void calculateHippoPaths(Node node, LinkedList<String> paths) throws RepositoryException {
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

    protected void setHippoPath(Node node, LinkedList<String> paths) throws RepositoryException {
        node.setProperty(HIPPO_PATHS, paths.toArray(new String[0]));
    }

    protected void setHandleHippoPaths(Node handle, LinkedList<String> paths) throws RepositoryException {
        paths.add(0, handle.getIdentifier());
        for (NodeIterator nodes = handle.getNodes(handle.getName()); nodes.hasNext(); ) {
            Node subnode = nodes.nextNode();
            paths.add(0, subnode.getIdentifier());
            setHippoPath(subnode, paths);
            paths.remove(0);
        }
    }
}
