package org.bloomreach.forge.brut.resources;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.DelegatingServletInputStream;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Abstract base class for testing JAX-RS resources with the HST container.
 * <p>
 * Supports both JUnit 4 {@code @Before} and JUnit 5 {@code @BeforeAll} patterns.
 * Component manager is shared across test instances for performance while maintaining test isolation.
 * </p>
 *
 * @see AbstractResourceTest
 * @since 5.0.1
 */
public abstract class AbstractJaxrsTest extends AbstractResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJaxrsTest.class);
    private static final int DEFAULT_BYTE_ARRAY_INPUT_STREAM_SIZE = 1024;
    private static final ReentrantLock initializationLock = new ReentrantLock();
    private static final AtomicBoolean componentManagerInitialized = new AtomicBoolean(false);

    public void init() {
        setupHstRequest();
        setupServletContext();

        if (isFirstInitialization()) {
            performFirstTimeInitialization();
        } else {
            reuseSharedInstances();
        }

        setupForNewRequest();
    }

    private boolean isFirstInitialization() {
        return !componentManagerInitialized.get();
    }

    private void performFirstTimeInitialization() {
        initializationLock.lock();
        try {
            if (isFirstInitialization()) {
                setupComponentManager();
                setupHstPlatform();
                storeSharedReferences();
                componentManagerInitialized.set(true);
            }
        } finally {
            initializationLock.unlock();
        }
    }

    private void storeSharedReferences() {
        sharedComponentManager = componentManager;
        sharedHstModelRegistry = hstModelRegistry;
        sharedPlatformServices = platformServices;
        sharedPlatformModelAvailableService = platformModelAvailableService;
    }

    private void reuseSharedInstances() {
        componentManager = sharedComponentManager;
        hstModelRegistry = sharedHstModelRegistry;
        platformServices = sharedPlatformServices;
        platformModelAvailableService = sharedPlatformModelAvailableService;
        restoreRepositoryReference();
    }

    private void restoreRepositoryReference() {
        if (hstModelRegistry != null && componentManager != null) {
            hstModelRegistry.setRepository(componentManager.getComponent(javax.jcr.Repository.class));
        }
    }

    /**
     * Resets per-request state. Call from {@code @BeforeEach} for JUnit 5 tests with multiple methods.
     * Subclasses can override to add custom setup but must call {@code super.setupForNewRequest()}.
     */
    protected void setupForNewRequest() {
        unregisterHstModel();
        registerHstModel();
        setupHstResponse();
    }

    @Override
    public void performValidation() {
        if (hstRequest.getRequestURI() == null || "".equals(hstRequest.getRequestURI())) {
            throw new IllegalStateException("Request URI was missing in hstRequest");
        }
        if (hstRequest.getMethod() == null) {
            throw new IllegalStateException(("Method name was missing hstRequest"));
        }
    }

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
        includeAdditionalSpringConfigurations();
        includeAdditionalAddonModules();
        componentManager.initialize();
        HstServices.setComponentManager(componentManager);
        ContainerConfigurationImpl containerConfiguration = componentManager.getComponent("containerConfiguration");
        containerConfiguration.setProperty("hst.configuration.rootPath", contributeHstConfigurationRootPath());
        HstManagerImpl hstManager = (HstManagerImpl) componentManager.getComponent(HstManager.class);
        hstManager.setServletContext(hstRequest.getServletContext());
    }

    private void includeAdditionalAddonModules() {
        if (contributeAddonModulePaths() != null) {
            List<ModuleDefinition> moduleDefinitions = contributeAddonModulePaths().stream()
                    .map(Utils::loadAddonModule)
                    .collect(Collectors.toList());
            this.componentManager.setAddonModuleDefinitions(moduleDefinitions);
        }
    }

    protected int getServletInputStreamSize() {
        return DEFAULT_BYTE_ARRAY_INPUT_STREAM_SIZE;
    }

    @Override
    public void destroy() {
        super.destroy();
        initializationLock.lock();
        try {
            componentManagerInitialized.set(false);
        } finally {
            initializationLock.unlock();
        }
    }
}
