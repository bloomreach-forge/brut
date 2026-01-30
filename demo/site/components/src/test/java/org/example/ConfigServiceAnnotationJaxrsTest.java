package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.bloomreach.forge.brut.resources.util.Response;
import org.example.model.ListItemPagination;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.jcr.Repository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Annotation-based JAX-RS test using ConfigServiceRepository.
 * Demonstrates loadProjectContent=true for production-parity HST configuration.
 */
@BrxmJaxrsTest(
        beanPackages = {"org.example.model"},
        springConfigs = {"/org/example/annotation-jaxrs.xml", "/org/example/rest-resources.xml"},
        loadProjectContent = true
)
public class ConfigServiceAnnotationJaxrsTest {

    @Test
    @DisplayName("loadProjectContent=true activates ConfigServiceRepository")
    void loadProjectContent_activatesConfigServiceRepository(DynamicJaxrsTest brxm) {
        Repository repository = brxm.getComponentManager().getComponent(Repository.class);
        assertNotNull(repository);
        assertTrue(repository.getClass().getName().contains("ConfigServiceRepository"));
    }

    @Test
    @DisplayName("Fluent RepositorySession for HST structure verification")
    void fluentRepository_verifyHstStructure(DynamicJaxrsTest brxm) {
        // brxm.repository() provides auto-closing session with fluent assertions
        try (var repo = brxm.repository()) {
            repo.assertNodeExists("/hst:myproject")
                .assertNodeExists("/hst:myproject/hst:configurations")
                .assertNodeExists("/hst:myproject/hst:hosts")
                .assertNodeExists("/hst:myproject/hst:sites")
                .assertNodeExists("/hst:myproject/hst:configurations/myproject")
                .assertNodeExists("/hst:myproject/hst:configurations/myproject/hst:sitemap")
                .assertNodeExists("/hippo:configuration");
        }
    }

    @Test
    @DisplayName("JAX-RS endpoint works with production HST config")
    void jaxrsEndpoint_worksWithProductionConfig(DynamicJaxrsTest brxm) {
        brxm.request()
            .get("/site/api/hello/config-service-user")
            .assertBody("Hello, World! config-service-user");
    }

    @Test
    @DisplayName("Complex endpoint with executeWithStatus()")
    void complexEndpoint_executeWithStatus(DynamicJaxrsTest brxm) throws JsonProcessingException {
        Response<ListItemPagination> response = brxm.request()
            .get("/site/api/news")
            .executeWithStatus(ListItemPagination.class);

        assertEquals(200, response.status());
        assertTrue(response.isSuccessful());
        assertFalse(response.isClientError());
        assertEquals(3, response.body().getItems().size());
    }
}
