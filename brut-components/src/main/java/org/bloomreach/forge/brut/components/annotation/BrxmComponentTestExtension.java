package org.bloomreach.forge.brut.components.annotation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.stream.Stream;

public class BrxmComponentTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmComponentTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.component.test.instance";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        BrxmComponentTest annotation = testClass.getAnnotation(BrxmComponentTest.class);
        if (annotation == null) {
            throw ComponentConfigurationException.missingAnnotation(testClass, "BrxmComponentTest");
        }

        ComponentTestConfig config = ComponentConfigResolver.resolve(annotation, testClass);
        ComponentConfigurationSummary.log(LOG, testClass, config);

        DynamicComponentTest testInstance = new DynamicComponentTest(config);

        getStore(context).put(TEST_INSTANCE_KEY, testInstance);
        injectTestInstance(context, testInstance);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        DynamicComponentTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicComponentTest.class);
        if (testInstance == null) {
            throw ComponentConfigurationException.invalidState(
                    "Test instance not available in beforeEach",
                    "DynamicComponentTest should be initialized in beforeAll",
                    "Instance is null"
            );
        }

        try {
            testInstance.setup();
            ComponentConfigurationSummary.logSuccess(LOG, context.getRequiredTestClass());
        } catch (Exception e) {
            ComponentConfigurationSummary.logFailure(LOG, context.getRequiredTestClass(), e);
            ComponentTestConfig config = ComponentConfigResolver.resolve(
                    context.getRequiredTestClass().getAnnotation(BrxmComponentTest.class),
                    context.getRequiredTestClass()
            );
            throw ComponentConfigurationException.setupFailed("Component test setup", config, e);
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

        Field[] allFields = testClass.getDeclaredFields();
        Field targetField = null;
        for (Field field : allFields) {
            if (DynamicComponentTest.class.isAssignableFrom(field.getType())) {
                targetField = field;
                break;
            }
        }

        if (targetField == null) {
            String[] scannedFieldInfo = Stream.of(allFields)
                    .map(f -> f.getName() + " (" + f.getType().getSimpleName() + ")")
                    .toArray(String[]::new);
            throw ComponentConfigurationException.missingField(testClass, "DynamicComponentTest", scannedFieldInfo);
        }

        targetField.setAccessible(true);
        targetField.set(testObject, testInstance);
        LOG.debug("Injected DynamicComponentTest instance into field: {}", targetField.getName());
    }
}
