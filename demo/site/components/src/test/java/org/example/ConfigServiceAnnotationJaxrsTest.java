package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.jcr.Repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Annotation-based JAX-RS test using ConfigServiceRepository.
 * Demonstrates fluent repository utilities for HST structure verification.
 */
@BrxmJaxrsTest(
        beanPackages = {"org.example.model"},
        springConfigs = {"/org/example/annotation-jaxrs.xml", "/org/example/rest-resources.xml"},
        loadProjectContent = true
)
public class ConfigServiceAnnotationJaxrsTest {

    private DynamicJaxrsTest brxm;

    @Test
    @DisplayName("ConfigServiceRepository is active")
    void testConfigServiceRepositoryActive() {
        Repository repository = brxm.getComponentManager().getComponent(Repository.class);
        assertNotNull(repository);
        assertTrue(repository.getClass().getName().contains("ConfigServiceRepository"));

        // Using fluent RepositorySession (reduces 18 lines to 1 fluent chain)
        try (var repo = brxm.repository()) {
            // Verify HST configuration structure
            repo.assertNodeExists("/hst:myproject")
                .assertNodeExists("/hst:myproject/hst:configurations")
                .assertNodeExists("/hst:myproject/hst:hosts")
                .assertNodeExists("/hst:myproject/hst:sites")
                // Verify project configuration
                .assertNodeExists("/hst:myproject/hst:configurations/myproject")
                .assertNodeExists("/hst:myproject/hst:configurations/myproject/hst:sitemap")
                // Verify core repository structure
                .assertNodeExists("/hippo:configuration");
        }
    }

    @Test
    @DisplayName("JAX-RS endpoint works with ConfigService setup")
    void testJaxrsEndpoint() {
        // Using fluent RequestBuilder (reduces 4 lines to 1 fluent chain)
        String user = "config-service-user";
        brxm.request()
                .get("/site/api/hello/" + user)
                .assertBody("Hello, World! " + user);
    }
}
