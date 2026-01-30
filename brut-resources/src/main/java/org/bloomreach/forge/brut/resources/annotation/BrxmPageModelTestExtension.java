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
 * JUnit 5 extension that manages lifecycle for @BrxmPageModelTest annotated tests.
 * Handles initialization, request setup, and cleanup automatically.
 */
public class BrxmPageModelTestExtension extends BaseDynamicTestExtension<DynamicPageModelTest, BrxmPageModelTest> {

    private static final Logger LOG = LoggerFactory.getLogger(BrxmPageModelTestExtension.class);
    private static final String TEST_INSTANCE_KEY = "brxm.test.instance";
    private static final String FRAMEWORK = "PageModel";

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
    protected Class<BrxmPageModelTest> getAnnotationClass() {
        return BrxmPageModelTest.class;
    }

    @Override
    protected Class<DynamicPageModelTest> getTestInstanceClass() {
        return DynamicPageModelTest.class;
    }

    @Override
    protected DynamicPageModelTest createTestInstance(TestConfig config) {
        return new DynamicPageModelTest(config);
    }

    @Override
    protected TestConfig resolveConfig(BrxmPageModelTest annotation, Class<?> testClass) {
        return ConventionBasedConfigResolver.resolve(annotation, testClass);
    }
}
