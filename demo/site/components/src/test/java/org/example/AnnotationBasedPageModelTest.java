package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.annotation.BrxmPageModelTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicPageModelTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Annotation-based PageModel test example.
 * Demonstrates using @BrxmPageModelTest with minimal configuration and JSON assertion approaches.
 */
@BrxmPageModelTest(
        beanPackages = {"org.example.beans"},
        springConfig = "/org/example/annotation-pagemodel.xml"
)
public class AnnotationBasedPageModelTest {

    private DynamicPageModelTest brxm;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("PageModel API - Jackson JsonNode approach")
    void testPageModelWithJackson() throws Exception {
        // Approach 1: Direct Jackson JsonNode (flexible, low-level)
        String response = brxm.request()
                .get("/site/resourceapi/news")
                .pageModelComponentRendering("r5_r1_r1")
                .execute();

        JsonNode json = mapper.readTree(response);
        assertTrue(json.has("page"), "Response should have 'page' node");
        assertTrue(json.get("page").size() > 0, "Page should contain components");

        JsonNode component = json.get("page").get("uid0");
        assertNotNull(component, "Should have uid0 component");
        assertEquals(8, component.size(), "Component should have 8 fields");
        assertTrue(component.has("id"));
        assertTrue(component.has("type"));
        assertTrue(component.has("meta"));
    }

    @Test
    @DisplayName("PageModel API - POJO deserialization approach (recommended)")
    void testPageModelWithPojo() throws Exception {
        // Approach 2: Type-safe POJO deserialization (recommended for production)
        // This gives compile-time safety and better IDE support
        String response = brxm.request()
                .get("/site/resourceapi/news")
                .pageModelComponentRendering("r5_r1_r1")
                .execute();

        // Deserialize to POJO for type-safe assertions
        PageModelResponse pageModel = mapper.readValue(response, PageModelResponse.class);
        assertNotNull(pageModel.getPage(), "PageModel should have page components");
        assertFalse(pageModel.getPage().isEmpty(), "Page should contain components");

        // Access components in type-safe way
        PageModelResponse.ComponentModel component = pageModel.getPage().get("uid0");
        assertNotNull(component, "Component uid0 should exist");
        assertNotNull(component.getId());
        assertNotNull(component.getType());
        assertNotNull(component.getMeta());

        // Benefits of POJO approach:
        // - Compile-time type checking
        // - IDE autocomplete works
        // - Clear documentation of expected structure
        // - Easy refactoring

        // Other useful libraries for JSON assertions:
        // - AssertJ JSON: assertThatJson(response).node("page.uid0.id").isPresent()
        // - JSONPath: JsonPath.parse(response).read("$.page.uid0.id")
        // - Hamcrest: assertThat(json, hasJsonPath("$.page.uid0.id"))
    }

    @Test
    @DisplayName("Multiple test methods work correctly")
    void testMultipleRequests() {
        // First request - using fluent API
        String response1 = brxm.request()
                .get("/site/resourceapi/news")
                .execute();
        assertNotNull(response1);

        // Second request (verifies beforeEach setup works)
        String response2 = brxm.request()
                .get("/site/resourceapi/news")
                .execute();
        assertNotNull(response2);
    }
}
