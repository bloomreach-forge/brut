package com.bloomreach.ps.brut.resources;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.container.HstDelegateeFilterBean;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.json.JSONException;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletContext;

public abstract class AbstractPageModelTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPageModelTest.class);

    private static final String PAGEMODEL_ADDON_PATH = "com/bloomreach/ps/brut/resources/hst/pagemodel-addon/module.xml";
    private static final String HST_RESET_FILTER = "org.hippoecm.hst.container.HstFilter.reset";
    private SpringComponentManager componentManager;
    private MockHstRequest hstRequest;
    private MockHstResponse hstResponse;

    public void init() {
        setupComponentManager();
//        setupHstRequest();
//        setupServletContext();
//        setupHstResponse();
    }

    protected void setupForNewRequest() {
        setupHstRequest();
        getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        getHstRequest().setMethod(HttpMethod.GET);
        setupServletContext();
        setupHstResponse();
    }

    /**
     * @return Result json as string
     */

    @Nullable
    protected String invokeFilter() {

        performValidation();
        //Invoke
        HstDelegateeFilterBean filter = componentManager.getComponent(HstFilter.class.getName());
        try {
            filter.doFilter(hstRequest, hstResponse, null);
            String contentAsString = hstResponse.getContentAsString();
            LOGGER.info(contentAsString);
            //important! set the filter done attribute to null for subsequent filter invocations
            hstRequest.setAttribute(HST_RESET_FILTER, true);
            return contentAsString;
        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage());
        }
        return null;
    }

    private void performValidation() {
        if (hstRequest.getRequestURI() == null || "".equals(hstRequest.getRequestURI())) {
            throw new IllegalStateException("Request URI was missing in hstRequest");
        }
    }

    protected void setupHstResponse() {
        this.hstResponse = new MockHstResponse();
    }

    protected void setupServletContext() {
        MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath("/site");
        servletContext.setInitParameter(DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM,
                getAnnotatedHstBeansClasses());
        hstRequest.setServletContext(servletContext);
        componentManager.setServletContext(servletContext);
        if (ServletContextRegistry.getContext("/site") == null) {
            ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.HST);
        }
        HstManagerImpl hstManager = (HstManagerImpl) componentManager.getComponent(HstManager.class);
        hstManager.setServletContext(hstRequest.getServletContext());
    }

    protected abstract String getAnnotatedHstBeansClasses();

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

    private void includeAdditionalSpringConfigurations() {
        ArrayList<String> configList = new ArrayList<>(Arrays.asList(this.componentManager.getConfigurationResources()));
        configList.addAll(contributeSpringConfigurationLocations());
        this.componentManager.setConfigurationResources(configList);
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

    /**
     * Return any additional spring xml locations to be included in the spring application context The returned value
     * should be a pattern
     *
     * @return
     */

    protected abstract List<String> contributeSpringConfigurationLocations();

    /**
     * Return any additional hst addon module location patterns
     *
     * @return
     */
    protected abstract List<String> contributeAddonModulePaths();

    public SpringComponentManager getComponentManager() {
        return componentManager;
    }

    public MockHstRequest getHstRequest() {
        return hstRequest;
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

    /**
     * This method can be called by sub-testclasses after node changes via JCR API. Invalidating the HST model is
     * necessary only if the hst configuration nodes are updated. In a brXM project normally this is done via JCR level
     * event handlers but within the test execution context we cannot have asyncrhonous events. We have to explicitly
     * force the HST to do a node lookup in the repository.
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void invalidateHstModel() throws NoSuchFieldException, IllegalAccessException {
        HstNodeLoadingCache hstNodeLoadingCache = getComponentManager().getComponent(HstNodeLoadingCache.class);
        Field rootNodeField = hstNodeLoadingCache.getClass().getDeclaredField("rootNode");
        rootNodeField.setAccessible(true);
        rootNodeField.set(hstNodeLoadingCache, null);
        HstManager hstManager = getComponentManager().getComponent(HstManager.class);
        hstManager.markStale();
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
        Optional<Node> sitemapItem = Optional.of(!sitemap.hasNode(id) ? sitemap.addNode(id, "hst:sitemapitem") : sitemap.getNode(id));//.orElseThrow(RuntimeException);
        if (sitemapItem.isPresent()) {
            Node node = sitemapItem.get();
            node.setProperty("hst:componentconfigurationid", "hst:pages/" + id);
            session.save();
            session.logout();
        }

    }

    protected Session getSession() throws RepositoryException {
        return getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    protected SkeletonRepository getRepository() {
        return getComponentManager().getComponent(SkeletonRepository.class);
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
