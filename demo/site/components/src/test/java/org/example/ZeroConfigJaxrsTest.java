package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.bloomreach.forge.brut.resources.util.Response;
import org.example.rest.HelloResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TRUE zero-config JAX-RS test - NO Spring XML, NO explicit patterns.
 *
 * <p>This test demonstrates the absolute minimum configuration:</p>
 * <ul>
 *   <li>{@code beanPackages} - for HST content bean scanning</li>
 *   <li>{@code resources} - for JAX-RS endpoint registration</li>
 * </ul>
 *
 * <p>Everything else is auto-detected:</p>
 * <ul>
 *   <li>HST root from project name</li>
 *   <li>Test YAML from {@code <testPackage>/imports/}*.yaml</li>
 *   <li>Project content via ConfigService</li>
 *   <li>Jackson JSON support</li>
 * </ul>
 */
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    resources = {HelloResource.class}
)
public class ZeroConfigJaxrsTest {

    @Test
    @DisplayName("Zero config - endpoint works via auto-detected YAML")
    void zeroConfig_endpointWorks(DynamicJaxrsTest brxm) {
        brxm.request()
            .get("/site/api/hello/zero-config")
            .assertBody("Hello, World! zero-config");
    }

    @Test
    @DisplayName("Fluent API - asUser() sets authenticated user context")
    void fluentApi_asUserSetsContext(DynamicJaxrsTest brxm) {
        // asUser() sets remote user, principal, and roles
        brxm.request()
            .get("/site/api/hello/authenticated")
            .asUser("john", "admin", "editor")
            .assertBody("Hello, World! authenticated");

        // Verify the user context was set
        assertEquals("john", brxm.getHstRequest().getRemoteUser());
        assertTrue(brxm.getHstRequest().isUserInRole("admin"));
        assertTrue(brxm.getHstRequest().isUserInRole("editor"));
    }

    @Test
    @DisplayName("Fluent API - withHeader() and queryParam() chain nicely")
    void fluentApi_chainedMethods(DynamicJaxrsTest brxm) {
        String response = brxm.request()
            .get("/site/api/hello/chained")
            .withHeader("X-Custom-Header", "custom-value")
            .queryParam("filter", "active")
            .execute();

        assertEquals("Hello, World! chained", response);
    }

    @Test
    @DisplayName("Fluent API - executeWithStatus() for response inspection")
    void fluentApi_executeWithStatus(DynamicJaxrsTest brxm) {
        Response<String> response = brxm.request()
            .get("/site/api/hello/status-check")
            .executeWithStatus();

        assertEquals(200, response.status());
        assertTrue(response.isSuccessful());
        assertEquals("Hello, World! status-check", response.body());
    }

    @Test
    @DisplayName("Repository is properly initialized")
    void zeroConfig_repositoryInitialized(DynamicJaxrsTest brxm) {
        try (var repo = brxm.repository()) {
            repo.assertNodeExists("/hst:myproject")
                .assertNodeExists("/hippo:configuration");
        }
    }
}
