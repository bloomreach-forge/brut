package org.example;

import org.bloomreach.forge.brut.resources.AbstractPageModelTest;
import org.bloomreach.forge.brut.resources.pagemodel.PageComponent;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A user (client) of the testing library providing his/her own config/content.
 *
 * Note: When extending AbstractPageModelTest directly, you must:
 * 1. Call setupForNewRequest() in @BeforeEach to reset HST state between tests
 * 2. Use dynamic component ID lookup via root.$ref (IDs like uid0 are not stable)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PageModelTest extends AbstractPageModelTest {

    @BeforeAll
    public void init() {
        super.init();
    }

    @AfterAll
    public void destroy() {
        super.destroy();
    }

    @BeforeEach
    public void setupRequest() {
        setupForNewRequest();
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/example/beans/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Collections.singletonList("/org/example/custom-pagemodel.xml");
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return null;
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:myproject";
    }

    @Test
    @DisplayName("Component rendering url response")
    public void test() throws Exception {
        getHstRequest().setRequestURI("/site/resourceapi/news");
        getHstRequest().setQueryString("_hn:type=component-rendering&_hn:ref=r5_r1_r1");
        String response = invokeFilter();

        PageModelResponse pageModel = PageModelResponse.parse(response);
        assertTrue(pageModel.getPage().size() > 0);

        // Use getRootComponent() for dynamic component lookup (IDs are not stable across test runs)
        PageComponent rootComponent = pageModel.getRootComponent();
        assertNotNull(rootComponent);
        assertNotNull(rootComponent.getId());
        assertNotNull(rootComponent.getType());
    }
}
