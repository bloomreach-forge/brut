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

    private DynamicComponentTest brxm;

    @Test
    @DisplayName("Component infrastructure is initialized")
    void testInfrastructureSetup() throws Exception {
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstResponse());
        assertNotNull(brxm.getHstRequestContext());
        assertNotNull(brxm.getRootNode());
        assertTrue(brxm.getRootNode().hasNode("hippo:configuration"));
    }
}
