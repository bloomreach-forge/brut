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

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Auto-managed JCR session with fluent assertions for repository operations.
 * Implements AutoCloseable for automatic session cleanup.
 * Reduces repository access boilerplate from 7-9 lines to a single fluent chain.
 *
 * <p>Example usage:
 * <pre>
 * try (RepositorySession repo = RepositorySession.forRepository(repository)) {
 *     repo.assertNodeExists("/hst:myproject")
 *         .getNode("/hst:myproject")
 *             .assertPrimaryType("hst:hst");
 * }
 * </pre>
 */
public class RepositorySession implements AutoCloseable {

    private static final String DEFAULT_USERNAME = "admin";
    private static final char[] DEFAULT_PASSWORD = "admin".toCharArray();

    private final Session session;

    /**
     * Creates a RepositorySession with default admin credentials.
     *
     * @param repository JCR repository
     * @throws RepositoryException if login fails
     */
    public RepositorySession(Repository repository) throws RepositoryException {
        this(repository, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    /**
     * Creates a RepositorySession with custom credentials.
     *
     * @param repository JCR repository
     * @param username username for login
     * @param password password for login
     * @throws RepositoryException if login fails
     */
    public RepositorySession(Repository repository, String username, char[] password) throws RepositoryException {
        this.session = repository.login(new SimpleCredentials(username, password));
    }

    /**
     * Static factory method for creating RepositorySession with admin credentials.
     *
     * @param repository JCR repository
     * @return new RepositorySession
     */
    public static RepositorySession forRepository(Repository repository) {
        try {
            return new RepositorySession(repository);
        } catch (RepositoryException e) {
            throw new AssertionError("Failed to create repository session: " + e.getMessage(), e);
        }
    }

    /**
     * Asserts that a node exists at the specified path.
     *
     * @param path absolute node path
     * @return this for chaining
     */
    public RepositorySession assertNodeExists(String path) {
        try {
            assertTrue(session.nodeExists(path),
                      "Expected node to exist at: " + path);
        } catch (RepositoryException e) {
            fail("Failed to check node existence at " + path + ": " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that a node does NOT exist at the specified path.
     *
     * @param path absolute node path
     * @return this for chaining
     */
    public RepositorySession assertNodeNotExists(String path) {
        try {
            assertFalse(session.nodeExists(path),
                       "Expected node NOT to exist at: " + path);
        } catch (RepositoryException e) {
            fail("Failed to check node existence at " + path + ": " + e.getMessage());
        }
        return this;
    }

    /**
     * Gets a node and returns a NodeAssert for fluent assertions.
     *
     * @param path absolute node path
     * @return NodeAssert for the node
     */
    public NodeAssert getNode(String path) {
        try {
            Node node = session.getNode(path);
            return new NodeAssert(node);
        } catch (RepositoryException e) {
            fail("Failed to get node at " + path + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a node at the specified path with the given primary type.
     * Creates parent nodes as needed.
     *
     * @param path absolute node path
     * @param primaryType JCR primary type (e.g., "hippostd:folder")
     * @return this for chaining
     */
    public RepositorySession createNode(String path, String primaryType) {
        try {
            // Split path and create nodes recursively
            String[] parts = path.substring(1).split("/");
            Node current = session.getRootNode();

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (current.hasNode(part)) {
                    current = current.getNode(part);
                } else {
                    // For last part, use specified type; for intermediates, use nt:unstructured
                    String nodeType = (i == parts.length - 1) ? primaryType : "nt:unstructured";
                    current = current.addNode(part, nodeType);
                }
            }
        } catch (RepositoryException e) {
            fail("Failed to create node at " + path + ": " + e.getMessage());
        }
        return this;
    }

    /**
     * Saves the session.
     *
     * @return this for chaining
     */
    public RepositorySession save() {
        try {
            session.save();
        } catch (RepositoryException e) {
            fail("Failed to save session: " + e.getMessage());
        }
        return this;
    }

    /**
     * Refreshes the session.
     *
     * @param keepChanges whether to keep unsaved changes
     * @return this for chaining
     */
    public RepositorySession refresh(boolean keepChanges) {
        try {
            session.refresh(keepChanges);
        } catch (RepositoryException e) {
            fail("Failed to refresh session: " + e.getMessage());
        }
        return this;
    }

    /**
     * Gets the underlying JCR session for advanced operations.
     *
     * @return JCR session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the root node.
     *
     * @return NodeAssert for root node
     */
    public NodeAssert getRootNode() {
        try {
            return new NodeAssert(session.getRootNode());
        } catch (RepositoryException e) {
            fail("Failed to get root node: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }
}
