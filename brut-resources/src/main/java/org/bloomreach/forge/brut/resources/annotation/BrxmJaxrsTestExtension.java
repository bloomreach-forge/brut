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

/**
 * JUnit 5 extension that manages lifecycle for @BrxmJaxrsTest annotated tests.
 * Handles initialization, request setup, and cleanup automatically.
 */
public class BrxmJaxrsTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmJaxrsTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.jaxrs.test.instance";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();

        // Get annotation
        BrxmJaxrsTest annotation = testClass.getAnnotation(BrxmJaxrsTest.class);
        if (annotation == null) {
            throw new IllegalStateException("@BrxmJaxrsTest annotation not found on " + testClass.getName());
        }

        // Resolve configuration
        TestConfig config = ConventionBasedConfigResolver.resolve(annotation, testClass);

        // Create and initialize DynamicJaxrsTest
        DynamicJaxrsTest testInstance = new DynamicJaxrsTest(config);

        LOG.info("Initializing JAX-RS test infrastructure for {}", testClass.getSimpleName());
        try {
            testInstance.init();
        } catch (Exception e) {
            LOG.error("Failed to initialize JAX-RS test infrastructure", e);
            throw e;
        }

        // Store instance for access in beforeEach and afterAll
        getStore(context).put(TEST_INSTANCE_KEY, testInstance);

        // Inject into test class field
        injectTestInstance(context, testInstance);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        DynamicJaxrsTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicJaxrsTest.class);

        if (testInstance == null) {
            throw new IllegalStateException("Test instance not initialized. This should not happen.");
        }

        // Setup request with defaults for each test method
        testInstance.setupForNewRequest();
        testInstance.getHstRequest().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        testInstance.getHstRequest().setMethod(HttpMethod.GET);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        DynamicJaxrsTest testInstance = getStore(context).get(TEST_INSTANCE_KEY, DynamicJaxrsTest.class);

        if (testInstance != null) {
            LOG.info("Destroying JAX-RS test infrastructure for {}", context.getRequiredTestClass().getSimpleName());
            try {
                testInstance.destroy();
            } catch (Exception e) {
                LOG.error("Failed to destroy JAX-RS test infrastructure", e);
            } finally {
                getStore(context).remove(TEST_INSTANCE_KEY);
            }
        }
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(BrxmJaxrsTestExtension.class, context.getRequiredTestClass()));
    }

    private void injectTestInstance(ExtensionContext context, DynamicJaxrsTest testInstance) throws Exception {
        Object testObject = context.getRequiredTestInstance();
        Class<?> testClass = testObject.getClass();

        // Look for field of type DynamicJaxrsTest
        Field targetField = null;
        for (Field field : testClass.getDeclaredFields()) {
            if (DynamicJaxrsTest.class.isAssignableFrom(field.getType())) {
                targetField = field;
                break;
            }
        }

        if (targetField == null) {
            throw new IllegalStateException(
                    String.format("Test class %s must declare a field of type DynamicJaxrsTest. " +
                            "Example: private DynamicJaxrsTest brxm;", testClass.getName())
            );
        }

        // Inject
        targetField.setAccessible(true);
        targetField.set(testObject, testInstance);
        LOG.debug("Injected DynamicJaxrsTest instance into field: {}", targetField.getName());
    }
}
