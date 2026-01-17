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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.stream.Stream;

/**
 * JUnit 5 extension that manages lifecycle for @BrxmPageModelTest annotated tests.
 * Handles initialization, request setup, and cleanup automatically.
 */
public class BrxmPageModelTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmPageModelTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.test.instance";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        // Get annotation
        BrxmPageModelTest annotation = testClass.getAnnotation(BrxmPageModelTest.class);
        if (annotation == null) {
            throw BrutConfigurationException.missingAnnotation(testClass, "BrxmPageModelTest");
        }

        // Resolve configuration
        TestConfig config = ConventionBasedConfigResolver.resolve(annotation, testClass);

        // Log configuration summary
        ConfigurationSummary.log(LOG, testClass, "PageModel", config);

        // Create and initialize DynamicPageModelTest
        DynamicPageModelTest testInstance = new DynamicPageModelTest(config);

        try {
            testInstance.init();
            ConfigurationSummary.logSuccess(LOG, "PageModel", testClass);
        } catch (Exception e) {
            ConfigurationSummary.logFailure(LOG, "PageModel", testClass, e);
            throw BrutConfigurationException.bootstrapFailed("PageModel test initialization", config, e);
        }

        // Store instance for access in beforeEach and afterAll
        getStore(context).put(TEST_INSTANCE_KEY, testInstance);

        // Inject into test class field
        injectTestInstance(context, testInstance);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        DynamicPageModelTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicPageModelTest.class);

        if (testInstance == null) {
            throw BrutConfigurationException.invalidState(
                    "Test instance not available in beforeEach",
                    "DynamicPageModelTest should be initialized in beforeAll",
                    "Instance is null"
            );
        }

        // Setup request with defaults for each test method
        testInstance.setupForNewRequest();
        testInstance.getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        testInstance.getHstRequest().setMethod(HttpMethod.GET);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        DynamicPageModelTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicPageModelTest.class);

        if (testInstance != null) {
            LOG.info("Destroying PageModel test infrastructure for {}", context.getRequiredTestClass().getSimpleName());
            try {
                testInstance.destroy();
            } catch (Exception e) {
                LOG.error("Failed to destroy PageModel test infrastructure", e);
            } finally {
                getStore(context).remove(TEST_INSTANCE_KEY);
            }
        }
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(BrxmPageModelTestExtension.class, context.getRequiredTestClass()));
    }

    private void injectTestInstance(ExtensionContext context, DynamicPageModelTest testInstance) throws Exception {
        Object testObject = context.getRequiredTestInstance();
        Class<?> testClass = testObject.getClass();

        // Look for field of type DynamicPageModelTest
        Field[] allFields = testClass.getDeclaredFields();
        Field targetField = null;
        for (Field field : allFields) {
            if (DynamicPageModelTest.class.isAssignableFrom(field.getType())) {
                targetField = field;
                break;
            }
        }

        if (targetField == null) {
            String[] scannedFieldInfo = Stream.of(allFields)
                    .map(f -> f.getName() + " (" + f.getType().getSimpleName() + ")")
                    .toArray(String[]::new);
            throw BrutConfigurationException.missingField(testClass, "DynamicPageModelTest", scannedFieldInfo);
        }

        // Inject
        targetField.setAccessible(true);
        targetField.set(testObject, testInstance);
        LOG.debug("Injected DynamicPageModelTest instance into field: {}", targetField.getName());
    }
}
