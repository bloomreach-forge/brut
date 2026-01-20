package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.AbstractPageModelTest;
import org.bloomreach.forge.brut.resources.annotation.BrxmPageModelTest;
import org.junit.jupiter.api.*;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Example of annotation-based Page Model API testing with @BrxmPageModelTest.
 * Demonstrates Page Model-specific testing with component rendering assertions.
 *
 * @since 5.1.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BrxmPageModelTest(
    hstConfigPath = "/hst:myproject",
    beanClasses = "classpath*:org/example/model/*.class,",
    springConfigPaths = "/custom-pagemodel.xml"
)
public class AnnotatedPageModelTest extends AbstractPageModelTest {

    @BeforeAll
    public void init() {
        super.init();
    }

    @BeforeEach
    public void beforeEach() {
        setupForNewRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/example/model/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/custom-pagemodel.xml");
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    @AfterAll
    public void destroy() {
        super.destroy();
    }

    @Test
    @DisplayName("Test Page Model component rendering")
    void testComponentRendering() throws IOException {
        getHstRequest().setRequestURI("/site/resourceapi/news");
        getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");

        String response = invokeFilter();
        JsonNode json = new ObjectMapper().readTree(response);

        assertNotNull(json, "Response should be valid JSON");
        assertTrue(json.has("page"), "Response should contain 'page' field");
        assertTrue(json.get("page").size() > 0, "Page should have rendered components");
    }

    @Test
    @DisplayName("Test Page Model component list")
    void testPageModelComponentList() throws IOException {
        getHstRequest().setRequestURI("/site/resourceapi/news");
        getHstRequest().setQueryString("_hn:type=component-rendering");

        String response = invokeFilter();
        JsonNode json = new ObjectMapper().readTree(response);

        assertNotNull(json, "Response should be valid JSON");
        assertNotNull(json.get("page"), "Should have page object");
    }

    @Test
    @DisplayName("Test Page Model with session attribute")
    void testPageModelWithSessionAttribute() throws IOException {
        // Set session attribute
        getHstRequest().getSession().setAttribute("userRole", "admin");

        getHstRequest().setRequestURI("/site/resourceapi/news");
        getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");

        String response = invokeFilter();
        JsonNode json = new ObjectMapper().readTree(response);

        // Verify session persists
        assertEquals("admin", getHstRequest().getSession().getAttribute("userRole"));
        assertNotNull(json);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

}
