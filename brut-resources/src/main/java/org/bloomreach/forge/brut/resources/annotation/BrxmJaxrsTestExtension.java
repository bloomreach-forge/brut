/*
 * Copyright 2024 Bloomreach, Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.brut.resources.annotation;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.bloomreach.forge.brut.common.exception.BrutTestConfigurationException;
import org.bloomreach.forge.brut.common.junit.NestedTestClassSupport;
import org.bloomreach.forge.brut.common.junit.TestInstanceInjector;
import org.bloomreach.forge.brut.common.logging.TestConfigurationLogger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that manages lifecycle for @BrxmJaxrsTest annotated tests.
 * Handles initialization, request setup, and cleanup automatically.
 */
public class BrxmJaxrsTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmJaxrsTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.jaxrs.test.instance";
    private static final String FRAMEWORK = "JAX-RS";
    private static final String ANNOTATION_PACKAGE = "org.bloomreach.forge.brut.resources.annotation";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        // For nested classes, infrastructure is already initialized by the root class
        if (NestedTestClassSupport.isNestedTestClass(testClass)) {
            return;
        }

        BrxmJaxrsTest annotation = NestedTestClassSupport.findAnnotation(testClass, BrxmJaxrsTest.class);
        if (annotation == null) {
            throw BrutTestConfigurationException.missingAnnotation(testClass, "BrxmJaxrsTest", ANNOTATION_PACKAGE);
        }

        TestConfig config = ConventionBasedConfigResolver.resolve(annotation, testClass);
        logTestConfig(testClass, config);

        DynamicJaxrsTest testInstance = new DynamicJaxrsTest(config);

        try {
            testInstance.init();
            TestConfigurationLogger.logSuccess(LOG, FRAMEWORK, testClass);
        } catch (Exception e) {
            TestConfigurationLogger.logFailure(LOG, FRAMEWORK, testClass, e);
            throw BrutTestConfigurationException.bootstrapFailed(FRAMEWORK + " test initialization",
                    config.getBeanPatterns(), config.getSpringConfigs(), config.getHstRoot(), e);
        }

        getRootStore(context).put(TEST_INSTANCE_KEY, testInstance);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        DynamicJaxrsTest testInstance = getRootStore(context).get(TEST_INSTANCE_KEY, DynamicJaxrsTest.class);

        if (testInstance == null) {
            throw BrutTestConfigurationException.invalidState(
                    "Test instance not available in beforeEach",
                    "DynamicJaxrsTest should be initialized in beforeAll",
                    "Instance is null"
                    );
        }

        TestInstanceInjector.inject(context, testInstance, DynamicJaxrsTest.class, LOG);
        testInstance.setupForNewRequest();
        testInstance.getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        testInstance.getHstRequest().setMethod(HttpMethod.GET);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Only clean up from the root class to avoid premature cleanup for nested classes
        if (NestedTestClassSupport.isNestedTestClass(context.getRequiredTestClass())) {
            return;
        }

        DynamicJaxrsTest testInstance = getRootStore(context).get(TEST_INSTANCE_KEY, DynamicJaxrsTest.class);

        if (testInstance != null) {
            LOG.info("Destroying JAX-RS test infrastructure for {}", context.getRequiredTestClass().getSimpleName());
            try {
                testInstance.destroy();
            } catch (Exception e) {
                LOG.error("Failed to destroy JAX-RS test infrastructure", e);
            } finally {
                getRootStore(context).remove(TEST_INSTANCE_KEY);
            }
        }
    }

    private ExtensionContext.Store getRootStore(ExtensionContext context) {
        Class<?> rootClass = NestedTestClassSupport.getRootTestClass(context.getRequiredTestClass());
        return context.getRoot().getStore(ExtensionContext.Namespace.create(BrxmJaxrsTestExtension.class, rootClass));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == DynamicJaxrsTest.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getRootStore(extensionContext).get(TEST_INSTANCE_KEY, DynamicJaxrsTest.class);
    }

    private void logTestConfig(Class<?> testClass, TestConfig config) {
        TestConfigurationLogger.logConfiguration(LOG, testClass, FRAMEWORK, log -> {
            TestConfigurationLogger.logBeanPatterns(log, config.getBeanPatterns());
            TestConfigurationLogger.logSpringConfigs(log, config.getSpringConfigs());
            TestConfigurationLogger.logHstRoot(log, config.getHstRoot());
            TestConfigurationLogger.logModules(log, "Repository Data Modules", config.getRepositoryDataModules());
            TestConfigurationLogger.logModules(log, "Addon Modules", config.getAddonModules());
        });
    }
}
