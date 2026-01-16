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
        String user = "test-user";
        brxm.getHstRequest().setRequestURI("/site/api/hello/" + user);

        String response = brxm.invokeFilter();

        assertEquals("Hello, World! " + user, response);
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

        // Verify repository structure is loaded
        javax.jcr.Repository repository = brxm.getComponentManager().getComponent(javax.jcr.Repository.class);
        assertNotNull(repository);

        javax.jcr.Session session = null;
        try {
            session = repository.login(new javax.jcr.SimpleCredentials("admin", "admin".toCharArray()));

            // Verify HST configuration is loaded
            assertTrue(session.nodeExists("/hst:myproject"), "HST root should exist");
            assertTrue(session.nodeExists("/hst:myproject/hst:configurations"), "HST configurations should exist");
            assertTrue(session.nodeExists("/hippo:configuration"), "Hippo configuration should exist");
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
}
