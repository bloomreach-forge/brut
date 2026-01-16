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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fluent assertions for JCR nodes.
 * Provides readable assertions for node properties, children, and structure.
 *
 * <p>Example usage:
 * <pre>
 * repo.getNode("/hst:myproject")
 *     .assertPrimaryType("hst:hst")
 *     .assertHasNode("hst:configurations")
 *     .assertProperty("jcr:primaryType", "hst:hst");
 * </pre>
 */
public class NodeAssert {

    private final Node node;

    /**
     * Creates a NodeAssert for the specified node.
     *
     * @param node JCR node to assert on
     */
    public NodeAssert(Node node) {
        this.node = node;
    }

    /**
     * Asserts that the node has a property with the expected string value.
     *
     * @param name property name
     * @param expected expected property value
     * @return this for chaining
     */
    public NodeAssert assertProperty(String name, String expected) {
        try {
            assertTrue(node.hasProperty(name),
                      "Expected property '" + name + "' to exist on node " + node.getPath());
            Property property = node.getProperty(name);
            assertEquals(expected, property.getString(),
                        "Property '" + name + "' value mismatch");
        } catch (RepositoryException e) {
            fail("Failed to check property '" + name + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that the node has a property with the expected long value.
     *
     * @param name property name
     * @param expected expected property value
     * @return this for chaining
     */
    public NodeAssert assertPropertyLong(String name, long expected) {
        try {
            assertTrue(node.hasProperty(name),
                      "Expected property '" + name + "' to exist on node " + node.getPath());
            Property property = node.getProperty(name);
            assertEquals(expected, property.getLong(),
                        "Property '" + name + "' value mismatch");
        } catch (RepositoryException e) {
            fail("Failed to check property '" + name + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that the node has a property with the expected boolean value.
     *
     * @param name property name
     * @param expected expected property value
     * @return this for chaining
     */
    public NodeAssert assertPropertyBoolean(String name, boolean expected) {
        try {
            assertTrue(node.hasProperty(name),
                      "Expected property '" + name + "' to exist on node " + node.getPath());
            Property property = node.getProperty(name);
            assertEquals(expected, property.getBoolean(),
                        "Property '" + name + "' value mismatch");
        } catch (RepositoryException e) {
            fail("Failed to check property '" + name + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that the node has a property (without checking value).
     *
     * @param name property name
     * @return this for chaining
     */
    public NodeAssert assertHasProperty(String name) {
        try {
            assertTrue(node.hasProperty(name),
                      "Expected property '" + name + "' to exist on node " + node.getPath());
        } catch (RepositoryException e) {
            fail("Failed to check property '" + name + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that the node has a child node at the relative path.
     *
     * @param relPath relative path to child node
     * @return this for chaining
     */
    public NodeAssert assertHasNode(String relPath) {
        try {
            assertTrue(node.hasNode(relPath),
                      "Expected child node '" + relPath + "' to exist under " + node.getPath());
        } catch (RepositoryException e) {
            fail("Failed to check child node '" + relPath + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that the node does NOT have a child node at the relative path.
     *
     * @param relPath relative path to child node
     * @return this for chaining
     */
    public NodeAssert assertNotHasNode(String relPath) {
        try {
            assertFalse(node.hasNode(relPath),
                       "Expected child node '" + relPath + "' NOT to exist under " + node.getPath());
        } catch (RepositoryException e) {
            fail("Failed to check child node '" + relPath + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that the node has the expected primary type.
     *
     * @param expected expected primary type (e.g., "hst:hst")
     * @return this for chaining
     */
    public NodeAssert assertPrimaryType(String expected) {
        return assertProperty("jcr:primaryType", expected);
    }

    /**
     * Asserts that the node name matches the expected value.
     *
     * @param expected expected node name
     * @return this for chaining
     */
    public NodeAssert assertName(String expected) {
        try {
            assertEquals(expected, node.getName(),
                        "Node name mismatch");
        } catch (RepositoryException e) {
            fail("Failed to get node name: " + e.getMessage());
        }
        return this;
    }

    /**
     * Asserts that the node path matches the expected value.
     *
     * @param expected expected node path
     * @return this for chaining
     */
    public NodeAssert assertPath(String expected) {
        try {
            assertEquals(expected, node.getPath(),
                        "Node path mismatch");
        } catch (RepositoryException e) {
            fail("Failed to get node path: " + e.getMessage());
        }
        return this;
    }

    /**
     * Gets a child node and returns a NodeAssert for it.
     *
     * @param relPath relative path to child node
     * @return NodeAssert for child node
     */
    public NodeAssert getChildNode(String relPath) {
        try {
            Node childNode = node.getNode(relPath);
            return new NodeAssert(childNode);
        } catch (RepositoryException e) {
            fail("Failed to get child node '" + relPath + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the underlying JCR node for advanced operations.
     *
     * @return JCR node
     */
    public Node getNode() {
        return node;
    }
}
