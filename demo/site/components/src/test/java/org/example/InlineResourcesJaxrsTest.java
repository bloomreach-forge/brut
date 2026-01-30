package org.example;

import org.bloomreach.forge.brut.resources.annotation.BrxmJaxrsTest;
import org.bloomreach.forge.brut.resources.annotation.DynamicJaxrsTest;
import org.example.rest.HelloResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates the inline resources parameter which eliminates Spring XML for JAX-RS resources.
 *
 * <p>Compare to {@link AnnotationBasedJaxrsTest} which requires:</p>
 * <pre>
 * springConfigs = {"/org/example/annotation-jaxrs.xml", "/org/example/rest-resources.xml"}
 * </pre>
 *
 * <p>This test only needs:</p>
 * <pre>
 * resources = {HelloResource.class}
 * </pre>
 */
@BrxmJaxrsTest(
    beanPackages = {"org.example.model"},
    springConfigs = {"/org/example/annotation-jaxrs.xml"},
    resources = {HelloResource.class}
)
public class InlineResourcesJaxrsTest {

    private DynamicJaxrsTest brxm;

    @Test
    @DisplayName("resources parameter registers JAX-RS endpoint without rest-resources.xml")
    void resourcesParameter_registersEndpoint() {
        String response = brxm.request()
            .get("/site/api/hello/world")
            .execute();

        assertEquals("Hello, World! world", response);
    }

    @Test
    @DisplayName("Multiple requests work correctly")
    void multipleRequests_work() {
        brxm.request()
            .get("/site/api/hello/alice")
            .assertBody("Hello, World! alice");

        brxm.request()
            .get("/site/api/hello/bob")
            .assertBody("Hello, World! bob");
    }
}
