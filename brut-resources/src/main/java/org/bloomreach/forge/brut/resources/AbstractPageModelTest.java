package org.bloomreach.forge.brut.resources;

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
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPageModelTest extends AbstractResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPageModelTest.class);

    private static final String PAGEMODEL_ADDON_PATH = "org/bloomreach/forge/brut/resources/hst/pagemodel-addon/module.xml";

    public void init() {
        setupComponentManager();
        setupHstRequest();
        setupServletContext();
        setupHstResponse();
    }

    protected void setupForNewRequest() {
        setupHstRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
        setupServletContext();
        setupHstResponse();
    }

    @Override
    public void performValidation() {
        if (hstRequest.getRequestURI() == null || "".equals(hstRequest.getRequestURI())) {
            throw new IllegalStateException("Request URI was missing in hstRequest");
        }
    }

    protected void setupHstRequest() {
        this.hstRequest = new MockHstRequest();
        hstRequest.setContextPath("/site");
        hstRequest.setHeader("Host", "localhost:8080");
        hstRequest.setHeader("X-Forwarded-Proto", "http");
    }

    protected void setupComponentManager() {
        this.componentManager = new SpringComponentManager();
        includeAdditionalSpringConfigurations();
        includeAdditionalAddonModules();
        componentManager.setAddonModuleDefinitions(Collections.singletonList(Utils.loadAddonModule(PAGEMODEL_ADDON_PATH)));
        componentManager.initialize();
        HstServices.setComponentManager(componentManager);
    }

    private void includeAdditionalAddonModules() {
        if (contributeAddonModulePaths() != null) {
            //load pagemodel addon by default
            ModuleDefinition pageModelAddonDefinition = Utils.loadAddonModule(PAGEMODEL_ADDON_PATH);
            List<ModuleDefinition> contributedDefinitions = contributeAddonModulePaths().stream()
                    .map(Utils::loadAddonModule)
                    .collect(Collectors.toList());
            contributedDefinitions.add(0, pageModelAddonDefinition); //add pagemodel addon as first
            this.componentManager.setAddonModuleDefinitions(contributedDefinitions);
        }
    }

    public void destroy() {

    }

    protected void testComponent(final String hstConfig, final String id, final String expectedResource, final Class componentClass, final Object paramInfo) {
        importComponent(hstConfig, id, componentClass, paramInfo);

        String response = getResponseById(id);
        String expected = getExpectedResponse(expectedResource);

        try {
            JSONAssert.assertEquals(componentClass.getName() + " does not contain the expected response",
                    expected, response, JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            LOGGER.error("Error while testing component during Page Model Test");
        }

    }

    protected void testComponentFail(final String hstConfig, final String id, final String expectedResource, final Class componentClass, final Object paramInfo) {
        importComponent(hstConfig, id, componentClass, paramInfo);

        String response = getResponseById(id);
        String expected = getExpectedResponse(expectedResource);

        try {
            JSONAssert.assertNotEquals(componentClass.getName() + " does not contain the expected response",
                    expected, response, JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            LOGGER.error("Error while testing component during Page Model Test - onFail");
        }

    }

    protected String getResponseById(final String id) {
        getHstRequest().setRequestURI("/site/resourceapi/" + id);
        return removeRefIdFromJsonString(invokeFilter());
    }

    public String removeRefIdFromJsonString(String jsonString) {
        // Handle null input
        if (jsonString == null) {
            return null;
        }
        // Replace all ref id values with empty strings
        jsonString = jsonString.replaceAll(
                "(\\n?\\s*\"id\"\\s?:\\s?\")[^\\n\"]*(\",?\\n?)", "$1$2");

        return jsonString.replaceAll(
                "(\\n?\\s*ref=)[^\\n\"]*(\",?\\n?)", "$1$2");

    }

    protected String getExpectedResponse(final String expectedResource) {
        String expected = null;
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = getClass().getResourceAsStream(expectedResource);
            expected = IOUtils.toString(resourceAsStream, Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("IO exception retrieving contents of file " + expected);
        } finally {
            IOUtils.closeQuietly(resourceAsStream);
        }
        return removeRefIdFromJsonString(expected);
    }

    protected void importComponent(final String hstConfig, final String id, final Class componentClass, final Object paramInfo) {
        try {
            createSitemapItem(hstConfig, id);
            createPageDefinition(hstConfig, id, componentClass, paramInfo);
            invalidateHstModel();
        } catch (RepositoryException | IllegalAccessException | NoSuchFieldException e) {
            LOGGER.error("Exception during import of component " + id);
        }


    }


    protected void createPageDefinition(final String hstConfig, final String id, final Class componentClass, final Object paramInfo) throws RepositoryException {
        Map<String, String> properties = new HashMap<>();
        properties(properties, paramInfo);
        Set<String> names = properties.keySet();
        Collection<String> values = properties.values();
        Session session = getSession();
        Node pages = session.getNode("/hst:hst/hst:configurations/" + hstConfig + "/hst:pages");
        Optional<Node> pageDefinition = Optional.of(!pages.hasNode(id) ? pages.addNode(id, "hst:component") : pages.getNode(id));//.orElseThrow(RuntimeException);
        if (pageDefinition.isPresent()) {
            Node node = pageDefinition.get();
            node.setProperty("hst:componentclassname", componentClass.getName());
            node.setProperty("hst:parameternames", names.toArray(new String[0]));
            node.setProperty("hst:parametervalues", values.toArray(new String[0]));
            session.save();
            session.logout();
        }

    }

    protected void createSitemapItem(final String hstConfig, final String id) throws RepositoryException {
        Session session = getSession();
        Node sitemap = session.getNode("/hst:hst/hst:configurations/" + hstConfig + "/hst:sitemap");
        Node sitemapItem = (!sitemap.hasNode(id) ? sitemap.addNode(id, "hst:sitemapitem") : sitemap.getNode(id));
        sitemapItem.setProperty("hst:componentconfigurationid", "hst:pages/" + id);
        session.save();
        session.logout();

    }

    protected Session getSession() throws RepositoryException {
        return getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    protected Repository getRepository() {
        return getComponentManager().getComponent(Repository.class);
    }

    protected void properties(final Map<String, String> properties, Object componentInfo) {
        for (Method method : componentInfo.getClass().getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                // new style annotations
                Parameter propAnnotation = method.getAnnotation(Parameter.class);
                try {
                    properties.put(propAnnotation.name(), String.valueOf(method.invoke(componentInfo)));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error("Error retrieving the properties " + propAnnotation.name());
                }
            }
        }
    }


}
