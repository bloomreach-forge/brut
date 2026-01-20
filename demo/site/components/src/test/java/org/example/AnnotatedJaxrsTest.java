package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.AbstractJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.example.model.ListItemPagination;
import org.example.model.NewsItemRep;
import org.junit.jupiter.api.*;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Example of annotation-based JAX-RS testing with @BrxmJaxrsTest.
 * Demonstrates reduced boilerplate while maintaining full control over setup.
 *
 * Note: Still requires manual init/destroy/beforeEach for now. Future versions
 * may support fully automatic setup with @ExtendWith pattern.
 *
 * @since 5.1.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@BrxmJaxrsTest(
    hstConfigPath = "/hst:myproject",
    beanClasses = "classpath*:org/example/model/*.class,",
    springConfigPaths = "/org/example/custom-jaxrs.xml,/org/example/rest-resources.xml"
)
public class AnnotatedJaxrsTest extends AbstractJaxrsTest {

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
        return Arrays.asList("/org/example/custom-jaxrs.xml", "/org/example/rest-resources.xml");
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    @AfterAll
    public void destroy() {
        super.destroy();
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return null;
    }

    @Test
    @DisplayName("Test REST endpoint with annotation-based setup")
    void testEndpointWithAnnotation() {
        String user = "testuser";
        getHstRequest().setRequestURI("/site/api/hello/" + user);
        String response = invokeFilter();
        assertEquals("Hello, World! " + user, response);
    }

    @Test
    @DisplayName("Test HTTP session in request")
    void testSessionHandling() {
        getHstRequest().setRequestURI("/site/api/hello/sessiontest");

        // Store user in session
        getHstRequest().getSession().setAttribute("userId", "user123");
        getHstRequest().getSession().setAttribute("userName", "Test User");

        String response = invokeFilter();
        assertEquals("Hello, World! sessiontest", response);

        // Verify session attributes persisted
        assertEquals("user123", getHstRequest().getSession().getAttribute("userId"));
        assertEquals("Test User", getHstRequest().getSession().getAttribute("userName"));
    }

    @Test
    @DisplayName("Test news endpoint")
    void testNewsEndpoint() throws Exception {
        getHstRequest().setRequestURI("/site/api/news");
        String response = invokeFilter();
        ListItemPagination<NewsItemRep> pageable = new ObjectMapper()
                .readValue(response, new TypeReference<ListItemPagination<NewsItemRep>>() {});
        assertEquals(3, pageable.getItems().size(), "Should have 3 news items");
    }

}
