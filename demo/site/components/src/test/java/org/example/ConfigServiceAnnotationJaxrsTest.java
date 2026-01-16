package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Annotation-based JAX-RS test using ConfigServiceRepository.
 */
@BrxmJaxrsTest(
        beanPackages = {"org.example.model"},
        springConfigs = {"/org/example/annotation-jaxrs.xml", "/org/example/rest-resources.xml"},
        useConfigService = true
)
public class ConfigServiceAnnotationJaxrsTest {

    private DynamicJaxrsTest brxm;

    @Test
    @DisplayName("ConfigServiceRepository is active")
    void testConfigServiceRepositoryActive() throws Exception {
        Repository repository = brxm.getComponentManager().getComponent(Repository.class);
        assertNotNull(repository);
        assertTrue(repository.getClass().getName().contains("ConfigServiceRepository"));

        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            assertTrue(session.nodeExists("/hst:myproject"));
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Test
    @DisplayName("JAX-RS endpoint works with ConfigService setup")
    void testJaxrsEndpoint() {
        String user = "config-service-user";
        brxm.getHstRequest().setRequestURI("/site/api/hello/" + user);

        String response = brxm.invokeFilter();

        assertEquals("Hello, World! " + user, response);
    }
}
