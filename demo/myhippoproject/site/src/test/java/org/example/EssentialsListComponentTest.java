package org.example;

import java.net.URL;
import java.util.List;

import javax.jcr.RepositoryException;

import org.example.beans.NewsDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.onehippo.cms7.essentials.components.EssentialsListComponent;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;

import com.bloomreach.ps.brxm.jcr.repository.utils.ImporterUtils;
import com.bloomreach.ps.brxm.unittester.BaseHippoTest;
import com.bloomreach.ps.brxm.unittester.exception.SetupTeardownException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EssentialsListComponentTest extends BaseHippoTest {

    private EssentialsListComponent component = new EssentialsListComponent();

    @Before
    public void setup() {
        try {
            super.setup();
            registerNodetypes();
            URL resource = getClass().getResource("/news.yaml");
            ImporterUtils.importYaml(resource, rootNode, "/content/documents/mychannel", "hippostd:folder");
            recalculateHippoPaths();
            setSiteContentBase("/content/documents/mychannel");
            component.init(null, componentConfiguration);
        } catch (RepositoryException e) {
            throw new SetupTeardownException(e);
        }
    }

    @After
    public void teardown() {
        super.teardown();
    }

    @Override
    protected String getAnnotatedClassesResourcePath() {
        return "classpath*:org/onehippo/forge/**/*.class, " +
                "classpath*:com/onehippo/**/*.class, " +
                "classpath*:org/onehippo/cms7/hst/beans/**/*.class, " +
                "classpath*:org/example/beans/**/*.class";
    }

    @Test
    public void simpleTest() throws RepositoryException {
        setParamInfo("news", "myhippoproject:newsdocument", 5,
                "hippostdpubwf:publicationDate", "asc", false);
        component.doBeforeRender(request, response);
        IterablePagination<NewsDocument> pageable = getRequestAttribute("pageable");
        List<NewsDocument> items = pageable.getItems();
        Assert.assertEquals(3, items.size());
        Assert.assertEquals("The gastropoda news", items.get(0).getTitle());
        Assert.assertEquals("The medusa news", items.get(1).getTitle());
        Assert.assertEquals("2013 harvest", items.get(2).getTitle());
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

    private void registerNodetypes() throws RepositoryException {
        registerMixinType("hippo:named");
        registerMixinType("hippostd:publishableSummary");

        registerNodeType("hippo:mirror");
        registerNodeType("hippostd:html");
        registerNodeType("myhippoproject:newsdocument");
        registerNodeType("hippogallerypicker:imagelink");
    }
}
