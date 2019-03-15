package com.bloomreach.ps.brxm.jcr.repository;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.junit.Assert;
import org.junit.Test;

import com.bloomreach.ps.brxm.jcr.repository.utils.NodeTypeUtils;

import static javax.jcr.query.Query.XPATH;

public class InMemoryJcrRepositoryTest {

    private static final String NODE_TYPE = "ns:MyNodeType";
    private static final String NODE_NAME = "mynode";

    @Test
    public void creatingRepository() throws Exception {
        try (InMemoryJcrRepository repository = new InMemoryJcrRepository()) {
            Session session = repository.login(
                    new SimpleCredentials("admin", "admin".toCharArray())
            );
            addSampleNode(session);
        }
    }

    @Test
    public void search() throws IOException, RepositoryException, URISyntaxException {
        try (InMemoryJcrRepository repository = new InMemoryJcrRepository()) {
            Session session = repository.login(
                    new SimpleCredentials("admin", "admin".toCharArray())
            );
            addSampleNode(session);

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("//element(*,ns:MyNodeType)", XPATH);
            QueryResult execute = query.execute();
            NodeIterator nodes = execute.getNodes();

            Assert.assertEquals(1, nodes.getSize());
            Assert.assertEquals(NODE_NAME, nodes.nextNode().getName());
        }
    }

    private void addSampleNode(Session session) throws RepositoryException {
        Node rootNode = session.getRootNode();
        NodeTypeUtils.createNodeType(session, NODE_TYPE);
        rootNode.addNode(NODE_NAME, NODE_TYPE);
        session.save();
    }

}