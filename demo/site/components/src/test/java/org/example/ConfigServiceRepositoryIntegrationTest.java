package org.example;

import org.bloomreach.forge.brut.resources.AbstractJaxrsTest;
import org.junit.jupiter.api.*;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating ConfigServiceRepository with Spring XML override.
 * <p>
 * This test validates that ConfigServiceRepository works correctly in a standard
 * project structure by:
 * <ul>
 *   <li>Overriding the repository bean via custom Spring XML</li>
 *   <li>Providing HCM module descriptor and config files</li>
 *   <li>Verifying HST structure is created by ConfigService</li>
 *   <li>Testing JAX-RS endpoints work with production-parity structure</li>
 * </ul>
 *
 * @see org.bloomreach.forge.brut.resources.ConfigServiceRepository
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigServiceRepositoryIntegrationTest extends AbstractJaxrsTest {

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

    /**
     * Override to include custom Spring XML that uses ConfigServiceRepository.
     * <p>
     * The config-service-jcr.xml overrides the javax.jcr.Repository bean to use
     * ConfigServiceRepository instead of SkeletonRepository.
     */
    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList(
            "/org/example/config-service-jcr.xml",  // Custom repository override
            "/org/example/custom-jaxrs.xml",
            "/org/example/rest-resources.xml"
        );
    }

    @AfterAll
    public void destroy() {
        super.destroy();
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
     * Verify HST structure was created by ConfigService (not manual construction).
     * This proves ConfigServiceRepository is working correctly.
     */
    @Test
    @DisplayName("HST structure created via ConfigService")
    public void testHstStructureCreatedByConfigService() throws Exception {
        Repository repository = getComponentManager().getComponent(Repository.class);
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        try {
            // Verify HST root structure (BRUT uses /hst:myproject as root, not /hst:hst)
            assertTrue(session.nodeExists("/hst:myproject"),
                "HST root should exist (created by ConfigService)");

            assertTrue(session.nodeExists("/hst:myproject/hst:sites"),
                "HST sites container should exist");

            assertTrue(session.nodeExists("/hst:myproject/hst:configurations"),
                "HST configurations container should exist");

            assertTrue(session.nodeExists("/hst:myproject/hst:hosts"),
                "HST virtual hosts should exist");

            // Verify project-specific structure from HCM config
            assertTrue(session.nodeExists("/hst:myproject/hst:sites/myproject"),
                "Project site should exist (from hcm-config/hst/demo-hst.yaml)");

            assertTrue(session.nodeExists("/hst:myproject/hst:configurations/myproject"),
                "Project configuration should exist (from HCM config)");

            assertTrue(session.nodeExists("/hst:myproject/hst:hosts/dev-localhost"),
                "Virtual host group should exist (from HCM config)");

            // Verify sitemap structure
            assertTrue(session.nodeExists("/hst:myproject/hst:configurations/myproject/hst:sitemap"),
                "Sitemap should exist");

            assertTrue(session.nodeExists("/hst:myproject/hst:configurations/myproject/hst:sitemap/root"),
                "Root sitemap item should exist");

            // Verify pages structure
            assertTrue(session.nodeExists("/hst:myproject/hst:configurations/myproject/hst:pages"),
                "Pages should exist");

            assertTrue(session.nodeExists("/hst:myproject/hst:configurations/myproject/hst:pages/homepage"),
                "Homepage component should exist");

        } finally {
            session.logout();
        }
    }

    /**
     * Verify JAX-RS endpoints work with ConfigService-created structure.
     * This ensures the production-parity structure is compatible with HST runtime.
     */
    @Test
    @DisplayName("JAX-RS endpoint works with ConfigService structure")
    public void testJaxrsEndpointWithConfigServiceRepository() {
        String user = "configservice-test";
        getHstRequest().setRequestURI("/site/api/hello/" + user);
        String response = invokeFilter();
        assertEquals("Hello, World! " + user, response,
            "JAX-RS endpoint should work with ConfigService-created HST structure");
    }

    /**
     * Verify ConfigServiceRepository is being used (not SkeletonRepository).
     */
    @Test
    @DisplayName("ConfigServiceRepository is active")
    public void testConfigServiceRepositoryIsActive() {
        Repository repository = getComponentManager().getComponent(Repository.class);
        assertNotNull(repository, "Repository should be injected");

        // ConfigServiceRepository is a subclass of BrxmTestingRepository
        String className = repository.getClass().getName();
        assertTrue(className.contains("ConfigServiceRepository"),
            "Repository should be ConfigServiceRepository, but was: " + className);
    }

    /**
     * Verify content structure is still loaded from YAML.
     * ConfigServiceRepository handles HST bootstrap, but content loading remains unchanged.
     */
    @Test
    @DisplayName("Content structure loaded from YAML")
    public void testContentStructureFromYaml() throws Exception {
        Repository repository = getComponentManager().getComponent(Repository.class);
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        try {
            assertTrue(session.nodeExists("/content"),
                "Content root should exist from YAML import");
        } finally {
            session.logout();
        }
    }
}
