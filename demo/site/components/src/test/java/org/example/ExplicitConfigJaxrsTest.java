package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.example.model.ListItemPagination;
import org.example.model.NewsItemRep;
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

    private DynamicJaxrsTest brxm;

    @Test
    @DisplayName("JAX-RS endpoint with explicit configuration")
    void testJaxrsExplicitConfig() {
        String user = "explicit-user";
        brxm.getHstRequest().setRequestURI("/site/api/hello/" + user);

        String response = brxm.invokeFilter();

        assertEquals("Hello, World! " + user, response);
    }

    @Test
    @DisplayName("Complex JAX-RS endpoint returns JSON")
    void testComplexEndpoint() throws Exception {
        brxm.getHstRequest().setRequestURI("/site/api/news");

        String response = brxm.invokeFilter();

        assertNotNull(response, "Response should not be null");

        // Parse JSON response
        ObjectMapper mapper = new ObjectMapper();
        ListItemPagination<NewsItemRep> result =
                mapper.readValue(response, new TypeReference<ListItemPagination<NewsItemRep>>() {});

        assertNotNull(result);
    }

    @Test
    @DisplayName("Custom configuration is applied")
    void testCustomConfigApplied() {
        // Verify HST request is set up correctly with explicit config
        assertNotNull(brxm.getHstRequest());
        assertTrue(brxm.getHstRequest().getHeader("Host").contains("localhost"));
    }
}
