package org.bloomreach.forge.brut.common.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JcrTransactionSupportTest {

    private BrxmTestingRepository repository;
    private Session session;
    private JcrTransactionSupport txSupport;

    @BeforeEach
    void setUp() throws Exception {
        repository = new BrxmTestingRepository();
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        txSupport = new JcrTransactionSupport();
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void rollback_discardsContentSavedWithinTransaction() throws Exception {
        txSupport.begin(session);
        session.getRootNode().addNode("test-rollback-node", "nt:unstructured");
        session.save();
        assertTrue(session.getRootNode().hasNode("test-rollback-node"));

        txSupport.rollback();

        assertFalse(session.getRootNode().hasNode("test-rollback-node"));
    }

    @Test
    void commit_persistsContentSavedWithinTransaction() throws Exception {
        txSupport.begin(session);
        session.getRootNode().addNode("test-commit-node", "nt:unstructured");
        session.save();
        txSupport.commit();

        assertTrue(session.getRootNode().hasNode("test-commit-node"));
    }

    @Test
    void begin_afterRollback_allowsNewTransaction() throws Exception {
        txSupport.begin(session);
        txSupport.rollback();

        txSupport.begin(session);
        txSupport.rollback();
        // no exception → pass
    }
}
