package org.bloomreach.forge.brut.resources.diagnostics;

import org.bloomreach.forge.brut.resources.pagemodel.PageComponent;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PageModelDiagnosticsTest {

    @Test
    void testDiagnosePageNotFound_DetectsPageNotFoundScenario() {
        PageModelResponse pageModel = createPageModelWithPageNotFound();

        DiagnosticResult result = PageModelDiagnostics.diagnosePageNotFound(
            "homepage",
            "/",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("Expected page 'homepage' but got 'pagenotfound'"));
        assertFalse(result.recommendations().isEmpty());
    }

    @Test
    void testDiagnosePageNotFound_NoIssueWhenPageMatches() {
        PageModelResponse pageModel = createPageModelWithPage("homepage");

        DiagnosticResult result = PageModelDiagnostics.diagnosePageNotFound(
            "homepage",
            "/",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.SUCCESS, result.severity());
        assertTrue(result.message().contains("Page 'homepage' loaded successfully"));
    }

    @Test
    void testDiagnosePageNotFound_HandlesNullPageModel() {
        DiagnosticResult result = PageModelDiagnostics.diagnosePageNotFound(
            "homepage",
            "/",
            null
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("PageModel is null"));
    }

    private PageModelResponse createPageModelWithPageNotFound() {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent root = new PageComponent();
        root.setId("root");
        root.setName("pagenotfound");
        root.setType("container");

        page.put("root", root);
        pageModel.setPage(page);

        org.bloomreach.forge.brut.resources.pagemodel.ContentRef rootRef =
            new org.bloomreach.forge.brut.resources.pagemodel.ContentRef();
        rootRef.setRef("/page/root");
        pageModel.setRoot(rootRef);

        return pageModel;
    }

    private PageModelResponse createPageModelWithPage(String pageName) {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent root = new PageComponent();
        root.setId("root");
        root.setName(pageName);
        root.setType("container");

        page.put("root", root);
        pageModel.setPage(page);

        org.bloomreach.forge.brut.resources.pagemodel.ContentRef rootRef =
            new org.bloomreach.forge.brut.resources.pagemodel.ContentRef();
        rootRef.setRef("/page/root");
        pageModel.setRoot(rootRef);

        return pageModel;
    }

    @Test
    void testDiagnoseComponentNotFound_ComponentMissing() {
        PageModelResponse pageModel = createPageModelWithComponents("header", "footer");

        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            "banner",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("Component 'banner' not found"));
        assertTrue(result.recommendations().stream()
            .anyMatch(rec -> rec.contains("Available components")));
    }

    @Test
    void testDiagnoseComponentNotFound_EmptyPageModel() {
        PageModelResponse pageModel = new PageModelResponse();
        pageModel.setPage(new HashMap<>());

        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            "banner",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("No components found"));
    }

    @Test
    void testDiagnoseComponentNotFound_ComponentExists() {
        PageModelResponse pageModel = createPageModelWithComponents("banner", "header");

        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            "banner",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.SUCCESS, result.severity());
        assertTrue(result.message().contains("Component 'banner' found"));
    }

    @Test
    void testDiagnoseEmptyContainer_ContainerHasNoChildren() {
        PageModelResponse pageModel = createPageModelWithEmptyContainer("main");

        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyContainer(
            "main",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.WARNING, result.severity());
        assertTrue(result.message().contains("Container 'main' is empty"));
        assertTrue(result.recommendations().stream()
            .anyMatch(rec -> rec.contains("containercomponentreference")));
    }

    @Test
    void testDiagnoseEmptyContainer_ContainerNotFound() {
        PageModelResponse pageModel = createPageModelWithComponents("header");

        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyContainer(
            "main",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("Container 'main' not found"));
    }

    @Test
    void diagnoseEmptyResponse_withRequestUri_messageContainsUri() {
        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyResponse("/site/fr/resourceapi");

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("/site/fr/resourceapi"));
    }

    @Test
    void diagnoseEmptyResponse_withRequestUri_recommendationsMentionComponentClass() {
        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyResponse("/site/fr/resourceapi");

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.recommendations().stream()
            .anyMatch(recommendation -> recommendation.toLowerCase().contains("component class")));
    }

    @Test
    void diagnoseEmptyResponse_withNullUri_stillReturnsError() {
        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyResponse(null);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertFalse(result.recommendations().isEmpty());
    }

    @Test
    void diagnoseComponentNotFound_emptyPageMap_withRequestUri_messageContainsRequestUri() {
        PageModelResponse pageModel = new PageModelResponse();
        pageModel.setPage(new HashMap<>());

        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            "banner", "/site/fpp/resourceapi", pageModel
        );

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("/site/fpp/resourceapi"));
    }

    @Test
    void diagnoseComponentNotFound_emptyPageMap_withRequestUri_recommendationsMentionSitemap() {
        PageModelResponse pageModel = new PageModelResponse();
        pageModel.setPage(new HashMap<>());

        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            "banner", "/site/fpp/resourceapi", pageModel
        );

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.recommendations().stream()
            .anyMatch(recommendation -> recommendation.contains("sitemap")));
    }

    @Test
    void diagnoseComponentNotFound_emptyPageMap_withRequestUri_stripsResourceApiSuffix() {
        PageModelResponse pageModel = new PageModelResponse();
        pageModel.setPage(new HashMap<>());

        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            "banner", "/site/fpp/resourceapi", pageModel
        );

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.recommendations().stream()
            .anyMatch(recommendation -> recommendation.contains("/site/fpp") && !recommendation.contains("/resourceapi")));
    }

    @Test
    void testDiagnoseEmptyContainer_ContainerHasChildren() {
        PageModelResponse pageModel = createPageModelWithContainer("main", "banner", "footer");

        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyContainer(
            "main",
            pageModel
        );

        assertNotNull(result);
        assertEquals(DiagnosticSeverity.SUCCESS, result.severity());
        assertTrue(result.message().contains("Container 'main' has 2 child"));
    }

    private PageModelResponse createPageModelWithComponents(String... componentNames) {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        for (String name : componentNames) {
            PageComponent component = new PageComponent();
            component.setId(name + "_id");
            component.setName(name);
            component.setType("component");
            page.put(component.getId(), component);
        }

        pageModel.setPage(page);
        return pageModel;
    }

    private PageModelResponse createPageModelWithEmptyContainer(String containerName) {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent container = new PageComponent();
        container.setId(containerName + "_id");
        container.setName(containerName);
        container.setType("container");
        container.setChildren(List.of());

        page.put(container.getId(), container);
        pageModel.setPage(page);

        return pageModel;
    }

    private PageModelResponse createPageModelWithContainer(String containerName, String... childNames) {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        List<org.bloomreach.forge.brut.resources.pagemodel.ContentRef> childRefs = new ArrayList<>();
        for (String childName : childNames) {
            PageComponent child = new PageComponent();
            child.setId(childName + "_id");
            child.setName(childName);
            child.setType("component");
            page.put(child.getId(), child);

            org.bloomreach.forge.brut.resources.pagemodel.ContentRef ref =
                new org.bloomreach.forge.brut.resources.pagemodel.ContentRef();
            ref.setRef("/page/" + child.getId());
            childRefs.add(ref);
        }

        PageComponent container = new PageComponent();
        container.setId(containerName + "_id");
        container.setName(containerName);
        container.setType("container");
        container.setChildren(childRefs);

        page.put(container.getId(), container);
        pageModel.setPage(page);

        return pageModel;
    }
}
