package org.bloomreach.forge.brut.resources;

import org.hippoecm.hst.container.HstDelegateeFilterBean;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.internal.PlatformModelAvailableService;
import org.hippoecm.hst.core.request.HstRequestContext;
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

import javax.jcr.Repository;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceTest.class);

    private static final String HST_RESET_FILTER = "org.hippoecm.hst.container.HstFilter.reset";
    private static final Object WEBAPP_CONTEXT_LOCK = new Object();

    protected SpringComponentManager componentManager = new SpringComponentManager();
    protected MockHstRequest hstRequest;
    protected MockHstResponse hstResponse;
    protected MockServletContext servletContext = new MockServletContext();
    ;

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

    public HstModelRegistryImpl getHstModelRegistry() {
        return hstModelRegistry;
    }

    public PlatformServicesImpl getPlatformServices() {
        return platformServices;
    }

    protected void setupServletContext() {
        servletContext.setContextPath("/site");
        servletContext.setInitParameter(DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM,
                getAnnotatedHstBeansClasses());
        hstRequest.setServletContext(servletContext);
        hstRequest.setServletPath("/");
        componentManager.setServletContext(servletContext);
        synchronized (WEBAPP_CONTEXT_LOCK) {
            if (HippoWebappContextRegistry.get().getContext("/site") == null) {
                HippoWebappContextRegistry.get().register(new HippoWebappContext(HippoWebappContext.Type.SITE, servletContext));
            }
        }
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

    /**
     * Registers the HST model with the model registry.
     * <p>
     * This method gracefully handles the case where a model is already registered for the servlet context path,
     * which can occur when running multiple test methods in the same test class. The IllegalStateException
     * is caught and logged at debug level rather than failing the test.
     * </p>
     *
     * @since 5.0.1 - Added exception handling for already-registered models
     */
    protected void registerHstModel() {
        try {
            hstModelRegistry.registerHstModel(servletContext, componentManager, true);
        } catch (IllegalStateException e) {
            // Model already registered for this context path - this is expected when running
            // multiple test methods in the same test class. Log and continue.
            LOGGER.debug("HstModel already registered for contextPath '{}': {}",
                servletContext.getContextPath(), e.getMessage());
        }
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
     * Invokes the HST filter to process the current request and returns the response content.
     * <p>
     * This method properly manages the {@code RequestContextProvider} ThreadLocal, making it available
     * to JAX-RS resources during request processing. The ThreadLocal is guaranteed to be cleaned up
     * via a finally block to prevent memory leaks.
     * </p>
     * <p>
     * Exceptions during filter invocation are logged with full stack traces and re-thrown wrapped
     * in a {@code RuntimeException} for easier debugging.
     * </p>
     *
     * @return The response content as a string
     * @throws RuntimeException if filter invocation fails
     * @since 5.0.1 - Added RequestContextProvider ThreadLocal management and improved exception handling
     */
    protected String invokeFilter() {

        performValidation();
        //Invoke
        HstDelegateeFilterBean filter = componentManager.getComponent(HstFilter.class.getName());
        try {
            filter.doFilter(hstRequest, hstResponse, null);

            HstRequestContext requestContext = (HstRequestContext) hstRequest.getAttribute(
                ContainerConstants.HST_REQUEST_CONTEXT
            );

            // Ensure ThreadLocal is set for any post-filter processing
            if (requestContext != null) {
                setRequestContextProvider(requestContext);
            }

            String contentAsString = hstResponse.getContentAsString();
            LOGGER.info(contentAsString);
            //important! set the filter reset attribute to true for subsequent filter invocations
            hstRequest.setAttribute(HST_RESET_FILTER, true);

            return contentAsString;
        } catch (Exception e) {
            LOGGER.error("Exception during filter invocation", e);
            throw new RuntimeException("Filter invocation failed", e);
        } finally {
            // Always clean up ThreadLocal to prevent memory leaks
            clearRequestContextProvider();
        }
    }

    /**
     * Sets the HstRequestContext in the RequestContextProvider's ThreadLocal using reflection.
     * This is necessary because RequestContextProvider.set() is a private method.
     */
    private void setRequestContextProvider(HstRequestContext requestContext) {
        try {
            Method set = RequestContextProvider.class.getDeclaredMethod("set", HstRequestContext.class);
            set.setAccessible(true);
            set.invoke(null, requestContext);
            set.setAccessible(false);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Failed to set RequestContextProvider", e);
        }
    }

    /**
     * Clears the HstRequestContext from the RequestContextProvider's ThreadLocal using reflection.
     * This is necessary because RequestContextProvider.clear() is a private method.
     */
    private void clearRequestContextProvider() {
        try {
            Method clear = RequestContextProvider.class.getDeclaredMethod("clear");
            clear.setAccessible(true);
            clear.invoke(null);
            clear.setAccessible(false);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Failed to clear RequestContextProvider", e);
        }
    }

    protected void includeAdditionalSpringConfigurations() {
        ArrayList<String> configList = new ArrayList<>(Arrays.asList(this.componentManager.getConfigurationResources()));
        configList.addAll(contributeSpringConfigurationLocations());
        this.componentManager.setConfigurationResources(configList);
    }

    /**
     * This method can be called by sub-testclasses after node changes via JCR API. Invalidating the HST model is
     * necessary only if the hst configuration nodes are updated. In a brXM project normally this is done via JCR level
     * event handlers but within the test execution context we cannot have asynchronous events. We have to explicitly
     * force the HST to do a node lookup in the repository.
     */
    public void invalidateHstModel() {
        unregisterHstModel();
        registerHstModel();
    }

    /**
     * @return any additional spring xml locations to be included in the spring application context The returned value
     * should be a pattern
     */

    protected abstract List<String> contributeSpringConfigurationLocations();

    /**
     * @return any additional hst addon module location patterns
     */
    protected abstract List<String> contributeAddonModulePaths();

    /**
     * Perform validation before invoking the HST filter
     */
    protected abstract void performValidation();

    /**
     * @return absolute path of the root hst configuration node. E.g. "/hst:myproject"
     */

    protected abstract String contributeHstConfigurationRootPath();
}
