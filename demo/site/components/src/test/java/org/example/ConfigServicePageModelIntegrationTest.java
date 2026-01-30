package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.AbstractPageModelTest;
import org.junit.jupiter.api.*;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating ConfigServiceRepository with PageModel API.
 * <p>
 * This test validates that ConfigServiceRepository works correctly with
 * AbstractPageModelTest by:
 * <ul>
 *   <li>Overriding the repository bean via custom Spring XML</li>
 *   <li>Using the same HCM module and config as JAX-RS test</li>
 *   <li>Verifying HST structure created by ConfigService</li>
 *   <li>Testing PageModel API endpoints work with production-parity structure</li>
 * </ul>
 *
 * @see org.bloomreach.forge.brut.resources.ConfigServiceRepository
 * @see ConfigServiceRepositoryIntegrationTest
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigServicePageModelIntegrationTest extends AbstractPageModelTest {

    @BeforeAll
    public void init() {
        super.init();
    }

    @AfterAll
    public void destroy() {
        super.destroy();
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/example/beans/*.class,";
    }

    /**
     * Override to include custom Spring XML that uses ConfigServiceRepository.
     */
    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList(
            "/org/example/config-service-jcr.xml",  // ConfigServiceRepository override
            "/org/example/custom-pagemodel.xml"
        );
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return null;
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    /**
     * Verify HST structure was created by ConfigService.
     */
    @Test
    @DisplayName("HST structure created via ConfigService for PageModel")
    public void testHstStructureCreatedByConfigService() throws Exception {
        Repository repository = getComponentManager().getComponent(Repository.class);
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        try {
            // Verify HST root structure
            assertTrue(session.nodeExists("/hst:myproject"),
                "HST root should exist (created by ConfigService)");

            assertTrue(session.nodeExists("/hst:myproject/hst:configurations"),
                "HST configurations should exist");

            assertTrue(session.nodeExists("/hst:myproject/hst:configurations/myproject"),
                "Project configuration should exist from HCM config");

        } finally {
            session.logout();
        }
    }

    /**
     * Verify PageModel API works with ConfigService-created structure.
     */
    @Test
    @DisplayName("PageModel API works with ConfigService structure")
    public void testPageModelWithConfigService() throws IOException {
        getHstRequest().setRequestURI("/site/resourceapi/news");
        getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");
        String response = invokeFilter();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readValue(response, JsonNode.class);

        assertNotNull(jsonNode.get("page"),
            "PageModel response should have 'page' node with ConfigService structure");
        assertTrue(jsonNode.get("page").size() > 0,
            "Page should have components");
    }

    /**
     * Verify ConfigServiceRepository is being used.
     */
    @Test
    @DisplayName("ConfigServiceRepository is active in PageModel test")
    public void testConfigServiceRepositoryIsActive() {
        Repository repository = getComponentManager().getComponent(Repository.class);
        assertNotNull(repository, "Repository should be injected");

        String className = repository.getClass().getName();
        assertTrue(className.contains("ConfigServiceRepository"),
            "Repository should be ConfigServiceRepository, but was: " + className);
    }
}
