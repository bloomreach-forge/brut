package client.packagename;

import client.packagename.model.ListItemPagination;
import client.packagename.model.NewsItemRep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.AbstractJaxrsTest;
import org.junit.jupiter.api.*;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A user (client) of the testing library providing his/her own config/content
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JaxrsTest extends AbstractJaxrsTest {

    @BeforeAll
    public void init() {
        super.init();
    }

    @BeforeEach
    public void beforeEach() {
        setupForNewRequest();
    }

    @AfterAll
    public void afterAll() {
        super.destroy();
    }

    private void setupForNewRequest() {
        setupHstRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
        setupServletContext();
        unregisterHstModel();
        registerHstModel();
        setupHstResponse();
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:client/packagename/model/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/client/packagename/custom-jaxrs.xml", "/client/packagename/rest-resources.xml");
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
    @DisplayName("Test invoking the user endpoint")
    public void testUserEndpoint() {
        String user = "baris";
        getHstRequest().setRequestURI("/site/api/hello/" + user);
        String response = invokeFilter();
        assertEquals("Hello, World! " + user, response);
    }

    @Test
    @DisplayName("Test HST config changes are not visible if HST model is not reloaded after a node update via JCR API")
    public void testMountParamsUpdated() throws Exception {
        String key = "paramName";
        String value = "paramValue";
        getHstRequest().setRequestURI("/site/api/hello/mount/" + key);
        String response = invokeFilter();
        setParamsOnMount(new String[]{key}, new String[]{value});
        assertEquals("", response,
                "Expected nothing to change since the HST model was not explicitly reloaded");

        invalidateHstModel();
        String response2 = invokeFilter();
        assertEquals(value, response2, "Expected param value to be updated since HST model was loaded");
    }

    @Test
    @DisplayName("Test running HST query in news endpoint")
    public void testNewsEndpoint() throws Exception {
        getHstRequest().setRequestURI("/site/api/news");
        String response = invokeFilter();
        ListItemPagination<NewsItemRep> pageable = new ObjectMapper().readValue(response, new TypeReference<ListItemPagination<NewsItemRep>>() {
        });
        assertEquals(3, pageable.getItems().size(), "Pageable didn't have enough results");
    }

    private void setParamsOnMount(String[] paramNames, String[] paramValues) throws Exception {
        Repository repository = getComponentManager().getComponent(Repository.class);
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        String rootMountPath = "/hst:myproject/hst:hosts/dev-localhost/localhost/hst:root";
        Node rootMount = session.getNode(rootMountPath);
        rootMount.setProperty("hst:parameternames", paramNames);
        rootMount.setProperty("hst:parametervalues", paramValues);
        session.save();
        session.logout();
    }

}
