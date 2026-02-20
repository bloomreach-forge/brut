package org.bloomreach.forge.brut.common.repository;

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

import org.junit.jupiter.api.Test;

import org.bloomreach.forge.brut.common.repository.utils.NodeTypeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BrxmTestingRepositoryTest {

    private static final String NODE_TYPE = "ns:MyNodeType";
    private static final String NODE_NAME = "mynode";

    @Test
    public void creatingRepository() throws Exception {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(
                    new SimpleCredentials("admin", "admin".toCharArray())
            );
            addSampleNode(session);
        }
    }

    @Test
    public void search() throws IOException, RepositoryException, URISyntaxException {
        try (BrxmTestingRepository repository = new BrxmTestingRepository()) {
            Session session = repository.login(
                    new SimpleCredentials("admin", "admin".toCharArray())
            );
            addSampleNode(session);

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("SELECT * FROM [ns:MyNodeType]", Query.JCR_SQL2);
            QueryResult execute = query.execute();
            NodeIterator nodes = execute.getNodes();

            assertEquals(1, nodes.getSize());
            assertEquals(NODE_NAME, nodes.nextNode().getName());
        }
    }

    @Test
    void close_whenManaged_doesNotShutdown() throws Exception {
        BrxmTestingRepository repo = new BrxmTestingRepository();
        repo.setManaged(true);
        repo.close();
        Session session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertNotNull(session);
        session.logout();
        repo.forceClose();
    }

    @Test
    void forceClose_alwaysShutdowns() throws Exception {
        BrxmTestingRepository repo = new BrxmTestingRepository();
        repo.setManaged(true);
        repo.forceClose();
        assertThrows(Exception.class, () -> repo.login(new SimpleCredentials("admin", "admin".toCharArray())));
    }

    private void addSampleNode(Session session) throws RepositoryException {
        Node rootNode = session.getRootNode();
        NodeTypeUtils.createNodeType(session, NODE_TYPE);
        rootNode.addNode(NODE_NAME, NODE_TYPE);
        session.save();
    }

}