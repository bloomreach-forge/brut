package org.bloomreach.forge.brut.resources.diagnostics;

import org.bloomreach.forge.brut.resources.pagemodel.PageComponent;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;
import org.bloomreach.forge.brut.resources.pagemodel.ContentRef;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PageModelAssertTest {

    @Test
    void testHasPage_Success() {
        PageModelResponse pageModel = createPageModel("homepage");

        assertDoesNotThrow(() ->
            PageModelAssert.assertThat(pageModel, "/", "landing")
                .hasPage("homepage")
        );
    }

    @Test
    void testHasPage_FailsWithDiagnostics() {
        PageModelResponse pageModel = createPageModel("pagenotfound");

        AssertionFailedError error = assertThrows(AssertionFailedError.class, () ->
            PageModelAssert.assertThat(pageModel, "/", "landing")
                .hasPage("homepage")
        );

        assertTrue(error.getMessage().contains("Expected page 'homepage' but got 'pagenotfound'"));
        assertTrue(error.getMessage().contains("RECOMMENDATIONS"));
    }

    @Test
    void testHasComponent_Success() {
        PageModelResponse pageModel = createPageModelWithComponents("banner", "header");

        assertDoesNotThrow(() ->
            PageModelAssert.assertThat(pageModel)
                .hasComponent("banner")
        );
    }

    @Test
    void testHasComponent_FailsWithDiagnostics() {
        PageModelResponse pageModel = createPageModelWithComponents("header", "footer");

        AssertionFailedError error = assertThrows(AssertionFailedError.class, () ->
            PageModelAssert.assertThat(pageModel)
                .hasComponent("banner")
        );

        assertTrue(error.getMessage().contains("Component 'banner' not found"));
        assertTrue(error.getMessage().contains("Available components"));
    }

    @Test
    void testGetComponent_ReturnsComponent() {
        PageModelResponse pageModel = createPageModelWithComponents("banner", "header");

        PageComponent component = PageModelAssert.assertThat(pageModel)
            .getComponent("banner");

        assertNotNull(component);
        assertEquals("banner", component.getName());
    }

    @Test
    void testContainerNotEmpty_Success() {
        PageModelResponse pageModel = createPageModelWithContainer("main", "banner", "footer");

        assertDoesNotThrow(() ->
            PageModelAssert.assertThat(pageModel)
                .containerNotEmpty("main")
        );
    }

    @Test
    void testContainerNotEmpty_FailsWhenEmpty() {
        PageModelResponse pageModel = createEmptyContainer("main");

        AssertionFailedError error = assertThrows(AssertionFailedError.class, () ->
            PageModelAssert.assertThat(pageModel)
                .containerNotEmpty("main")
        );

        assertTrue(error.getMessage().contains("Container 'main' is empty"));
        assertTrue(error.getMessage().contains("containercomponentreference"));
    }

    @Test
    void testContainerHasMinChildren_Success() {
        PageModelResponse pageModel = createPageModelWithContainer("main", "banner", "header", "footer");

        assertDoesNotThrow(() ->
            PageModelAssert.assertThat(pageModel)
                .containerHasMinChildren("main", 2)
        );
    }

    @Test
    void testContainerHasMinChildren_FailsWhenNotEnough() {
        PageModelResponse pageModel = createPageModelWithContainer("main", "banner");

        AssertionFailedError error = assertThrows(AssertionFailedError.class, () ->
            PageModelAssert.assertThat(pageModel)
                .containerHasMinChildren("main", 3)
        );

        assertTrue(error.getMessage().contains("has 1 children, expected at least 3"));
    }

    @Test
    void testChaining_MultipleAssertions() {
        PageModelResponse pageModel = createCompletePageModel();

        assertDoesNotThrow(() ->
            PageModelAssert.assertThat(pageModel, "/", "landing")
                .hasPage("homepage")
                .hasComponent("banner")
                .hasComponent("header")
                .containerNotEmpty("main")
                .containerHasMinChildren("main", 2)
        );
    }

    @Test
    void testComponentHasModel_Success() {
        PageModelResponse pageModel = createPageModelWithModel("banner", "document");

        assertDoesNotThrow(() ->
            PageModelAssert.assertThat(pageModel)
                .componentHasModel("banner", "document")
        );
    }

    @Test
    void testComponentHasModel_Fails() {
        PageModelResponse pageModel = createPageModelWithComponents("banner");

        AssertionFailedError error = assertThrows(AssertionFailedError.class, () ->
            PageModelAssert.assertThat(pageModel)
                .componentHasModel("banner", "document")
        );

        assertTrue(error.getMessage().contains("does not have model 'document'"));
    }

    private PageModelResponse createPageModel(String pageName) {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent root = new PageComponent();
        root.setId("root");
        root.setName(pageName);
        root.setType("container");

        page.put("root", root);
        pageModel.setPage(page);

        ContentRef rootRef = new ContentRef();
        rootRef.setRef("/page/root");
        pageModel.setRoot(rootRef);

        return pageModel;
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

    private PageModelResponse createEmptyContainer(String containerName) {
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

        List<ContentRef> childRefs = new ArrayList<>();
        for (String childName : childNames) {
            PageComponent child = new PageComponent();
            child.setId(childName + "_id");
            child.setName(childName);
            child.setType("component");
            page.put(child.getId(), child);

            ContentRef ref = new ContentRef();
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

    private PageModelResponse createCompletePageModel() {
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

        PageComponent header = new PageComponent();
        header.setId("header_id");
        header.setName("header");
        header.setType("component");

        PageComponent footer = new PageComponent();
        footer.setId("footer_id");
        footer.setName("footer");
        footer.setType("component");

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
        page.put("footer_id", footer);
        page.put("main_id", main);

        pageModel.setPage(page);

        ContentRef rootRef = new ContentRef();
        rootRef.setRef("/page/root");
        pageModel.setRoot(rootRef);

        return pageModel;
    }

    private PageModelResponse createPageModelWithModel(String componentName, String modelName) {
        PageModelResponse pageModel = new PageModelResponse();
        Map<String, PageComponent> page = new HashMap<>();

        PageComponent component = new PageComponent();
        component.setId(componentName + "_id");
        component.setName(componentName);
        component.setType("component");

        Map<String, Object> models = new HashMap<>();
        models.put(modelName, Map.of("data", "test"));
        component.setModels(models);

        page.put(component.getId(), component);
        pageModel.setPage(page);

        return pageModel;
    }
}
