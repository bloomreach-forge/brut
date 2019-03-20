package com.bloomreach.ps.brut.resources;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.container.HstDelegateeFilterBean;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockServletContext;

public abstract class AbstractJaxrsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJaxrsTest.class);

    private SpringComponentManager componentManager;
    private MockHstRequest hstRequest;
    private MockHstResponse hstResponse;

    private static final String HST_RESET_FILTER = "org.hippoecm.hst.container.HstFilter.reset";
    private static final int DEFAULT_BYTE_ARRAY_INPUT_STREAM_SIZE = 1024;

    public void init() {
        setupComponentManager();
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
        if (hstRequest.getMethod() == null) {
            throw new IllegalStateException(("Method name was missing hstRequest"));
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
        hstRequest.setInputStream(new DelegatingServletInputStream(new ByteArrayInputStream(new byte[getServletInputStreamSize()])));
        hstRequest.setScheme("http");
        hstRequest.setServerName("localhost:8080");
    }

    protected void setupComponentManager() {
        this.componentManager = new SpringComponentManager();
        includeAdditionalSpringConfigurations();
        includeAdditionalAddonModules();
        componentManager.initialize();
        HstServices.setComponentManager(componentManager);
    }

    private void includeAdditionalAddonModules() {
        if (contributeAddonModulePaths() != null) {
            List<ModuleDefinition> moduleDefinitions = contributeAddonModulePaths().stream()
                    .map(Utils::loadAddonModule)
                    .collect(Collectors.toList());
            this.componentManager.setAddonModuleDefinitions(moduleDefinitions);
        }
    }

    private void includeAdditionalSpringConfigurations() {
        ArrayList<String> configList = new ArrayList<>(Arrays.asList(this.componentManager.getConfigurationResources()));
        configList.addAll(contributeSpringConfigurationLocations());
        this.componentManager.setConfigurationResources(configList);
    }

    /**
     * Return any additional spring xml locations to be included in the spring application context The returned value
     * should be a pattern
     *
     * @return
     */

    protected abstract List<String> contributeSpringConfigurationLocations();

    protected abstract List<String> contributeAddonModulePaths();

    protected int getServletInputStreamSize() {
        return DEFAULT_BYTE_ARRAY_INPUT_STREAM_SIZE;
    }

    public SpringComponentManager getComponentManager() {
        return componentManager;
    }

    public MockHstRequest getHstRequest() {
        return hstRequest;
    }
}
