package org.example;

import java.net.URL;
import java.util.List;

import javax.jcr.RepositoryException;

import org.example.domain.AnotherType;
import org.example.domain.NewsPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.cms7.essentials.components.EssentialsListComponent;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;

import org.bloomreach.forge.brut.common.repository.utils.ImporterUtils;
import org.bloomreach.forge.brut.components.BaseComponentTest;
import org.bloomreach.forge.brut.components.exception.SetupTeardownException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EssentialsListComponentTest extends BaseComponentTest {

    private EssentialsListComponent component = new EssentialsListComponent();

    @BeforeEach
    public void setup() {
        try {
            super.setup();
            registerNodeType("ns:NewsPage", "ns:AnotherType");
            URL newsResource = getClass().getResource("/news.yaml");
            ImporterUtils.importYaml(newsResource, rootNode, "/content/documents/mychannel", "hippostd:folder");
            recalculateHippoPaths();
            setSiteContentBase("/content/documents/mychannel");
            component.init(null, componentConfiguration);
        } catch (RepositoryException e) {
            throw new SetupTeardownException(e);
        }
    }

    @AfterEach
    public void teardown() {
        super.teardown();
    }

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return "classpath*:org/onehippo/forge/**/*.class, " +
                "classpath*:com/onehippo/**/*.class, " +
                "classpath*:org/onehippo/cms7/hst/beans/**/*.class, " +
                "classpath*:org/example/domain/**/*.class";
    }

    @Test
    public void ascending() throws RepositoryException {
        setParamInfo("news", "ns:NewsPage", 5,
                "ns:releaseDate", "asc", false);
        component.doBeforeRender(request, response);
        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        List<NewsPage> items = pageable.getItems();
        assertEquals(3, items.size());
        assertEquals("news1", items.get(0).getName());
        assertEquals("news2", items.get(1).getName());
        assertEquals("news3", items.get(2).getName());
    }


    @Test
    public void descending() throws RepositoryException {
        setParamInfo("news", "ns:NewsPage", 5,
                "ns:releaseDate", "desc", false);
        component.doBeforeRender(request, response);
        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        List<NewsPage> items = pageable.getItems();
        assertEquals(3, items.size());
        assertEquals("news3", items.get(0).getName());
        assertEquals("news2", items.get(1).getName());
        assertEquals("news1", items.get(2).getName());
    }

    @Test
    public void sortByTitle() throws RepositoryException {
        setParamInfo("news", "ns:NewsPage", 5,
                "ns:title", "asc", false);
        component.doBeforeRender(request, response);
        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        List<NewsPage> items = pageable.getItems();
        assertEquals(3, items.size());
        assertEquals("news3", items.get(0).getName());
        assertEquals("news1", items.get(1).getName());
        assertEquals("news2", items.get(2).getName());
    }

    @Test
    public void pageSize() throws RepositoryException {
        setParamInfo("news", "ns:NewsPage", 2,
                "ns:releaseDate", "asc", false);
        component.doBeforeRender(request, response);
        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        List<NewsPage> items = pageable.getItems();
        assertEquals(2, items.size());
        assertEquals("news1", items.get(0).getName());
        assertEquals("news2", items.get(1).getName());
    }

    @Test
    public void paging() throws RepositoryException {
        setParamInfo("news", "ns:NewsPage", 2,
                "ns:releaseDate", "asc", false);
        request.addParameter("page", "2");
        component.doBeforeRender(request, response);
        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        List<NewsPage> items = pageable.getItems();
        assertEquals(1, items.size());
        assertEquals("news3", items.get(0).getName());
    }

    @Test
    public void search() throws RepositoryException {
        setParamInfo("news", "ns:NewsPage", 2,
                "ns:releaseDate", "asc", false);
        request.addParameter("query", "sapien");
        component.doBeforeRender(request, response);
        IterablePagination<NewsPage> pageable = getRequestAttribute("pageable");
        List<NewsPage> items = pageable.getItems();
        assertEquals(1, items.size());
        assertEquals("news1", items.get(0).getName());
        assertEquals("Pellentesque sapien tellus, commodo luctus orci sed, " +
                        "auctor pretium nulla. Sed metus justo, placerat nec hendrerit elementum",
                items.get(0).getTitle());
    }

    @Test
    public void type() throws RepositoryException {
        setParamInfo("news", "ns:AnotherType", 5,
                "ns:releaseDate", "asc", false);
        component.doBeforeRender(request, response);
        IterablePagination<AnotherType> pageable = getRequestAttribute("pageable");
        List<AnotherType> items = pageable.getItems();
        assertEquals(1, items.size());
        assertEquals("another-type", items.get(0).getName());
    }

    @Test
    public void includeSubtypes() throws RepositoryException {
        setParamInfo("news", "ns:AnotherType", 5,
                "ns:releaseDate", "asc", true);
        component.doBeforeRender(request, response);
        IterablePagination<AnotherType> pageable = getRequestAttribute("pageable");
        List<AnotherType> items = pageable.getItems();
        assertEquals(4, items.size());
    }


    private void setParamInfo(String path, String type, int pageSize, String sortField, String sortOrder, boolean includeSubtypes) {
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
