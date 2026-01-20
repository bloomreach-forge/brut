package org.example;

import org.bloomreach.forge.brut.components.BaseComponentTest;
import org.bloomreach.forge.brut.components.annotation.BrxmComponentTest;
import org.example.domain.NewsPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.onehippo.cms7.essentials.components.EssentialsListComponent;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;

import javax.jcr.RepositoryException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Example of annotation-based component testing with zero-config setup.
 * Demonstrates @BrxmComponentTest for simplified test writing.
 *
 * @since 5.1.0
 */
@BrxmComponentTest(
    contentPath = "/news.yaml",
    nodeTypes = {"ns:NewsPage", "ns:AnotherType"},
    importPath = "/content/documents/mychannel",
    parentNodeType = "hippostd:folder",
    recalculateHippoPaths = true
)
public class AnnotatedComponentTest extends BaseComponentTest {

    private EssentialsListComponent component = new EssentialsListComponent();

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return "classpath*:org/onehippo/forge/**/*.class, " +
                "classpath*:com/onehippo/**/*.class, " +
                "classpath*:org/onehippo/cms7/hst/beans/**/*.class, " +
                "classpath*:org/example/domain/**/*.class";
    }

    @Test
    @DisplayName("Test component with annotation-based setup")
    void testComponentWithAnnotation() throws RepositoryException {
        setSiteContentBase("/content/documents/mychannel");
        component.init(null, componentConfiguration);

        setParamInfo("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
        component.doBeforeRender(request, response);

        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        List<NewsPage> items = pageable.getItems();

        assertEquals(3, items.size(), "Should have 3 news items");
        assertEquals("news1", items.get(0).getName());
    }

    @Test
    @DisplayName("Test sorting ascending")
    void testSortingAscending() throws RepositoryException {
        setSiteContentBase("/content/documents/mychannel");
        component.init(null, componentConfiguration);

        setParamInfo("news", "ns:NewsPage", 5, "ns:releaseDate", "asc", false);
        component.doBeforeRender(request, response);

        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        assertEquals("news1", pageable.getItems().get(0).getName());
    }

    @Test
    @DisplayName("Test sorting descending")
    void testSortingDescending() throws RepositoryException {
        setSiteContentBase("/content/documents/mychannel");
        component.init(null, componentConfiguration);

        setParamInfo("news", "ns:NewsPage", 5, "ns:releaseDate", "desc", false);
        component.doBeforeRender(request, response);

        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        assertEquals("news3", pageable.getItems().get(0).getName());
    }

    private void setParamInfo(String path, String type, int pageSize, String sortField,
                             String sortOrder, boolean includeSubtypes) {
        EssentialsListComponentInfo paramInfo = mock(EssentialsListComponentInfo.class);
        when(paramInfo.getDocumentTypes()).thenReturn(type);
        when(paramInfo.getIncludeSubtypes()).thenReturn(includeSubtypes);
        when(paramInfo.getPath()).thenReturn(path);
        when(paramInfo.getPageSize()).thenReturn(pageSize);
        when(paramInfo.getShowPagination()).thenReturn(true);
        when(paramInfo.getSortField()).thenReturn(sortField);
        when(paramInfo.getSortOrder()).thenReturn(sortOrder);
        setComponentParameterInfo(paramInfo);
    }

}
