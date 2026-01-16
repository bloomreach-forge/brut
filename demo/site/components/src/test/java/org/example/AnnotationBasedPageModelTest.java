package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.annotation.BrxmPageModelTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicPageModelTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Annotation-based PageModel test example.
 * Demonstrates using @BrxmPageModelTest with minimal configuration.
 * Only beanPackages and springConfig need to be specified - HST root is auto-detected.
 */
@BrxmPageModelTest(
        beanPackages = {"org.example.beans"},
        springConfig = "/org/example/annotation-pagemodel.xml"
)
public class AnnotationBasedPageModelTest {

    private DynamicPageModelTest brxm;

    @Test
    @DisplayName("PageModel API with zero configuration")
    void testPageModelZeroConfig() throws IOException {
        brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
        brxm.getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");

        String response = brxm.invokeFilter();

        assertNotNull(response, "Response should not be null");
        assertTrue(response.contains("page"), "Response should contain page model data");

        JsonNode json = new ObjectMapper().readTree(response);
        assertNotNull(json.get("page"), "Response should have 'page' node");
        assertTrue(json.get("page").size() > 0, "Page node should contain components");

        // Verify component structure is properly loaded
        JsonNode pageNode = json.get("page");
        JsonNode component = pageNode.get("uid0");
        assertNotNull(component, "Should have uid0 component");
        assertEquals(8, component.size(), "Component should have 8 fields");

        // Verify key component fields
        assertNotNull(component.get("id"), "Component should have id");
        assertNotNull(component.get("type"), "Component should have type");
        assertNotNull(component.get("meta"), "Component should have meta");
    }

    @Test
    @DisplayName("Multiple test methods work correctly")
    void testMultipleRequests() throws IOException {
        // First request
        brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
        String response1 = brxm.invokeFilter();
        assertNotNull(response1);

        // Second request (verifies beforeEach setup works)
        brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
        String response2 = brxm.invokeFilter();
        assertNotNull(response2);
    }
}
