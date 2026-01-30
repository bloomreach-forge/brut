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

import java.lang.annotation.Annotation;

/**
 * Base JUnit 5 extension that manages lifecycle for dynamic test classes.
 * Handles initialization, request setup, and cleanup automatically.
 *
 * @param <T> the dynamic test type (e.g., DynamicJaxrsTest or DynamicPageModelTest)
 * @param <A> the annotation type (e.g., BrxmJaxrsTest or BrxmPageModelTest)
 */
abstract class BaseDynamicTestExtension<T extends DynamicTest, A extends Annotation>
        implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, ParameterResolver {

    private static final String ANNOTATION_PACKAGE = "org.bloomreach.forge.brut.resources.annotation";

    protected abstract Logger getLogger();
    protected abstract String getTestInstanceKey();
    protected abstract String getFrameworkName();
    protected abstract Class<A> getAnnotationClass();
    protected abstract Class<T> getTestInstanceClass();
    protected abstract T createTestInstance(TestConfig config);
    protected abstract TestConfig resolveConfig(A annotation, Class<?> testClass);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        if (NestedTestClassSupport.isNestedTestClass(testClass)) {
            return;
        }

        A annotation = NestedTestClassSupport.findAnnotation(testClass, getAnnotationClass());
        if (annotation == null) {
            throw BrutTestConfigurationException.missingAnnotation(
                    testClass, getAnnotationClass().getSimpleName(), ANNOTATION_PACKAGE);
        }

        TestConfig config = resolveConfig(annotation, testClass);
        logTestConfig(testClass, config);

        T testInstance = createTestInstance(config);

        try {
            testInstance.init();
            TestConfigurationLogger.logSuccess(getLogger(), getFrameworkName(), testClass);
        } catch (Exception e) {
            TestConfigurationLogger.logFailure(getLogger(), getFrameworkName(), testClass, e);
            throw BrutTestConfigurationException.bootstrapFailed(
                    getFrameworkName() + " test initialization",
                    config.getBeanPatterns(), config.getSpringConfigs(), config.getHstRoot(), e);
        }

        getRootStore(context).put(getTestInstanceKey(), testInstance);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        T testInstance = getRootStore(context).get(getTestInstanceKey(), getTestInstanceClass());

        if (testInstance == null) {
            throw BrutTestConfigurationException.invalidState(
                    "Test instance not available in beforeEach",
                    getTestInstanceClass().getSimpleName() + " should be initialized in beforeAll",
                    "Instance is null");
        }

        TestInstanceInjector.inject(context, testInstance, getTestInstanceClass(), getLogger());
        testInstance.setupForNewRequest();
        testInstance.getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        testInstance.getHstRequest().setMethod(HttpMethod.GET);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (NestedTestClassSupport.isNestedTestClass(context.getRequiredTestClass())) {
            return;
        }

        T testInstance = getRootStore(context).get(getTestInstanceKey(), getTestInstanceClass());

        if (testInstance != null) {
            getLogger().info("Destroying {} test infrastructure for {}",
                    getFrameworkName(), context.getRequiredTestClass().getSimpleName());
            try {
                testInstance.destroy();
            } catch (Exception e) {
                getLogger().error("Failed to destroy {} test infrastructure", getFrameworkName(), e);
            } finally {
                getRootStore(context).remove(getTestInstanceKey());
            }
        }
    }

    private ExtensionContext.Store getRootStore(ExtensionContext context) {
        Class<?> rootClass = NestedTestClassSupport.getRootTestClass(context.getRequiredTestClass());
        return context.getRoot().getStore(ExtensionContext.Namespace.create(getClass(), rootClass));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == getTestInstanceClass();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getRootStore(extensionContext).get(getTestInstanceKey(), getTestInstanceClass());
    }

    private void logTestConfig(Class<?> testClass, TestConfig config) {
        TestConfigurationLogger.logConfiguration(getLogger(), testClass, getFrameworkName(), log -> {
            TestConfigurationLogger.logBeanPatterns(log, config.getBeanPatterns());
            TestConfigurationLogger.logSpringConfigs(log, config.getSpringConfigs());
            TestConfigurationLogger.logHstRoot(log, config.getHstRoot());
            TestConfigurationLogger.logModules(log, "Repository Data Modules", config.getRepositoryDataModules());
            TestConfigurationLogger.logModules(log, "Addon Modules", config.getAddonModules());
        });
    }
}
