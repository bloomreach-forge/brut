package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.annotation.BrxmPageModelTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicPageModelTest;
import org.bloomreach.forge.brut.resources.pagemodel.PageComponent;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;
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

        // Use root.$ref to find the main component (IDs are dynamic)
        String rootRef = json.get("root").get("$ref").asText(); // e.g., "/page/uid0"
        String componentKey = rootRef.replace("/page/", "");
        JsonNode component = json.get("page").get(componentKey);
        assertNotNull(component, "Should have root component from " + rootRef);
        assertEquals(8, component.size(), "Component should have 8 fields");
        assertTrue(component.has("id"));
        assertTrue(component.has("type"));
        assertTrue(component.has("meta"));
    }

    @Test
    @DisplayName("PageModel API - executeAsPageModel() approach (recommended)")
    void testPageModelWithExecuteAsPageModel() throws Exception {
        // Approach 2: Use executeAsPageModel() for type-safe PageModelResponse
        PageModelResponse pageModel = brxm.request()
                .get("/site/resourceapi/news")
                .pageModelComponentRendering("r5_r1_r1")
                .executeAsPageModel();

        assertNotNull(pageModel.getPage(), "PageModel should have page components");
        assertFalse(pageModel.getPage().isEmpty(), "Page should contain components");

        // Use getRootComponent() for cleaner access
        PageComponent rootComponent = pageModel.getRootComponent();
        assertNotNull(rootComponent, "Root component should exist");
        assertNotNull(rootComponent.getId());
        assertNotNull(rootComponent.getType());
        assertNotNull(rootComponent.getMeta());

        // Benefits of PageModelResponse:
        // - Compile-time type checking
        // - IDE autocomplete works
        // - Navigation APIs (findComponentByName, getChildComponents, etc.)
        // - Document conversion via as(Class<T>)
    }

    @Test
    @DisplayName("PageModel API - Component navigation")
    void testComponentNavigation() throws Exception {
        PageModelResponse pageModel = brxm.request()
                .get("/site/resourceapi/news")
                .pageModelComponentRendering("r5_r1_r1")
                .executeAsPageModel();

        // Navigate component tree
        PageComponent root = pageModel.getRootComponent();
        assertNotNull(root);
        assertTrue(pageModel.getComponentCount() > 0);
    }

    @Test
    @DisplayName("Multiple test methods work correctly")
    void testMultipleRequests() throws Exception {
        // First request - using fluent API
        PageModelResponse response1 = brxm.request()
                .get("/site/resourceapi/news")
                .executeAsPageModel();
        assertNotNull(response1);

        // Second request (verifies beforeEach setup works)
        PageModelResponse response2 = brxm.request()
                .get("/site/resourceapi/news")
                .executeAsPageModel();
        assertNotNull(response2);
    }
}
