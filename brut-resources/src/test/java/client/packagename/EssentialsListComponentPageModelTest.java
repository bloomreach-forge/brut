package client.packagename;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.onehippo.cms7.essentials.components.EssentialsListComponent;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;

import com.bloomreach.ps.brut.resources.AbstractPageModelTest;

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

    @BeforeEach
    public void beforeEach() {
        setupForNewRequest();
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
        testComponent("myproject",
                "essentialsListComponent",
                "/client/packagename/expected/EssentialsListComponentPageModelTest.testSingleSpaComponent.json",
                EssentialsListComponent.class,
                getParamInfo("news", "myproject:newsdocument", 5,
                        "hippostdpubwf:publicationDate", "asc", true));
    }

    //TODO determine the why this should fail
    @Test
    void testSingleEssentialsListComponentFail() {
        testComponentFail("myproject",
                "essentialsListComponent_FAIL",
                "/client/packagename/expected/EssentialsListComponentPageModelTest.testSingleSpaComponent_FAIL.json",
                EssentialsListComponent.class,
                getParamInfo("news", "myproject:newsdocument", 5,
                        "hippostdpubwf:publicationDate", "asc", true));
    }

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
