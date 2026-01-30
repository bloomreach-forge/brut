package org.example;

import org.bloomreach.forge.brut.components.annotation.BrxmComponentTest;
import org.bloomreach.forge.brut.components.annotation.DynamicComponentTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Annotation-based component test example.
 * Demonstrates using @BrxmComponentTest with minimal configuration.
 */
@BrxmComponentTest(beanPackages = {"org.example.beans"})
public class AnnotationBasedComponentTest {

    @Test
    @DisplayName("Component infrastructure is initialized")
    void testInfrastructureSetup(DynamicComponentTest brxm) throws Exception {
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstResponse());
        assertNotNull(brxm.getHstRequestContext());
        assertNotNull(brxm.getRootNode());
        assertTrue(brxm.getRootNode().hasNode("hippo:configuration"));
    }
}
