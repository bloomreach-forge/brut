package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.bloomreach.forge.brut.resources.util.Response;
import org.example.model.ListItemPagination;
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

    @Test
    @DisplayName("Fluent API: assertBody() for simple response validation")
    void testAssertBody(DynamicJaxrsTest brxm) {
        // assertBody() - concise validation in one chain
        brxm.request()
            .get("/site/api/hello/test-user")
            .assertBody("Hello, World! test-user");
    }

    @Test
    @DisplayName("Fluent API: execute() returns response as string")
    void testExecute(DynamicJaxrsTest brxm) {
        // execute() - when you need the raw response
        String response = brxm.request()
            .get("/site/api/hello/another-user")
            .execute();

        assertEquals("Hello, World! another-user", response);
    }

    @Test
    @DisplayName("Fluent API: executeAs() for type-safe JSON responses")
    void testExecuteAs(DynamicJaxrsTest brxm) throws JsonProcessingException {
        // executeAs() - JSON deserialization built-in
        ListItemPagination<?> result = brxm.request()
            .get("/site/api/news")
            .executeAs(ListItemPagination.class);

        assertEquals(3, result.getItems().size());
    }

    @Test
    @DisplayName("Fluent API: executeWithStatus() for status code access")
    void testExecuteWithStatus(DynamicJaxrsTest brxm) throws JsonProcessingException {
        // executeWithStatus() - when you need HTTP status + body
        Response<ListItemPagination> response = brxm.request()
            .get("/site/api/news")
            .executeWithStatus(ListItemPagination.class);

        assertEquals(200, response.status());
        assertTrue(response.isSuccessful());
        assertEquals(3, response.body().getItems().size());
    }

    @Test
    @DisplayName("Request infrastructure is properly set up")
    void testRequestSetup(DynamicJaxrsTest brxm) {
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
