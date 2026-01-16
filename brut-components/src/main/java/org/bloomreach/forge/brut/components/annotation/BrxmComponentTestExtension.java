package org.bloomreach.forge.brut.components.annotation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class BrxmComponentTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmComponentTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.component.test.instance";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        BrxmComponentTest annotation = testClass.getAnnotation(BrxmComponentTest.class);
        if (annotation == null) {
            throw new IllegalStateException("@BrxmComponentTest annotation not found on " + testClass.getName());
        }

        ComponentTestConfig config = ComponentConfigResolver.resolve(annotation, testClass);
        DynamicComponentTest testInstance = new DynamicComponentTest(config);

        getStore(context).put(TEST_INSTANCE_KEY, testInstance);
        injectTestInstance(context, testInstance);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        DynamicComponentTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicComponentTest.class);
        if (testInstance == null) {
            throw new IllegalStateException("Component test instance not initialized. This should not happen.");
        }

        try {
            testInstance.setup();
        } catch (Exception e) {
            LOG.error("Failed to initialize component test infrastructure", e);
            throw new RuntimeException("Failed to initialize component test infrastructure", e);
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

    private void injectTestInstance(ExtensionContext context, DynamicComponentTest testInstance) throws Exception {
        Object testObject = context.getRequiredTestInstance();
        Class<?> testClass = testObject.getClass();

        Field targetField = null;
        for (Field field : testClass.getDeclaredFields()) {
            if (DynamicComponentTest.class.isAssignableFrom(field.getType())) {
                targetField = field;
                break;
            }
        }

        if (targetField == null) {
            throw new IllegalStateException(
                String.format("Test class %s must declare a field of type DynamicComponentTest. " +
                        "Example: private DynamicComponentTest brxm;", testClass.getName())
            );
        }

        targetField.setAccessible(true);
        targetField.set(testObject, testInstance);
        LOG.debug("Injected DynamicComponentTest instance into field: {}", targetField.getName());
    }
}
