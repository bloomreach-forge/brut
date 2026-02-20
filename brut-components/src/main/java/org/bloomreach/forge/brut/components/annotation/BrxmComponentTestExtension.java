package org.bloomreach.forge.brut.components.annotation;

import org.bloomreach.forge.brut.common.exception.BrutTestConfigurationException;
import org.bloomreach.forge.brut.common.junit.NestedTestClassSupport;
import org.bloomreach.forge.brut.common.junit.TestInstanceInjector;
import org.bloomreach.forge.brut.common.logging.TestConfigurationLogger;
import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.bloomreach.forge.brut.common.repository.JcrTransactionSupport;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Set;

public class BrxmComponentTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmComponentTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.component.test.instance";
    private static final String TEST_CONFIG_KEY = "brxm.component.test.config";
    private static final String TX_SUPPORT_KEY = "brxm.component.test.tx.support";
    private static final String SHARED_REPO_KEY_PREFIX = "brxm.shared.repo.";
    private static final String FRAMEWORK = "Component";
    private static final String ANNOTATION_PACKAGE = "org.bloomreach.forge.brut.components.annotation";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        if (NestedTestClassSupport.isNestedTestClass(testClass)) {
            return;
        }

        BrxmComponentTest annotation = NestedTestClassSupport.findAnnotation(testClass, BrxmComponentTest.class);
        if (annotation == null) {
            throw BrutTestConfigurationException.missingAnnotation(testClass, "BrxmComponentTest", ANNOTATION_PACKAGE);
        }

        ComponentTestConfig config = ComponentConfigResolver.resolve(annotation, testClass);
        logComponentConfig(testClass, config);

        // Look up or bootstrap the shared repository for this config fingerprint.
        // Using the JUnit 5 root store means the SharedRepositoryEntry.close() is called
        // once at the end of the entire test suite, closing Jackrabbit exactly once per
        // unique config regardless of how many test classes share the same fingerprint.
        String sharedKey = SHARED_REPO_KEY_PREFIX + config.computeFingerprint();
        ExtensionContext.Store globalStore = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        SharedRepositoryEntry entry = globalStore.getOrComputeIfAbsent(
            sharedKey,
            k -> bootstrapSharedRepository(config),
            SharedRepositoryEntry.class
        );

        DynamicComponentTest testInstance = new DynamicComponentTest(config);

        try {
            testInstance.setRepository(entry.get());
            testInstance.setup();
            applyAnnotationConfig(testInstance, config);
        } catch (Exception e) {
            TestConfigurationLogger.logFailure(LOG, FRAMEWORK, testClass, e);
            String configDesc = String.format("  Bean patterns: %s%n  Test resource path: %s",
                    config.getAnnotatedClassesResourcePath(),
                    config.getTestResourcePath() != null ? config.getTestResourcePath() : "NONE");
            throw BrutTestConfigurationException.setupFailed("Component test setup", configDesc, e);
        }

        getRootStore(context).put(TEST_INSTANCE_KEY, testInstance);
        getRootStore(context).put(TEST_CONFIG_KEY, config);
        getRootStore(context).put(TX_SUPPORT_KEY, new JcrTransactionSupport());
    }

    /**
     * Creates and bootstraps a new {@link BrxmTestingRepository}, records the skeleton import
     * via {@link BrxmTestingRepository#recordInitialization}, then closes the bootstrap session
     * so subsequent test classes can open their own sessions on the shared repository.
     * <p>
     * The returned entry is stored in JUnit 5's root store; its {@code close()} method shuts
     * down the repository exactly once at the very end of the test suite.
     */
    private static SharedRepositoryEntry bootstrapSharedRepository(ComponentTestConfig config) {
        DynamicComponentTest bootstrap = new DynamicComponentTest(config);
        try {
            bootstrap.setup();
            BrxmTestingRepository repo = bootstrap.getRepository();
            repo.setManaged(true);
            Session bootstrapSession = bootstrap.getSession();
            if (bootstrapSession != null && bootstrapSession.isLive()) {
                bootstrapSession.logout();
            }
            return new SharedRepositoryEntry(repo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap shared repository for config: "
                + config.computeFingerprint(), e);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        DynamicComponentTest testInstance = getRootStore(context).get(TEST_INSTANCE_KEY, DynamicComponentTest.class);
        if (testInstance == null) {
            throw BrutTestConfigurationException.invalidState(
                    "Test instance not available in beforeEach",
                    "DynamicComponentTest should be initialized in beforeAll",
                    "Instance is null"
            );
        }

        JcrTransactionSupport txSupport = getRootStore(context).get(TX_SUPPORT_KEY, JcrTransactionSupport.class);

        try {
            txSupport.begin(testInstance.getSession());
            testInstance.setup();
            TestInstanceInjector.inject(context, testInstance, DynamicComponentTest.class, LOG);
            TestConfigurationLogger.logSuccess(LOG, FRAMEWORK, context.getRequiredTestClass());
        } catch (Exception e) {
            ComponentTestConfig config = getRootStore(context).get(TEST_CONFIG_KEY, ComponentTestConfig.class);
            TestConfigurationLogger.logFailure(LOG, FRAMEWORK, context.getRequiredTestClass(), e);
            String configDesc = String.format("  Bean patterns: %s%n  Test resource path: %s",
                    config.getAnnotatedClassesResourcePath(),
                    config.getTestResourcePath() != null ? config.getTestResourcePath() : "NONE");
            throw BrutTestConfigurationException.setupFailed("Component test setup", configDesc, e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        DynamicComponentTest testInstance = getRootStore(context).get(TEST_INSTANCE_KEY, DynamicComponentTest.class);
        JcrTransactionSupport txSupport = getRootStore(context).get(TX_SUPPORT_KEY, JcrTransactionSupport.class);

        if (testInstance != null) {
            try {
                testInstance.teardown();
            } catch (Exception e) {
                LOG.error("Failed to teardown component test infrastructure", e);
            }
        }

        if (txSupport != null) {
            try {
                txSupport.rollback();
            } catch (Exception e) {
                LOG.error("Failed to rollback XA transaction", e);
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (!NestedTestClassSupport.isNestedTestClass(context.getRequiredTestClass())) {
            // The repository lifecycle is managed by SharedRepositoryEntry (a CloseableResource
            // stored in the JUnit 5 root store). JUnit closes it once at the end of the suite,
            // so we must not forceClose() here — doing so would break any subsequent test class
            // that shares the same repository instance.
            getRootStore(context).remove(TEST_INSTANCE_KEY);
            getRootStore(context).remove(TX_SUPPORT_KEY);
        }
    }

    private ExtensionContext.Store getRootStore(ExtensionContext context) {
        Class<?> rootClass = NestedTestClassSupport.getRootTestClass(context.getRequiredTestClass());
        return context.getRoot().getStore(ExtensionContext.Namespace.create(BrxmComponentTestExtension.class, rootClass));
    }

    private void logComponentConfig(Class<?> testClass, ComponentTestConfig config) {
        TestConfigurationLogger.logConfiguration(LOG, testClass, FRAMEWORK, log -> {
            log.info("Bean Patterns:");
            log.info("  {}", config.getAnnotatedClassesResourcePath());
            if (config.getTestResourcePath() != null) {
                log.info("Test Resource Path: {}", config.getTestResourcePath());
            } else {
                log.info("Test Resource Path: NONE");
            }
        });
    }

    private void applyAnnotationConfig(DynamicComponentTest testInstance, ComponentTestConfig config) {
        registerNodeTypes(testInstance, config);
        if (config.hasContent() && config.hasContentRoot()) {
            testInstance.importYaml(config.getContent(), config.getContentRoot(),
                    "hippostd:folder", config.getTestClass());
            testInstance.recalculateRepositoryPaths();
            testInstance.setSiteContentBasePath(config.getContentRoot());
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == DynamicComponentTest.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getRootStore(extensionContext).get(TEST_INSTANCE_KEY, DynamicComponentTest.class);
    }

    /**
     * Wraps a shared {@link BrxmTestingRepository} as a JUnit 5 {@code CloseableResource}.
     * Stored in the root-scoped {@link ExtensionContext.Store}; JUnit calls {@link #close()}
     * exactly once at the end of the full test suite, shutting down the shared Jackrabbit
     * instance and cleaning up its temporary directory.
     */
    private static final class SharedRepositoryEntry
        implements ExtensionContext.Store.CloseableResource {

        private final BrxmTestingRepository repository;

        SharedRepositoryEntry(BrxmTestingRepository repository) {
            this.repository = repository;
        }

        BrxmTestingRepository get() {
            return repository;
        }

        @Override
        public void close() {
            repository.forceClose();
        }
    }

    private void registerNodeTypes(DynamicComponentTest testInstance, ComponentTestConfig config) {
        String[] explicitTypes = config.getNodeTypes();

        if (explicitTypes.length == 0) {
            Set<String> scannedTypes = NodeTypeScanner.scanForNodeTypes(config.getAnnotatedClassesResourcePath());
            if (!scannedTypes.isEmpty()) {
                LOG.debug("Auto-registering {} node type(s) from @Node annotations", scannedTypes.size());
                testInstance.registerNodeTypes(scannedTypes.toArray(new String[0]));
            }
            return;
        }

        for (String nodeTypeDef : explicitTypes) {
            String trimmed = nodeTypeDef.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int extendsIdx = trimmed.toLowerCase().indexOf(" extends ");
            if (extendsIdx > 0) {
                String nodeType = trimmed.substring(0, extendsIdx).trim();
                String superType = trimmed.substring(extendsIdx + 9).trim();
                testInstance.registerNodeTypeWithSupertype(nodeType, superType);
            } else {
                testInstance.registerNodeTypes(trimmed);
            }
        }
    }
}
