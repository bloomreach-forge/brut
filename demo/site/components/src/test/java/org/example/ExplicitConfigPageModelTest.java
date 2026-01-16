package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.annotation.BrxmPageModelTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicPageModelTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Explicit configuration PageModel test example.
 * Demonstrates using @BrxmPageModelTest with explicit configuration parameters.
 * Useful when conventions don't match your project structure.
 */
@BrxmPageModelTest(
        beanPackages = {"org.example.beans"},
        hstRoot = "/hst:myproject",
        springConfig = "/org/example/custom-pagemodel.xml"
)
public class ExplicitConfigPageModelTest {

    private DynamicPageModelTest brxm;

    @Test
    @DisplayName("PageModel API with explicit configuration")
    void testPageModelExplicitConfig() throws IOException {
        brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
        brxm.getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");

        String response = brxm.invokeFilter();

        assertNotNull(response, "Response should not be null");
        assertTrue(response.contains("page"), "Response should contain page model data");

        JsonNode json = new ObjectMapper().readTree(response);
        assertNotNull(json.get("page"), "Response should have 'page' node");
    }

    @Test
    @DisplayName("Custom configuration is applied")
    void testCustomConfigApplied() {
        // Verify HST request is set up correctly
        assertNotNull(brxm.getHstRequest());
        assertTrue(brxm.getHstRequest().getHeader("Host").contains("localhost"));
    }
}
