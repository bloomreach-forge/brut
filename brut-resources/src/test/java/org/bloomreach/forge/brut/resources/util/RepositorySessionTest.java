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
package org.bloomreach.forge.brut.resources.util;

import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RepositorySession fluent API.
 */
class RepositorySessionTest {

    private BrxmTestingRepository testRepository;
    private Repository repository;

    @BeforeEach
    void setUp() throws Exception {
        testRepository = new BrxmTestingRepository();
        repository = testRepository;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testRepository != null) {
            testRepository.shutdown();
        }
    }

    @Test
    @DisplayName("Constructor creates session with default credentials")
    void testConstructor() throws RepositoryException {
        try (RepositorySession repoSession = new RepositorySession(repository)) {
            assertNotNull(repoSession.getSession());
            assertTrue(repoSession.getSession().isLive());
        }
    }

    @Test
    @DisplayName("Constructor creates session with custom credentials")
    void testConstructorCustomCredentials() throws RepositoryException {
        try (RepositorySession repoSession = new RepositorySession(repository, "admin", "admin".toCharArray())) {
            assertNotNull(repoSession.getSession());
            assertTrue(repoSession.getSession().isLive());
        }
    }

    @Test
    @DisplayName("forRepository() static factory creates session")
    void testForRepositoryFactory() {
        RepositorySession repoSession = RepositorySession.forRepository(repository);
        assertNotNull(repoSession);
        assertNotNull(repoSession.getSession());
        assertTrue(repoSession.getSession().isLive());
        repoSession.close();
    }

    @Test
    @DisplayName("assertNodeExists() succeeds for existing node")
    void testAssertNodeExistsSuccess() throws Exception {
        // Create a test node first
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        session.getRootNode().addNode("testnode", "nt:unstructured");
        session.save();
        session.logout();

        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            assertDoesNotThrow(() ->
                repoSession.assertNodeExists("/testnode")
            );
        }
    }

    @Test
    @DisplayName("assertNodeExists() fails for non-existing node")
    void testAssertNodeExistsFailure() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            assertThrows(AssertionError.class, () ->
                repoSession.assertNodeExists("/nonexistent")
            );
        }
    }

    @Test
    @DisplayName("assertNodeNotExists() succeeds for non-existing node")
    void testAssertNodeNotExistsSuccess() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            assertDoesNotThrow(() ->
                repoSession.assertNodeNotExists("/nonexistent")
            );
        }
    }

    @Test
    @DisplayName("assertNodeNotExists() fails for existing node")
    void testAssertNodeNotExistsFailure() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            assertThrows(AssertionError.class, () ->
                repoSession.assertNodeNotExists("/")
            );
        }
    }

    @Test
    @DisplayName("getNode() returns NodeAssert for existing node")
    void testGetNode() throws Exception {
        // Create a test node
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Node testNode = session.getRootNode().addNode("testnode", "nt:unstructured");
        testNode.setProperty("testprop", "testvalue");
        session.save();
        session.logout();

        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            NodeAssert nodeAssert = repoSession.getNode("/testnode");
            assertNotNull(nodeAssert);
            assertNotNull(nodeAssert.getNode());
        }
    }

    @Test
    @DisplayName("getNode() fails for non-existing node")
    void testGetNodeFailure() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            assertThrows(AssertionError.class, () ->
                repoSession.getNode("/nonexistent")
            );
        }
    }

    @Test
    @DisplayName("createNode() creates node with primary type")
    void testCreateNode() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            repoSession.createNode("/testfolder", "nt:folder")
                      .save();

            // Verify node was created
            repoSession.assertNodeExists("/testfolder");
        }
    }

    @Test
    @DisplayName("createNode() creates nested path with intermediates")
    void testCreateNodeNested() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            repoSession.createNode("/parent/child/grandchild", "nt:folder")
                      .save();

            // Verify all nodes were created
            repoSession.assertNodeExists("/parent")
                      .assertNodeExists("/parent/child")
                      .assertNodeExists("/parent/child/grandchild");
        }
    }

    @Test
    @DisplayName("save() persists changes")
    void testSave() throws Exception {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            Node root = repoSession.getSession().getRootNode();
            root.addNode("testnode", "nt:unstructured");
            repoSession.save();
        }

        // Verify in new session
        Session verifySession = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertTrue(verifySession.nodeExists("/testnode"));
        verifySession.logout();
    }

    @Test
    @DisplayName("refresh() with keepChanges=false discards changes")
    void testRefreshDiscardChanges() throws Exception {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            Session session = repoSession.getSession();
            session.getRootNode().addNode("testnode", "nt:unstructured");

            // Discard changes
            repoSession.refresh(false);

            assertFalse(session.nodeExists("/testnode"), "Node should not exist after refresh(false)");
        }
    }

    @Test
    @DisplayName("refresh() with keepChanges=true preserves changes")
    void testRefreshKeepChanges() throws Exception {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            Session session = repoSession.getSession();
            session.getRootNode().addNode("testnode", "nt:unstructured");

            // Keep changes
            repoSession.refresh(true);

            assertTrue(session.nodeExists("/testnode"), "Node should exist after refresh(true)");
        }
    }

    @Test
    @DisplayName("getRootNode() returns NodeAssert for root")
    void testGetRootNode() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            NodeAssert rootAssert = repoSession.getRootNode();
            assertNotNull(rootAssert);
            assertNotNull(rootAssert.getNode());
        }
    }

    @Test
    @DisplayName("getSession() returns underlying JCR session")
    void testGetSession() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            Session session = repoSession.getSession();
            assertNotNull(session);
            assertTrue(session.isLive());
        }
    }

    @Test
    @DisplayName("close() logs out session")
    void testClose() throws Exception {
        RepositorySession repoSession = RepositorySession.forRepository(repository);
        Session session = repoSession.getSession();
        assertTrue(session.isLive());

        repoSession.close();

        assertFalse(session.isLive(), "Session should be logged out after close()");
    }

    @Test
    @DisplayName("AutoCloseable automatically logs out session")
    void testAutoCloseable() throws Exception {
        Session session;
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            session = repoSession.getSession();
            assertTrue(session.isLive());
        }

        assertFalse(session.isLive(), "Session should be logged out after try-with-resources");
    }

    @Test
    @DisplayName("Fluent chaining works correctly")
    void testFluentChaining() {
        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            assertDoesNotThrow(() ->
                repoSession.createNode("/test1", "nt:folder")
                          .createNode("/test2", "nt:folder")
                          .save()
                          .assertNodeExists("/test1")
                          .assertNodeExists("/test2")
                          .assertNodeNotExists("/test3")
            );
        }
    }

    @Test
    @DisplayName("getNode() and NodeAssert integration")
    void testGetNodeAndNodeAssert() throws Exception {
        // Setup test node with properties
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Node testNode = session.getRootNode().addNode("testnode", "nt:unstructured");
        testNode.setProperty("testprop", "testvalue");
        testNode.addNode("childnode", "nt:unstructured");
        session.save();
        session.logout();

        try (RepositorySession repoSession = RepositorySession.forRepository(repository)) {
            assertDoesNotThrow(() ->
                repoSession.getNode("/testnode")
                          .assertProperty("testprop", "testvalue")
                          .assertHasNode("childnode")
            );
        }
    }
}
