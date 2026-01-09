package client.packagename;

import client.packagename.model.ListItemPagination;
import client.packagename.model.NewsItemRep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.brut.resources.AbstractJaxrsTest;
import org.bloomreach.forge.brut.resources.AbstractResourceTest;
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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A user (client) of the testing library providing his/her own config/content
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JaxrsTest extends AbstractJaxrsTest {

    private ListAppender<ILoggingEvent> logAppender;
    private Logger abstractResourceTestLogger;
    private Logger cxfLogger;

    @BeforeAll
    public void init() {
        super.init();

        logAppender = new ListAppender<>();
        logAppender.start();

        abstractResourceTestLogger = (Logger) LoggerFactory.getLogger(AbstractResourceTest.class);
        abstractResourceTestLogger.addAppender(logAppender);

        cxfLogger = (Logger) LoggerFactory.getLogger("org.apache.cxf");
        cxfLogger.addAppender(logAppender);
    }

    @BeforeEach
    public void beforeEach() {
        setupForNewRequest();
        logAppender.list.clear();
    }

    @AfterAll
    public void afterAll() {
        abstractResourceTestLogger.detachAppender(logAppender);
        cxfLogger.detachAppender(logAppender);
        super.destroy();
    }

    @Override
    protected void setupForNewRequest() {
        setupHstRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
        setupServletContext();
        super.setupForNewRequest();
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

    @Test
    @DisplayName("Verify RequestContextProvider is available in JAX-RS resources")
    public void testRequestContextProviderAvailable() {
        getHstRequest().setRequestURI("/site/api/hello/request-context-available");
        String response = invokeFilter();
        assertEquals("PASS", response,
                "RequestContextProvider.get() should return non-null HstRequestContext in JAX-RS resource");
    }

    @Test
    @DisplayName("Verify exceptions are logged with full stack trace")
    public void testExceptionLoggingWithFullStackTrace() {
        getHstRequest().setRequestURI("/site/api/hello/exception-with-stack-trace");
        invokeFilter();

        List<ILoggingEvent> logEvents = logAppender.list;
        List<ILoggingEvent> logsWithException = logEvents.stream()
                .filter(event -> event.getThrowableProxy() != null)
                .filter(event -> event.getThrowableProxy().getMessage() != null)
                .filter(event -> event.getThrowableProxy().getMessage().contains("Test exception"))
                .toList();

        assertFalse(logsWithException.isEmpty(),
                "Exception should be logged with full details, not swallowed");
        ILoggingEvent exceptionLog = logsWithException.get(0);

        assertNotNull(exceptionLog.getThrowableProxy(),
                "Exception log must include full throwable/stack trace, not just message string");
        assertTrue(exceptionLog.getThrowableProxy().getStackTraceElementProxyArray().length > 0,
                "Stack trace must contain actual stack frames, not be empty");

        String firstStackFrame = exceptionLog.getThrowableProxy().getStackTraceElementProxyArray()[0].toString();
        assertNotNull(firstStackFrame,
                "Stack frames should be available for debugging");
    }

}
