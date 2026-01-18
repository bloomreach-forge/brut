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
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NodeAssert fluent API.
 */
class NodeAssertTest {

    private BrxmTestingRepository testRepository;
    private Repository repository;
    private Session session;
    private Node testNode;

    @BeforeEach
    void setUp() throws Exception {
        testRepository = new BrxmTestingRepository();
        repository = testRepository;
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        // Create test node structure
        testNode = session.getRootNode().addNode("testnode", "nt:unstructured");
        testNode.setProperty("stringprop", "testvalue");
        testNode.setProperty("longprop", 123456789L);
        testNode.setProperty("booleanprop", true);
        testNode.addNode("childnode", "nt:folder");
        session.save();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
        if (testRepository != null) {
            testRepository.shutdown();
        }
    }

    @Test
    @DisplayName("Constructor creates NodeAssert")
    void testConstructor() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertNotNull(nodeAssert);
    }

    @Test
    @DisplayName("assertProperty() succeeds for correct string value")
    void testAssertPropertySuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertProperty("stringprop", "testvalue")
        );
    }

    @Test
    @DisplayName("assertProperty() fails for wrong value")
    void testAssertPropertyWrongValue() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertProperty("stringprop", "wrongvalue")
        );
    }

    @Test
    @DisplayName("assertProperty() fails for non-existing property")
    void testAssertPropertyNotExists() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertProperty("nonexistent", "value")
        );
    }

    @Test
    @DisplayName("assertPropertyLong() succeeds for correct long value")
    void testAssertPropertyLongSuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertPropertyLong("longprop", 123456789L)
        );
    }

    @Test
    @DisplayName("assertPropertyLong() fails for wrong value")
    void testAssertPropertyLongWrongValue() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertPropertyLong("longprop", 999L)
        );
    }

    @Test
    @DisplayName("assertPropertyBoolean() succeeds for correct boolean value")
    void testAssertPropertyBooleanSuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertPropertyBoolean("booleanprop", true)
        );
    }

    @Test
    @DisplayName("assertPropertyBoolean() fails for wrong value")
    void testAssertPropertyBooleanWrongValue() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertPropertyBoolean("booleanprop", false)
        );
    }

    @Test
    @DisplayName("assertHasProperty() succeeds for existing property")
    void testAssertHasPropertySuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertHasProperty("stringprop")
        );
    }

    @Test
    @DisplayName("assertHasProperty() fails for non-existing property")
    void testAssertHasPropertyFailure() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertHasProperty("nonexistent")
        );
    }

    @Test
    @DisplayName("assertHasNode() succeeds for existing child node")
    void testAssertHasNodeSuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertHasNode("childnode")
        );
    }

    @Test
    @DisplayName("assertHasNode() fails for non-existing child node")
    void testAssertHasNodeFailure() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertHasNode("nonexistentchild")
        );
    }

    @Test
    @DisplayName("assertNotHasNode() succeeds for non-existing child node")
    void testAssertNotHasNodeSuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertNotHasNode("nonexistentchild")
        );
    }

    @Test
    @DisplayName("assertNotHasNode() fails for existing child node")
    void testAssertNotHasNodeFailure() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertNotHasNode("childnode")
        );
    }

    @Test
    @DisplayName("assertPrimaryType() succeeds for correct type")
    void testAssertPrimaryTypeSuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertPrimaryType("nt:unstructured")
        );
    }

    @Test
    @DisplayName("assertPrimaryType() fails for wrong type")
    void testAssertPrimaryTypeFailure() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertPrimaryType("nt:folder")
        );
    }

    @Test
    @DisplayName("assertName() succeeds for correct node name")
    void testAssertNameSuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertName("testnode")
        );
    }

    @Test
    @DisplayName("assertName() fails for wrong node name")
    void testAssertNameFailure() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertName("wrongname")
        );
    }

    @Test
    @DisplayName("assertPath() succeeds for correct path")
    void testAssertPathSuccess() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertPath("/testnode")
        );
    }

    @Test
    @DisplayName("assertPath() fails for wrong path")
    void testAssertPathFailure() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.assertPath("/wrongpath")
        );
    }

    @Test
    @DisplayName("getChildNode() returns NodeAssert for child")
    void testGetChildNode() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        NodeAssert childAssert = nodeAssert.getChildNode("childnode");

        assertNotNull(childAssert);
        assertDoesNotThrow(() ->
            childAssert.assertName("childnode")
        );
    }

    @Test
    @DisplayName("getChildNode() fails for non-existing child")
    void testGetChildNodeFailure() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertThrows(AssertionError.class, () ->
            nodeAssert.getChildNode("nonexistent")
        );
    }

    @Test
    @DisplayName("getNode() returns underlying JCR node")
    void testGetNode() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        Node node = nodeAssert.getNode();

        assertNotNull(node);
        assertEquals(testNode, node);
    }

    @Test
    @DisplayName("Fluent chaining works correctly")
    void testFluentChaining() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert
                .assertName("testnode")
                .assertPath("/testnode")
                .assertPrimaryType("nt:unstructured")
                .assertProperty("stringprop", "testvalue")
                .assertPropertyLong("longprop", 123456789L)
                .assertPropertyBoolean("booleanprop", true)
                .assertHasNode("childnode")
                .assertNotHasNode("nonexistent")
        );
    }

    @Test
    @DisplayName("Child node navigation and assertions")
    void testChildNodeNavigation() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertHasNode("childnode")
                     .getChildNode("childnode")
                         .assertName("childnode")
                         .assertPrimaryType("nt:folder")
        );
    }

    @Test
    @DisplayName("Multiple property assertions")
    void testMultiplePropertyAssertions() {
        NodeAssert nodeAssert = new NodeAssert(testNode);
        assertDoesNotThrow(() ->
            nodeAssert.assertHasProperty("stringprop")
                     .assertHasProperty("longprop")
                     .assertHasProperty("booleanprop")
                     .assertHasProperty("jcr:primaryType")
        );
    }
}
