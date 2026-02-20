package org.bloomreach.forge.brut.components.annotation;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BrxmComponentTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BrxmComponentTestExtensionLifecycleIT {

    DynamicComponentTest brxm;

    @Test
    @Order(1)
    void test1_importsContent() throws Exception {
        brxm.importYaml("/lifecycle-test.yaml", "/content/documents");
        brxm.recalculateRepositoryPaths();
        assertTrue(brxm.getRootNode().hasNode("content/documents/lifecycle-node"));
    }

    @Test
    @Order(2)
    void test2_contentFromTest1IsGone() throws Exception {
        assertFalse(brxm.getRootNode().hasNode("content/documents/lifecycle-node"));
    }

    @Test
    @Order(3)
    void test3_nodeTypesStillAvailable() throws Exception {
        assertNotNull(brxm.getRootNode().getSession()
                .getWorkspace().getNodeTypeManager().getNodeType("hippostd:folder"));
    }
}
