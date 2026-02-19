package org.bloomreach.forge.brut.resources.diagnostics;

import org.bloomreach.forge.brut.resources.pagemodel.ContentRef;
import org.bloomreach.forge.brut.resources.pagemodel.PageComponent;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration example demonstrating real-world usage of BRUT diagnostics.
 * Shows how developers would use the enhanced diagnostic capabilities in their tests.
 */
class DiagnosticsIntegrationExampleTest {

    @Test
    @DisplayName("Example: Page loads successfully with diagnostic validation")
    void exampleSuccessfulPageLoad() {
        PageModelResponse pageModel = createValidPageModel();

        PageModelAssert.assertThat(pageModel, "/", "landing")
            .hasPage("homepage")
            .hasComponent("banner")
            .hasComponent("header")
            .containerNotEmpty("main")
            .containerHasMinChildren("main", 2);
    }

    @Test
    @DisplayName("Example: Diagnostic output when page is not found")
    void examplePageNotFoundDiagnostics() {
        PageModelResponse pageModel = createPageNotFoundModel();

        DiagnosticResult result = PageModelDiagnostics.diagnosePageNotFound(
            "homepage",
            "/",
            pageModel
        );

        System.out.println("\n=== PAGE NOT FOUND DIAGNOSTIC ===");
        System.out.println(result);
        System.out.println("=================================\n");
    }

    @Test
    @DisplayName("Example: Diagnostic output when component is missing")
    void exampleComponentMissingDiagnostics() {
        PageModelResponse pageModel = createPageModelWithSomeComponents();

        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            "banner",
            pageModel
        );

        System.out.println("\n=== COMPONENT MISSING DIAGNOSTIC ===");
        System.out.println(result);
        System.out.println("====================================\n");
    }

    @Test
    @DisplayName("Example: Diagnostic output when container is empty")
    void exampleEmptyContainerDiagnostics() {
        PageModelResponse pageModel = createPageModelWithEmptyContainer();

        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyContainer(
            "main",
            pageModel
        );

        System.out.println("\n=== EMPTY CONTAINER DIAGNOSTIC ===");
        System.out.println(result);
        System.out.println("==================================\n");
    }

    @Test
    @DisplayName("Example: Standard PageModel API usage (no diagnostics)")
    void exampleStandardPageModelUsage() {
        PageModelResponse pageModel = createValidPageModel();

        PageComponent banner = pageModel.findComponentByName("banner")
            .orElseThrow(() -> new AssertionError("Banner not found"));

        System.out.println("Component name: " + banner.getName());
        System.out.println("Component type: " + banner.getType());
    }

    @Test
    @DisplayName("Example: Enhanced assertion usage with diagnostics")
    void exampleEnhancedAssertionUsage() {
        PageModelResponse pageModel = createValidPageModel();

        PageComponent banner = PageModelAssert.assertThat(pageModel)
            .hasComponent("banner")
            .getComponent("banner");

        System.out.println("Retrieved component: " + banner.getName());
    }

    private PageModelResponse createValidPageModel() {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent root = new PageComponent();
        root.setId("root");
        root.setName("homepage");
        root.setType("container");

        PageComponent banner = new PageComponent();
        banner.setId("banner_id");
        banner.setName("banner");
        banner.setType("component");
        banner.setComponentClass("com.example.BannerComponent");

        PageComponent header = new PageComponent();
        header.setId("header_id");
        header.setName("header");
        header.setType("component");
        header.setComponentClass("com.example.HeaderComponent");

        PageComponent main = new PageComponent();
        main.setId("main_id");
        main.setName("main");
        main.setType("container");

        List<ContentRef> mainChildren = new ArrayList<>();
        ContentRef bannerRef = new ContentRef();
        bannerRef.setRef("/page/banner_id");
        mainChildren.add(bannerRef);

        ContentRef headerRef = new ContentRef();
        headerRef.setRef("/page/header_id");
        mainChildren.add(headerRef);

        main.setChildren(mainChildren);

        page.put("root", root);
        page.put("banner_id", banner);
        page.put("header_id", header);
        page.put("main_id", main);

        pageModel.setPage(page);

        ContentRef rootRef = new ContentRef();
        rootRef.setRef("/page/root");
        pageModel.setRoot(rootRef);

        return pageModel;
    }

    private PageModelResponse createPageNotFoundModel() {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent root = new PageComponent();
        root.setId("root");
        root.setName("pagenotfound");
        root.setType("container");

        page.put("root", root);
        pageModel.setPage(page);

        ContentRef rootRef = new ContentRef();
        rootRef.setRef("/page/root");
        pageModel.setRoot(rootRef);

        return pageModel;
    }

    private PageModelResponse createPageModelWithSomeComponents() {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent header = new PageComponent();
        header.setId("header_id");
        header.setName("header");
        header.setType("component");

        PageComponent footer = new PageComponent();
        footer.setId("footer_id");
        footer.setName("footer");
        footer.setType("component");

        page.put("header_id", header);
        page.put("footer_id", footer);

        pageModel.setPage(page);

        return pageModel;
    }

    private PageModelResponse createPageModelWithEmptyContainer() {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent main = new PageComponent();
        main.setId("main_id");
        main.setName("main");
        main.setType("container");
        main.setChildren(List.of());

        page.put("main_id", main);
        pageModel.setPage(page);

        return pageModel;
    }
}
