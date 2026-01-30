package org.example;

import org.bloomreach.forge.brut.components.annotation.BrxmComponentTest;
import org.bloomreach.forge.brut.components.annotation.DynamicComponentTest;
import org.example.domain.AnotherType;
import org.example.domain.NewsPage;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.onehippo.cms7.essentials.components.EssentialsListComponent;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprehensive annotation-based component test demonstrating @BrxmComponentTest framework.
 * Mirrors EssentialsListComponentTest functionality with cleaner annotation-driven setup.
 *
 * <p>This test uses field injection (rather than parameter injection) because it needs
 * access to the test instance in @BeforeEach and nested classes.</p>
 */
@BrxmComponentTest(
        beanPackages = {"org.example.domain"},
        content = "/news.yaml",
        contentRoot = "/content/documents/mychannel"
)
public class AnnotationBasedEssentialsListComponentTest {

    @SuppressWarnings("unused") // Injected by BrxmComponentTestExtension
    private DynamicComponentTest brxm;
    private EssentialsListComponent component;

    @BeforeEach
    void setupComponent() {
        component = new EssentialsListComponent();
        component.init(null, brxm.getComponentConfiguration());
    }

    private void configureListComponent(String path, String documentType,
            int pageSize, String sortField, String sortOrder, boolean includeSubtypes) {
        EssentialsListComponentInfo paramInfo = mock(EssentialsListComponentInfo.class);
        when(paramInfo.getPath()).thenReturn(path);
        when(paramInfo.getDocumentTypes()).thenReturn(documentType);
        when(paramInfo.getPageSize()).thenReturn(pageSize);
        when(paramInfo.getSortField()).thenReturn(sortField);
        when(paramInfo.getSortOrder()).thenReturn(sortOrder);
        when(paramInfo.getIncludeSubtypes()).thenReturn(includeSubtypes);
        when(paramInfo.getShowPagination()).thenReturn(true);
        brxm.setComponentParameters(paramInfo);
    }

    // ===== SORTING TESTS =====

    @Test
    @DisplayName("Sorting: ascending by release date returns items in chronological order")
    void sorting_ascendingByReleaseDate() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        List<NewsPage> items = pageable.getItems();

        assertEquals(3, items.size());
        assertEquals("news1", items.get(0).getName());
        assertEquals("news2", items.get(1).getName());
        assertEquals("news3", items.get(2).getName());
    }

    @Test
    @DisplayName("Sorting: descending by release date returns items in reverse chronological order")
    void sorting_descendingByReleaseDate() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "desc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        List<NewsPage> items = pageable.getItems();

        assertEquals(3, items.size());
        assertEquals("news3", items.get(0).getName());
        assertEquals("news2", items.get(1).getName());
        assertEquals("news1", items.get(2).getName());
    }

    @Test
    @DisplayName("Sorting: ascending by title returns items in alphabetical order")
    void sorting_ascendingByTitle() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:title", "asc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        List<NewsPage> items = pageable.getItems();

        assertEquals(3, items.size());
        assertEquals("news3", items.get(0).getName());
        assertEquals("news1", items.get(1).getName());
        assertEquals("news2", items.get(2).getName());
    }

    // ===== PAGINATION TESTS =====

    @Test
    @DisplayName("Pagination: page size limits the number of items returned")
    void pagination_pageSizeLimitsResults() {
        configureListComponent("news", "ns:NewsPage", 2, "ns:releaseDate", "asc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        List<NewsPage> items = pageable.getItems();

        assertEquals(2, items.size());
        assertEquals("news1", items.get(0).getName());
        assertEquals("news2", items.get(1).getName());
    }

    @Test
    @DisplayName("Pagination: navigate to page 2 returns remaining items")
    void pagination_navigateToPage2() {
        configureListComponent("news", "ns:NewsPage", 2, "ns:releaseDate", "asc", false);
        brxm.addRequestParameter("page", "2");
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        List<NewsPage> items = pageable.getItems();

        assertEquals(1, items.size());
        assertEquals("news3", items.get(0).getName());
    }

    // ===== SEARCH TESTS =====

    @Test
    @DisplayName("Search: filter by query term returns matching documents")
    void search_filterByQueryTerm() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
        brxm.addRequestParameter("query", "sapien");
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        List<NewsPage> items = pageable.getItems();

        assertEquals(1, items.size());
        assertEquals("news1", items.get(0).getName());
    }

    @Test
    @DisplayName("Search: verify result contains expected title content")
    void search_verifyResultTitle() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
        brxm.addRequestParameter("query", "sapien");
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        NewsPage result = pageable.getItems().get(0);

        assertTrue(result.getTitle().contains("sapien"));
        assertEquals("Pellentesque sapien tellus, commodo luctus orci sed, " +
                "auctor pretium nulla. Sed metus justo, placerat nec hendrerit elementum",
                result.getTitle());
    }

    // ===== TYPE FILTERING TESTS =====

    @Test
    @DisplayName("TypeFiltering: filter by specific document type returns only matching types")
    void typeFiltering_filterByDocumentType() {
        configureListComponent("news", "ns:AnotherType", 5, "ns:releaseDate", "asc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<AnotherType> pageable = brxm.getRequestAttributeValue("pageable");
        List<AnotherType> items = pageable.getItems();

        assertEquals(1, items.size());
        assertEquals("another-type", items.get(0).getName());
    }

    // ===== BEAN PROPERTY TESTS =====

    @Test
    @DisplayName("BeanProperty: access title property from NewsPage bean")
    void beanProperty_accessTitle() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        NewsPage firstNews = pageable.getItems().get(0);

        assertNotNull(firstNews.getTitle());
        assertTrue(firstNews.getTitle().length() > 0);
    }

    @Test
    @DisplayName("BeanProperty: access introduction property from NewsPage bean")
    void beanProperty_accessIntroduction() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        NewsPage firstNews = pageable.getItems().get(0);

        assertNotNull(firstNews.getIntroduction());
        assertTrue(firstNews.getIntroduction().contains("Lorem ipsum"));
    }

    @Test
    @DisplayName("BeanProperty: access relatedNews linked bean from NewsPage")
    void beanProperty_accessRelatedNewsLinkedBean() {
        configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "desc", false);
        component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

        IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
        NewsPage news3 = pageable.getItems().get(0);

        assertEquals("news3", news3.getName());
        NewsPage relatedNews = news3.getRelatedNews();
        assertNotNull(relatedNews, "news3 should have a related news item");
        assertEquals("news2", relatedNews.getName());
    }

    // ===== HIPPO BEAN RETRIEVAL TESTS =====

    @Test
    @DisplayName("HippoBeanRetrieval: retrieve bean directly by absolute path")
    void hippoBeanRetrieval_byPath() {
        HippoBean newsFolder = brxm.getHippoBean("/content/documents/mychannel/news");

        assertNotNull(newsFolder);
        assertEquals("news", newsFolder.getName());
    }

    @Test
    @DisplayName("HippoBeanRetrieval: verify handle structure for document retrieval")
    void hippoBeanRetrieval_verifyHandleStructure() throws RepositoryException {
        Node handleNode = brxm.getRootNode().getNode("content/documents/mychannel/news/news1");

        assertTrue(handleNode.isNodeType("hippo:handle"));
        assertTrue(handleNode.hasNode("news1"));
        assertEquals("ns:NewsPage", handleNode.getNode("news1").getPrimaryNodeType().getName());
    }

    @Test
    @DisplayName("HippoBeanRetrieval: retrieve folder as HippoFolder bean")
    void hippoBeanRetrieval_folderBean() {
        HippoBean newsFolder = brxm.getHippoBean("/content/documents/mychannel/news");

        assertNotNull(newsFolder);
        assertTrue(newsFolder instanceof HippoFolder, "News folder should be a HippoFolder");
    }

    // ===== INFRASTRUCTURE TESTS =====

    @Test
    @DisplayName("Infrastructure: request context is properly initialized")
    void infrastructure_requestContextInitialized() throws RepositoryException {
        assertNotNull(brxm.getHstRequestContext());
        assertNotNull(brxm.getHstRequestContext().getSession());
        assertNotNull(brxm.getHstRequestContext().getObjectBeanManager());
    }

    @Test
    @DisplayName("Infrastructure: root node provides JCR access")
    void infrastructure_rootNodeJcrAccess() throws RepositoryException {
        Node rootNode = brxm.getRootNode();

        assertNotNull(rootNode);
        assertTrue(rootNode.hasNode("content"));
        assertTrue(rootNode.hasNode("hippo:configuration"));
    }

    @Test
    @DisplayName("Infrastructure: mock HST objects are available for component testing")
    void infrastructure_mockHstObjectsAvailable() {
        assertNotNull(brxm.getHstRequest());
        assertNotNull(brxm.getHstResponse());
        assertNotNull(brxm.getComponentConfiguration());
        assertNotNull(brxm.getResolvedSiteMapItem());
    }

    // ===== NESTED CLASS TESTS =====

    @Nested
    @DisplayName("Nested: field injection from outer class")
    class NestedClassTests {

        @Test
        @DisplayName("brxm field from outer class is accessible in nested test")
        void nestedTest_canAccessOuterBrxmField() {
            assertNotNull(brxm, "brxm field should be injected from outer class");
            assertNotNull(brxm.getHstRequest());
            assertNotNull(brxm.getHstRequestContext());
        }

        @Test
        @DisplayName("component operations work in nested test class")
        void nestedTest_componentOperationsWork() {
            configureListComponent("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
            component.doBeforeRender(brxm.getHstRequest(), brxm.getHstResponse());

            IterablePagination<NewsPage> pageable = brxm.getRequestAttributeValue("pageable");
            assertNotNull(pageable);
            assertEquals(3, pageable.getItems().size());
        }

        @Nested
        @DisplayName("Deeply nested class")
        class DeeplyNestedTests {

            @Test
            @DisplayName("brxm field accessible from deeply nested test")
            void deeplyNestedTest_canAccessBrxmField() {
                assertNotNull(brxm, "brxm should be accessible from deeply nested class");
                HippoBean newsFolder = brxm.getHippoBean("/content/documents/mychannel/news");
                assertNotNull(newsFolder);
                assertEquals("news", newsFolder.getName());
            }
        }
    }
}
