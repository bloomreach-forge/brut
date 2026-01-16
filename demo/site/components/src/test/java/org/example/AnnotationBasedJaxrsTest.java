package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Annotation-based JAX-RS test example.
 * Demonstrates using @BrxmJaxrsTest with minimal configuration.
 * Only beanPackages and springConfigs need to be specified - HST root is auto-detected.
 */
@BrxmJaxrsTest(
        beanPackages = {"org.example.model"},
        springConfigs = {"/org/example/annotation-jaxrs.xml", "/org/example/rest-resources.xml"}
)
public class AnnotationBasedJaxrsTest {

    private DynamicJaxrsTest brxm;

    @Test
    @DisplayName("JAX-RS endpoint with minimal configuration")
    void testJaxrsEndpoint() {
        // Using fluent RequestBuilder (reduces 4 lines to 1 fluent chain)
        String user = "test-user";
        brxm.request()
                .get("/site/api/hello/" + user)
                .assertBody("Hello, World! " + user);
    }

    @Test
    @DisplayName("Second test method works correctly")
    void testSecondRequest() {
        // Verifies beforeEach setup works across multiple test methods
        brxm.getHstRequest().setRequestURI("/site/api/hello/another-user");
        String response = brxm.invokeFilter();
        assertEquals("Hello, World! another-user", response);
    }

    @Test
    @DisplayName("Request infrastructure is properly set up")
    void testRequestSetup() throws Exception {
        // Verify HST request is set up correctly
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstRequest().getHeader("Host"));

        // Verify component manager is available
        assertNotNull(brxm.getComponentManager());

        // Using fluent RepositorySession (auto-cleanup with try-with-resources)
        try (var repo = brxm.repository()) {
            repo.assertNodeExists("/hst:myproject")
                .assertNodeExists("/hst:myproject/hst:configurations")
                .assertNodeExists("/hippo:configuration");
        }
    }
}
