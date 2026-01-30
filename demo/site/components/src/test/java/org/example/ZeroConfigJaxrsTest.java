package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.example.rest.HelloResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private DynamicJaxrsTest brxm;

    @Test
    @DisplayName("Zero config - endpoint works via auto-detected YAML")
    void zeroConfig_endpointWorks() {
        String response = brxm.request()
            .get("/site/api/hello/zero-config")
            .execute();

        assertEquals("Hello, World! zero-config", response);
    }

    @Test
    @DisplayName("Repository is properly initialized")
    void zeroConfig_repositoryInitialized() throws Exception {
        try (var repo = brxm.repository()) {
            repo.assertNodeExists("/hst:myproject")
                .assertNodeExists("/hippo:configuration");
        }
    }
}
