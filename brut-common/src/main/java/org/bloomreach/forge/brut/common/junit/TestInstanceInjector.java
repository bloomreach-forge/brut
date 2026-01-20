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
package org.bloomreach.forge.brut.common.junit;

import org.bloomreach.forge.brut.common.exception.BrutTestConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.stream.Stream;

/**
 * Utility for injecting test infrastructure instances into test class fields.
 * Used by JUnit 5 extensions to provide field injection of BRUT test instances.
 */
public final class TestInstanceInjector {

    private TestInstanceInjector() {
    }

    /**
     * Injects a test infrastructure instance into a field of the test class.
     *
     * @param context JUnit extension context
     * @param instance the instance to inject
     * @param targetType the type to look for in test class fields
     * @param log logger for debug output
     * @param <T> type of the instance
     * @throws Exception if injection fails
     */
    public static <T> void inject(ExtensionContext context, T instance, Class<T> targetType, Logger log)
            throws Exception {
        Object testObject = context.getRequiredTestInstance();
        Class<?> testClass = testObject.getClass();

        Field[] allFields = testClass.getDeclaredFields();
        Field targetField = null;
        for (Field field : allFields) {
            if (targetType.isAssignableFrom(field.getType())) {
                targetField = field;
                break;
            }
        }

        if (targetField == null) {
            String[] scannedFieldInfo = Stream.of(allFields)
                    .map(f -> f.getName() + " (" + f.getType().getSimpleName() + ")")
                    .toArray(String[]::new);
            throw BrutTestConfigurationException.missingField(testClass, targetType.getSimpleName(), scannedFieldInfo);
        }

        targetField.setAccessible(true);
        targetField.set(testObject, instance);

        if (log != null) {
            log.debug("Injected {} instance into field: {}", targetType.getSimpleName(), targetField.getName());
        }
    }
}
