package org.bloomreach.forge.brut.components.annotation;

import org.bloomreach.forge.brut.common.exception.BrutTestConfigurationException;
import org.bloomreach.forge.brut.common.junit.NestedTestClassSupport;
import org.bloomreach.forge.brut.common.junit.TestInstanceInjector;
import org.bloomreach.forge.brut.common.logging.TestConfigurationLogger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class BrxmComponentTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmComponentTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.component.test.instance";
    private static final String TEST_CONFIG_KEY = "brxm.component.test.config";
    private static final String FRAMEWORK = "Component";
    private static final String ANNOTATION_PACKAGE = "org.bloomreach.forge.brut.components.annotation";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        // For nested classes, infrastructure is already initialized by the root class
        if (NestedTestClassSupport.isNestedTestClass(testClass)) {
            return;
        }

        BrxmComponentTest annotation = NestedTestClassSupport.findAnnotation(testClass, BrxmComponentTest.class);
        if (annotation == null) {
            throw BrutTestConfigurationException.missingAnnotation(testClass, "BrxmComponentTest", ANNOTATION_PACKAGE);
        }

        ComponentTestConfig config = ComponentConfigResolver.resolve(annotation, testClass);
        logComponentConfig(testClass, config);

        DynamicComponentTest testInstance = new DynamicComponentTest(config);

        getRootStore(context).put(TEST_INSTANCE_KEY, testInstance);
        getRootStore(context).put(TEST_CONFIG_KEY, config);
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

        ComponentTestConfig config = getRootStore(context).get(TEST_CONFIG_KEY, ComponentTestConfig.class);

        try {
            TestInstanceInjector.inject(context, testInstance, DynamicComponentTest.class, LOG);
            testInstance.setup();
            applyAnnotationConfig(testInstance, config);
            TestConfigurationLogger.logSuccess(LOG, FRAMEWORK, context.getRequiredTestClass());
        } catch (Exception e) {
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
        // Only clean up from the root class to avoid premature cleanup for nested classes
        if (!NestedTestClassSupport.isNestedTestClass(context.getRequiredTestClass())) {
            getRootStore(context).remove(TEST_INSTANCE_KEY);
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
