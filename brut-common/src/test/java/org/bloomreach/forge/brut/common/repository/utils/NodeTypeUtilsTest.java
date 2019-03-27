package org.bloomreach.forge.brut.common.repository.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.jupiter.api.Test;

import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class NodeTypeUtilsTest {

    private static final String NODE_TYPE = "mysn:type";
    private static final String MIXIN_TYPE = "mysn:mixin";
    private static final String PARENT_MIXIN_TYPE = "mysn:parentMixin";
    private static final String PARENT_NODE_TYPE = "mysn:parentType";
    private static final String OTHER_NAMESPACE_PARENT_MIXIN_TYPE = "otherns:parentMixin";
    private static final String OTHER_NAMESPACE_PARENT_NODE_TYPE = "otherns:parentType";

    @Test
    public void createNodeType() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            NodeTypeUtils.createNodeType(session, NODE_TYPE);
            Node node = session.getRootNode().addNode("test", NODE_TYPE);
            assertTrue(node.isNodeType(NODE_TYPE));
        }
    }

    @Test
    public void createNodeTypeWithSuperType() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            testCreatingNodeTypeWithSuperType(PARENT_NODE_TYPE, session);
        }
    }

    @Test
    public void createNodeTypeWithRegisteredSuperType() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            NodeTypeUtils.createNodeType(session, OTHER_NAMESPACE_PARENT_NODE_TYPE);
            testCreatingNodeTypeWithSuperType(OTHER_NAMESPACE_PARENT_NODE_TYPE, session);
        }
    }

    @Test
    public void createNodeTypeWithSuperTypeOfAnotherNamespace() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            testCreatingNodeTypeWithSuperType(OTHER_NAMESPACE_PARENT_NODE_TYPE, session);
        }
    }

    @Test
    public void createMixin() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            NodeTypeUtils.createNodeType(session, NODE_TYPE);
            NodeTypeUtils.createMixin(session, MIXIN_TYPE);
            Node node = session.getRootNode().addNode("test", NODE_TYPE);
            node.addMixin(MIXIN_TYPE);
            assertTrue(node.isNodeType(NODE_TYPE));
            assertTrue(node.isNodeType(MIXIN_TYPE));
            session.save();
        }
    }

    @Test
    public void createMixinWithSuperType() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            testCreatingMixinWithSuperType(PARENT_MIXIN_TYPE, session);
        }
    }

    @Test
    public void createMixinWithRegisteredSuperType() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            NodeTypeUtils.createMixin(session, PARENT_MIXIN_TYPE);
            testCreatingMixinWithSuperType(PARENT_MIXIN_TYPE, session);
        }
    }

    @Test
    public void createMixinWithSuperTypeOfAnotherNamespace() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            testCreatingMixinWithSuperType(OTHER_NAMESPACE_PARENT_MIXIN_TYPE, session);
        }
    }

    @Test
    public void getOrRegisterNamespace() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            NodeTypeUtils.createNodeType(session, NODE_TYPE);
            Node node = session.getRootNode().addNode("test", NODE_TYPE);
            NodeTypeUtils.getOrRegisterNamespace(session, "test:test");
            node.setProperty("test:test", "test");
        }
    }

    private void testCreatingMixinWithSuperType(String parentMixinType, Session session) throws RepositoryException {
        NodeTypeUtils.createNodeType(session, NODE_TYPE);
        NodeTypeUtils.createMixin(session, MIXIN_TYPE, parentMixinType);
        Node node = session.getRootNode().addNode("test", NODE_TYPE);
        node.addMixin(MIXIN_TYPE);
        assertTrue(node.isNodeType(NODE_TYPE));
        assertTrue(node.isNodeType(MIXIN_TYPE));
        assertTrue(node.isNodeType(parentMixinType));
        session.save();
    }

    private void testCreatingNodeTypeWithSuperType(String parentNodeType, Session session) throws RepositoryException {
        NodeTypeUtils.createNodeType(session, NODE_TYPE, parentNodeType);
        Node node = session.getRootNode().addNode("test", NODE_TYPE);

        // making sure that new time is unstructured.
        node.setProperty("propertyName", "propertyValue");

        assertTrue(node.isNodeType(NODE_TYPE));
        assertTrue(node.isNodeType(parentNodeType));
        Node nodeOfParentType = session.getRootNode().addNode("test2", parentNodeType);
        assertFalse(nodeOfParentType.isNodeType(NODE_TYPE));
        assertTrue(nodeOfParentType.isNodeType(parentNodeType));
        session.save();
    }


}