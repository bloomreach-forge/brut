package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.bloomreach.forge.brut.resources.util.Response;
import org.example.model.ListItemPagination;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Explicit configuration JAX-RS test example.
 * Demonstrates using @BrxmJaxrsTest with explicit configuration parameters.
 * Useful when conventions don't match your project structure.
 */
@BrxmJaxrsTest(
        beanPackages = {"org.example.model"},
        hstRoot = "/hst:myproject",
        springConfigs = {"/org/example/custom-jaxrs.xml", "/org/example/rest-resources.xml"}
)
public class ExplicitConfigJaxrsTest {

    @Test
    @DisplayName("JAX-RS endpoint with explicit configuration")
    void testJaxrsExplicitConfig(DynamicJaxrsTest brxm) {
        // Fluent API - one line request execution with assertion
        brxm.request()
            .get("/site/api/hello/explicit-user")
            .assertBody("Hello, World! explicit-user");
    }

    @Test
    @DisplayName("Complex JAX-RS endpoint returns JSON with executeAs()")
    void testComplexEndpoint(DynamicJaxrsTest brxm) throws JsonProcessingException {
        // executeAs() provides type-safe JSON deserialization
        ListItemPagination<?> result = brxm.request()
            .get("/site/api/news")
            .executeAs(ListItemPagination.class);

        assertNotNull(result);
        assertEquals(3, result.getItems().size());
    }

    @Test
    @DisplayName("executeWithStatus() provides response status and body together")
    void testResponseStatus(DynamicJaxrsTest brxm) throws JsonProcessingException {
        // executeWithStatus() returns Response with status code access
        Response<ListItemPagination> response = brxm.request()
            .get("/site/api/news")
            .executeWithStatus(ListItemPagination.class);

        assertTrue(response.isSuccessful());
        assertEquals(200, response.status());
        assertNotNull(response.body());
        assertNotNull(response.rawBody()); // Original JSON string available
    }

    @Test
    @DisplayName("Custom configuration is applied")
    void testCustomConfigApplied(DynamicJaxrsTest brxm) {
        // Verify HST request is set up correctly with explicit config
        assertNotNull(brxm.getHstRequest());
        assertTrue(brxm.getHstRequest().getHeader("Host").contains("localhost"));
    }
}
