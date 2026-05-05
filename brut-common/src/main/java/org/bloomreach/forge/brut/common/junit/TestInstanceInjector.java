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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Utility for injecting test infrastructure instances into test class fields.
 * Used by JUnit 5 extensions to provide field injection of BRUT test instances.
 *
 * <p>Supports JUnit 5 {@code @Nested} test classes by searching enclosing class
 * instances via {@link TestInstances#getAllInstances()}.</p>
 *
 * <p>Field injection is optional - if no field is found, injection is skipped.
 * Tests can alternatively use parameter injection via JUnit 5 {@code ParameterResolver}.</p>
 */
public final class TestInstanceInjector {

    private TestInstanceInjector() {
    }

    /**
     * Injects a test infrastructure instance into a field of the test class if a matching field exists.
     * For nested test classes, also searches enclosing class instances.
     *
     * <p>If no matching field is found, injection is skipped (no error). Tests can use
     * parameter injection via ParameterResolver as an alternative.</p>
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
        List<Object> allInstances = context.getRequiredTestInstances().getAllInstances();

        // Search from innermost to outermost so the most specific field wins
        for (int i = allInstances.size() - 1; i >= 0; i--) {
            Object testObject = allInstances.get(i);
            Field targetField = findFieldOfType(testObject.getClass(), targetType);
            if (targetField != null) {
                targetField.setAccessible(true);
                targetField.set(testObject, instance);
                if (log != null) {
                    log.debug("Injected {} instance into field: {}", targetType.getSimpleName(), targetField.getName());
                }
                return;
            }
        }

        if (log != null) {
            log.debug("No field of type {} found; skipping field injection (parameter injection may be used)",
                    targetType.getSimpleName());
        }
    }

    private static Field findFieldOfType(Class<?> clazz, Class<?> targetType) {
        for (Field field : clazz.getDeclaredFields()) {
            if (targetType.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }
}
