package org.bloomreach.forge.brut.resources;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceTest.class);

    private static final String HST_RESET_FILTER = "org.hippoecm.hst.container.HstFilter.reset";

    private static final ReentrantLock webappContextLock = new ReentrantLock();

    // Cached once at class-load: avoids repeated getDeclaredMethod() on every invokeFilter() call
    // and makes module-system failures deterministic at startup.
    private static final Method REQUEST_CONTEXT_SET;
    private static final Method REQUEST_CONTEXT_CLEAR;

    static {
        try {
            REQUEST_CONTEXT_SET = RequestContextProvider.class.getDeclaredMethod("set", HstRequestContext.class);
            REQUEST_CONTEXT_SET.setAccessible(true);
            REQUEST_CONTEXT_CLEAR = RequestContextProvider.class.getDeclaredMethod("clear");
            REQUEST_CONTEXT_CLEAR.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Shared across test instances to support JUnit 4 @Before pattern.
    // volatile ensures the writes in storeSharedReferences() (inside the ReentrantLock) are
    // visible to reads in reuseSharedInstances() (outside the lock) without relying solely on
    // the AtomicBoolean volatile-chain for the happens-before guarantee.
    protected static volatile SpringComponentManager sharedComponentManager;
    protected static volatile HstModelRegistryImpl sharedHstModelRegistry;
    protected static volatile PlatformServicesImpl sharedPlatformServices;
    protected static volatile PlatformModelAvailableService sharedPlatformModelAvailableService;

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
        webappContextLock.lock();
        try {
            if (HippoWebappContextRegistry.get().getContext("/site") == null) {
                HippoWebappContextRegistry.get().register(new HippoWebappContext(HippoWebappContext.Type.SITE, servletContext));
            }
        } finally {
            webappContextLock.unlock();
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

    protected void registerHstModel() {
        if (hstModelRegistry == null) {
            LOGGER.warn("Cannot register HST model: hstModelRegistry is null. Ensure setupHstPlatform() has been called.");
            return;
        }
        try {
            hstModelRegistry.registerHstModel(servletContext, componentManager, true);
        } catch (IllegalStateException e) {
            // Expected when running multiple test methods in same class
            LOGGER.debug("HstModel already registered for contextPath '{}': {}",
                servletContext.getContextPath(), e.getMessage());
        }
    }

    protected void unregisterHstModel() {
        if (hstModelRegistry != null) {
            hstModelRegistry.unregisterHstModel(servletContext);
        }
    }


    public void destroy() {
        HippoServiceRegistry.unregister(platformModelAvailableService, PlatformModelAvailableService.class);

        HstServices.setComponentManager(null);

        HstSiteMapItemHandlerFactoriesImpl hstSiteMapItemHandlerFactories = (HstSiteMapItemHandlerFactoriesImpl) getComponentManager().getComponent(HstSiteMapItemHandlerFactories.class);
        hstSiteMapItemHandlerFactories.destroy();
        hstSiteMapItemHandlerFactories.unregister(servletContext.getContextPath());

        unregisterHstModel();
        if (hstModelRegistry != null) {
            hstModelRegistry.destroy();
            hstModelRegistry.setRepository(null);
        }

        if (platformServices != null) {
            platformServices.destroy();
            platformServices.setPreviewDecorator(null);
            platformServices.setHstModelRegistry(null);
        }
    }

    /**
     * Invokes the HST filter and returns response content.
     * Sets RequestContextProvider ThreadLocal for JAX-RS resources to access.
     */
    public String invokeFilter() {
        setupHstResponse();
        performValidation();

        HstDelegateeFilterBean filter = componentManager.getComponent(HstFilter.class.getName());
        if (filter == null) {
            throw new IllegalStateException(
                "HstDelegateeFilterBean is null. Ensure init() was called and setupComponentManager() completed successfully. " +
                "Component manager present: " + (componentManager != null)
            );
        }

        try {
            filter.doFilter(hstRequest, hstResponse, null);

            HstRequestContext requestContext = (HstRequestContext) hstRequest.getAttribute(
                ContainerConstants.HST_REQUEST_CONTEXT
            );

            if (requestContext != null) {
                setRequestContextProvider(requestContext);
            }

            String contentAsString = hstResponse.getContentAsString();
            LOGGER.info(contentAsString);
            hstRequest.setAttribute(HST_RESET_FILTER, true);

            return contentAsString;
        } catch (Exception e) {
            LOGGER.error("Exception during filter invocation", e);
            throw new RuntimeException("Filter invocation failed", e);
        } finally {
            clearRequestContextProvider();
        }
    }

    /**
     * Sets the HstRequestContext in the RequestContextProvider's ThreadLocal using reflection.
     * This is necessary because RequestContextProvider.set() is a private method.
     */
    private void setRequestContextProvider(HstRequestContext requestContext) {
        try {
            REQUEST_CONTEXT_SET.invoke(null, requestContext);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Failed to set RequestContextProvider", e);
        }
    }

    /**
     * Clears the HstRequestContext from the RequestContextProvider's ThreadLocal using reflection.
     * This is necessary because RequestContextProvider.clear() is a private method.
     */
    private void clearRequestContextProvider() {
        try {
            REQUEST_CONTEXT_CLEAR.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Failed to clear RequestContextProvider", e);
        }
    }

    protected int getResponseStatus() {
        if (hstResponse == null) {
            return 200;
        }
        try {
            Method getStatus = hstResponse.getClass().getMethod("getStatus");
            return (int) getStatus.invoke(hstResponse);
        } catch (Exception e) {
            return 200;
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

    protected String resolveExistingHstRoot(String configuredRoot) {
        String normalizedConfigured = normalizeHstRootPath(configuredRoot);
        String projectNamespace = ProjectDiscovery.resolveProjectNamespace(Paths.get(System.getProperty("user.dir")));
        String fallbackRoot = normalizedConfigured != null
            ? normalizedConfigured
            : normalizeHstRootPath(projectNamespace != null ? "/hst:" + projectNamespace : null);
        if (fallbackRoot == null) {
            fallbackRoot = configuredRoot;
        }

        if (componentManager == null) {
            return fallbackRoot;
        }
        Repository repository = componentManager.getComponent(Repository.class);
        if (repository == null) {
            return fallbackRoot;
        }

        Set<String> candidates = new LinkedHashSet<>();
        if (normalizedConfigured != null) {
            candidates.add(normalizedConfigured);
        }
        String projectRoot = normalizeHstRootPath(projectNamespace != null ? "/hst:" + projectNamespace : null);
        if (projectRoot != null) {
            candidates.add(projectRoot);
        }
        candidates.add("/hst:hst");

        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            for (String candidate : candidates) {
                if (candidate != null && hasHstRootStructure(session, candidate)) {
                    if (normalizedConfigured != null && !candidate.equals(normalizedConfigured)) {
                        LOGGER.warn("HST root '{}' not found; using '{}' from repository", normalizedConfigured, candidate);
                    }
                    return candidate;
                }
            }
        } catch (RepositoryException e) {
            LOGGER.warn("Unable to validate HST root; using '{}'", fallbackRoot, e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }

        return fallbackRoot;
    }

    private boolean hasHstRootStructure(Session session, String hstRoot) throws RepositoryException {
        if (session == null || hstRoot == null || hstRoot.isBlank()) {
            return false;
        }
        if (!session.nodeExists(hstRoot)) {
            return false;
        }
        return session.nodeExists(hstRoot + "/hst:configurations")
            || session.nodeExists(hstRoot + "/hst:sites")
            || session.nodeExists(hstRoot + "/hst:hosts");
    }

    private String normalizeHstRootPath(String hstRoot) {
        if (hstRoot == null) {
            return null;
        }
        String trimmed = hstRoot.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("/")) {
            return trimmed;
        }
        return "/" + trimmed;
    }
}
