package com.bloomreach.ps.brut.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.jcr.Repository;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.container.HstDelegateeFilterBean;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.internal.PlatformModelAvailableService;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.platform.container.sitemapitemhandler.HstSiteMapItemHandlerFactories;
import org.hippoecm.hst.platform.container.sitemapitemhandler.HstSiteMapItemHandlerFactoriesImpl;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.hippoecm.hst.platform.services.PlatformServicesImpl;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletContext;

public abstract class AbstractResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceTest.class);

    private static final String HST_RESET_FILTER = "org.hippoecm.hst.container.HstFilter.reset";

    protected SpringComponentManager componentManager;
    protected MockHstRequest hstRequest;
    protected MockHstResponse hstResponse;
    protected MockServletContext servletContext;

    protected HstModelRegistryImpl hstModelRegistry;
    protected PlatformServicesImpl platformServices;
    protected PlatformModelAvailableService platformModelAvailableService;

    public SpringComponentManager getComponentManager() {
        return componentManager;
    }

    public MockHstRequest getHstRequest() {
        return hstRequest;
    }

    protected abstract String getAnnotatedHstBeansClasses();

    protected void setupHstResponse() {
        this.hstResponse = new MockHstResponse();
    }

    protected void setupServletContext() {
        servletContext = new MockServletContext();
        servletContext.setContextPath("/site");
        servletContext.setInitParameter(DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM,
                getAnnotatedHstBeansClasses());
        hstRequest.setServletContext(servletContext);

        componentManager.setServletContext(servletContext);
        if (HippoWebappContextRegistry.get().getContext("/site") == null) {
            HippoWebappContextRegistry.get().register(new HippoWebappContext(HippoWebappContext.Type.SITE, servletContext));
        }
        HstManagerImpl hstManager = (HstManagerImpl) componentManager.getComponent(HstManager.class);
        hstManager.setServletContext(hstRequest.getServletContext());
    }

    protected void setupHstPlatform() {
        hstModelRegistry = new HstModelRegistryImpl();
        platformServices = new PlatformServicesImpl();
        platformModelAvailableService = new PlatformModelAvailableService() {
        };

        platformServices.setHstModelRegistry(hstModelRegistry);
        platformServices.init();
        hstModelRegistry.setRepository(getComponentManager().getComponent(Repository.class));
        hstModelRegistry.init();
    }

    protected void registerHstModel() {
        hstModelRegistry.registerHstModel(servletContext, componentManager, true);
    }

    protected void unregisterHstModel() {
        hstModelRegistry.unregisterHstModel(servletContext);
    }


    public void destroy() {
        HippoServiceRegistry.unregister(platformModelAvailableService, PlatformModelAvailableService.class);

        HstServices.setComponentManager(null);

        HstSiteMapItemHandlerFactoriesImpl hstSiteMapItemHandlerFactories = (HstSiteMapItemHandlerFactoriesImpl) getComponentManager().getComponent(HstSiteMapItemHandlerFactories.class);
        hstSiteMapItemHandlerFactories.destroy();
        hstSiteMapItemHandlerFactories.unregister(servletContext.getContextPath());

        unregisterHstModel();
        hstModelRegistry.destroy();
        hstModelRegistry.setRepository(null);

        platformServices.destroy();
        platformServices.setPreviewDecorator(null);
        platformServices.setHstModelRegistry(null);
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

    protected void includeAdditionalSpringConfigurations() {
        ArrayList<String> configList = new ArrayList<>(Arrays.asList(this.componentManager.getConfigurationResources()));
        configList.addAll(contributeSpringConfigurationLocations());
        this.componentManager.setConfigurationResources(configList);
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
        unregisterHstModel();
        registerHstModel();
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

    /**
     * Perform validation before invoking the HST filter
     */
    protected abstract void performValidation();
}
