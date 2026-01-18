package org.bloomreach.forge.brut.components.annotation;

import org.bloomreach.forge.brut.common.exception.BrutTestConfigurationException;
import org.bloomreach.forge.brut.common.junit.TestInstanceInjector;
import org.bloomreach.forge.brut.common.logging.TestConfigurationLogger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrxmComponentTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmComponentTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.component.test.instance";
    private static final String FRAMEWORK = "Component";
    private static final String ANNOTATION_PACKAGE = "org.bloomreach.forge.brut.components.annotation";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        BrxmComponentTest annotation = testClass.getAnnotation(BrxmComponentTest.class);
        if (annotation == null) {
            throw BrutTestConfigurationException.missingAnnotation(testClass, "BrxmComponentTest", ANNOTATION_PACKAGE);
        }

        ComponentTestConfig config = ComponentConfigResolver.resolve(annotation, testClass);
        logComponentConfig(testClass, config);

        DynamicComponentTest testInstance = new DynamicComponentTest(config);

        getStore(context).put(TEST_INSTANCE_KEY, testInstance);
        TestInstanceInjector.inject(context, testInstance, DynamicComponentTest.class, LOG);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        DynamicComponentTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicComponentTest.class);
        if (testInstance == null) {
            throw BrutTestConfigurationException.invalidState(
                    "Test instance not available in beforeEach",
                    "DynamicComponentTest should be initialized in beforeAll",
                    "Instance is null"
            );
        }

        try {
            testInstance.setup();
            TestConfigurationLogger.logSuccess(LOG, FRAMEWORK, context.getRequiredTestClass());
        } catch (Exception e) {
            TestConfigurationLogger.logFailure(LOG, FRAMEWORK, context.getRequiredTestClass(), e);
            ComponentTestConfig config = ComponentConfigResolver.resolve(
                    context.getRequiredTestClass().getAnnotation(BrxmComponentTest.class),
                    context.getRequiredTestClass()
            );
            String configDesc = String.format("  Bean patterns: %s%n  Test resource path: %s",
                    config.getAnnotatedClassesResourcePath(),
                    config.getTestResourcePath() != null ? config.getTestResourcePath() : "NONE");
            throw BrutTestConfigurationException.setupFailed("Component test setup", configDesc, e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        DynamicComponentTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicComponentTest.class);
        if (testInstance == null) {
            return;
        }
        try {
            testInstance.teardown();
        } catch (Exception e) {
            LOG.error("Failed to destroy component test infrastructure", e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        getStore(context).remove(TEST_INSTANCE_KEY);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(BrxmComponentTestExtension.class, context.getRequiredTestClass()));
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
}
