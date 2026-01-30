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
import org.slf4j.Logger;

import java.lang.reflect.Field;

/**
 * Utility for injecting test infrastructure instances into test class fields.
 * Used by JUnit 5 extensions to provide field injection of BRUT test instances.
 *
 * <p>Supports JUnit 5 {@code @Nested} test classes by searching enclosing classes
 * when the target field is not found in the nested class itself.</p>
 *
 * <p>Field injection is optional - if no field is found, injection is skipped.
 * Tests can alternatively use parameter injection via JUnit 5 {@code ParameterResolver}.</p>
 */
public final class TestInstanceInjector {

    private static final String ENCLOSING_INSTANCE_PREFIX = "this$";

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
        Object testObject = context.getRequiredTestInstance();
        InjectionTarget target = findInjectionTarget(testObject, targetType);

        if (target == null) {
            if (log != null) {
                log.debug("No field of type {} found in {}; skipping field injection (parameter injection may be used)",
                        targetType.getSimpleName(), testObject.getClass().getSimpleName());
            }
            return;
        }

        target.field.setAccessible(true);
        target.field.set(target.instance, instance);

        if (log != null) {
            log.debug("Injected {} instance into field: {}", targetType.getSimpleName(), target.field.getName());
        }
    }

    private static <T> InjectionTarget findInjectionTarget(Object object, Class<T> targetType) {
        Object currentInstance = object;

        while (currentInstance != null) {
            Field targetField = findFieldOfType(currentInstance.getClass(), targetType);
            if (targetField != null) {
                return new InjectionTarget(currentInstance, targetField);
            }
            currentInstance = getEnclosingInstance(currentInstance);
        }

        return null;
    }

    private static Field findFieldOfType(Class<?> clazz, Class<?> targetType) {
        for (Field field : clazz.getDeclaredFields()) {
            if (targetType.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }

    private static Object getEnclosingInstance(Object object) {
        for (Field field : object.getClass().getDeclaredFields()) {
            if (field.getName().startsWith(ENCLOSING_INSTANCE_PREFIX)) {
                try {
                    field.setAccessible(true);
                    return field.get(object);
                } catch (IllegalAccessException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static class InjectionTarget {
        final Object instance;
        final Field field;

        InjectionTarget(Object instance, Field field) {
            this.instance = instance;
            this.field = field;
        }
    }
}
