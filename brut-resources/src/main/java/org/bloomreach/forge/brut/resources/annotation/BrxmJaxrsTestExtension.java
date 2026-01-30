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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that manages lifecycle for @BrxmJaxrsTest annotated tests.
 * Handles initialization, request setup, and cleanup automatically.
 */
public class BrxmJaxrsTestExtension extends BaseDynamicTestExtension<DynamicJaxrsTest, BrxmJaxrsTest> {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmJaxrsTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.jaxrs.test.instance";
    private static final String FRAMEWORK = "JAX-RS";

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected String getTestInstanceKey() {
        return TEST_INSTANCE_KEY;
    }

    @Override
    protected String getFrameworkName() {
        return FRAMEWORK;
    }

    @Override
    protected Class<BrxmJaxrsTest> getAnnotationClass() {
        return BrxmJaxrsTest.class;
    }

    @Override
    protected Class<DynamicJaxrsTest> getTestInstanceClass() {
        return DynamicJaxrsTest.class;
    }

    @Override
    protected DynamicJaxrsTest createTestInstance(TestConfig config) {
        return new DynamicJaxrsTest(config);
    }

    @Override
    protected TestConfig resolveConfig(BrxmJaxrsTest annotation, Class<?> testClass) {
        return ConventionBasedConfigResolver.resolve(annotation, testClass);
    }
}
