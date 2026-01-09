package org.bloomreach.forge.brut.resources;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.springframework.mock.web.DelegatingServletInputStream;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for testing JAX-RS resources with the HST container.
 * <p>
 * This class provides thread-safe initialization of the HST platform and component manager.
 * For tests with multiple test methods using {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)},
 * call {@link #setupForNewRequest()} in a {@code @BeforeEach} method to reset state between tests.
 * </p>
 * <p>
 * <b>Thread Safety:</b> This class uses synchronized blocks to prevent race conditions during
 * component manager initialization, making it safe for parallel test execution.
 * </p>
 *
 * @see AbstractResourceTest
 * @since 5.0.1
 */
public abstract class AbstractJaxrsTest extends AbstractResourceTest {

    private static final int DEFAULT_BYTE_ARRAY_INPUT_STREAM_SIZE = 1024;
    private static final Object INITIALIZATION_LOCK = new Object();
    private static boolean componentManagerInitialized = false;

    /**
     * Initializes the HST platform and component manager. Call this in a {@code @BeforeAll} method.
     * <p>
     * This method performs one-time initialization of the Spring component manager and HST platform
     * in a thread-safe manner. Subsequent calls will skip initialization if already done.
     * </p>
     */
    public void init() {
        setupHstRequest();
        setupServletContext();

        synchronized (INITIALIZATION_LOCK) {
            if (!componentManagerInitialized) {
                setupComponentManager();
                setupHstPlatform();
                componentManagerInitialized = true;
            }
        }

        setupForNewRequest();
    }

    /**
     * Resets per-request state: HST model registration and response object.
     * <p>
     * Call this in a {@code @BeforeEach} method when running multiple test methods in the same test class
     * with {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)}. This ensures each test starts with
     * clean HST model and response state while avoiding expensive component manager re-initialization.
     * </p>
     * <p>
     * Subclasses can override this method to add additional per-request setup (e.g., setting request headers),
     * but must call {@code super.setupForNewRequest()} to ensure proper state reset.
     * </p>
     *
     * @since 5.0.1
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
        synchronized (INITIALIZATION_LOCK) {
            componentManagerInitialized = false;
        }
    }
}
