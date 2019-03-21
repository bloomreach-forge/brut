package client.packagename;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import com.bloomreach.ps.brut.resources.AbstractPageModelTest;
import com.bloomreach.ps.brut.resources.SkeletonRepository;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.core.parameters.Parameter;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.onehippo.cms7.essentials.components.EssentialsListComponent;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A user (client) of the testing library providing his/her own config/content
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EssentialsListComponentPageModelTest extends AbstractPageModelTest {

    @BeforeAll
    public void init() {
        super.init();

    }

    @AfterAll
    public void destroy() {
        super.destroy();
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:client/packagename/beans/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Collections.singletonList("/client/packagename/custom-pagemodel.xml");
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return null;
    }


    @Test
    void testSingleEssentialsListComponent() {
        testComponent("myhippoproject",
                "essentialsListComponent",
                "/client/packagename/expected/EssentialsListComponentPageModelTest.testSingleSpaComponent.json",
                EssentialsListComponent.class,
                getParamInfo("news", "myhippoproject:newsdocument", 5,
                        "hippostdpubwf:publicationDate", "asc", true));


    }

//    @Test
//    void testSingleEssentialsListComponentFail() {
//        testComponentFail("myhippoproject",
//                "essentialsListComponent_FAIL",
//                "/client/packagename/expected/EssentialsListComponentPageModelTest.testSingleSpaComponent_FAIL.json",
//                EssentialsListComponent.class,
//                getParamInfo("news", "myhippoproject:newsdocument", 5,
//                        "hippostdpubwf:publicationDate", "asc", true));
//
//
//    }

    private EssentialsListComponentInfo getParamInfo(String path, String type, int pageSize, String sortField, String sortOrder, boolean includeSubtypes) {
        EssentialsListComponentInfo paramInfo = mock(EssentialsListComponentInfo.class);
        when(paramInfo.getDocumentTypes()).thenReturn(type);
        when(paramInfo.getIncludeSubtypes()).thenReturn(includeSubtypes);
        when(paramInfo.getPath()).thenReturn(path);
        when(paramInfo.getPageSize()).thenReturn(pageSize);
        when(paramInfo.getShowPagination()).thenReturn(true);
        when(paramInfo.getSortField()).thenReturn(sortField);
        when(paramInfo.getSortOrder()).thenReturn(sortOrder);
        return paramInfo;
    }


}
