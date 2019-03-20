package org.example;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.example.model.ListItemPagination;
import org.example.model.NewsItemRep;
import org.hippoecm.hst.configuration.cache.HstEvent;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.model.HstManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.bloomreach.ps.brut.resources.AbstractJaxrsTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

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

    private void setupForNewRequest() {
        setupHstRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
        setupServletContext();
        setupHstResponse();
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/example/model/*.class,";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/org/example/custom-jaxrs.xml", "/org/example/rest-resources.xml");
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return null;
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

        markMountNodeAsStale();
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
        String rootMountPath = "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root";
        Node rootMount = session.getNode(rootMountPath);
        rootMount.setProperty("hst:parameternames", paramNames);
        rootMount.setProperty("hst:parametervalues", paramValues);
        session.save();
        session.logout();
    }

    private void markMountNodeAsStale() {
        String rootMountPath = "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root";
        //Changed a node after the hst model is loaded! Have to invalidate the hst model
        // and force a lookup on the next read of the node
        HstManager hstManager = getComponentManager().getComponent(HstManager.class);
        hstManager.markStale();
        HstNodeLoadingCache hstNodeLoadingCache = getComponentManager().getComponent(HstNodeLoadingCache.class);
        //updated a property on the node hence boolean is set to true
        HstEvent propertyEvent = new HstEvent(rootMountPath, true);
        HashSet<HstEvent> eventSet = Sets.newHashSet(propertyEvent);
        hstNodeLoadingCache.handleEvents(eventSet);
    }

}
