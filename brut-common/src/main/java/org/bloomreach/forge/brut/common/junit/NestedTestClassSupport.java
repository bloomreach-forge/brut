package org.bloomreach.forge.brut.common.junit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;

/**
 * Utility for handling JUnit 5 {@code @Nested} test classes.
 * Provides methods to find annotations on enclosing classes and manage nested test contexts.
 */
public final class NestedTestClassSupport {

    private NestedTestClassSupport() {
    }

    /**
     * Finds an annotation on the test class or any of its enclosing classes.
     * This supports nested test classes that should inherit configuration from their enclosing class.
     *
     * @param testClass       the test class to search from
     * @param annotationType  the annotation type to find
     * @param <A>             annotation type
     * @return the annotation if found, null otherwise
     */
    public static <A extends Annotation> A findAnnotation(Class<?> testClass, Class<A> annotationType) {
        Class<?> currentClass = testClass;
        while (currentClass != null) {
            A annotation = currentClass.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            currentClass = currentClass.getEnclosingClass();
        }
        return null;
    }

    /**
     * Gets the root test class, walking up through nested classes to find the outermost class.
     *
     * @param testClass the test class (may be nested)
     * @return the root test class
     */
    public static Class<?> getRootTestClass(Class<?> testClass) {
        Class<?> currentClass = testClass;
        while (currentClass.getEnclosingClass() != null && currentClass.isAnnotationPresent(Nested.class)) {
            currentClass = currentClass.getEnclosingClass();
        }
        return currentClass;
    }

    /**
     * Checks if the given test class is a nested test class.
     *
     * @param testClass the test class to check
     * @return true if the class is a nested test class
     */
    public static boolean isNestedTestClass(Class<?> testClass) {
        return testClass.isAnnotationPresent(Nested.class) && testClass.getEnclosingClass() != null;
    }

    /**
     * Gets the root extension context for nested test classes.
     * Walks up the context hierarchy to find the root.
     *
     * @param context the current extension context
     * @return the root extension context
     */
    public static ExtensionContext getRootContext(ExtensionContext context) {
        ExtensionContext current = context;
        while (current.getParent().isPresent()) {
            ExtensionContext parent = current.getParent().get();
            if (parent.getTestClass().isEmpty()) {
                break;
            }
            current = parent;
        }
        return current;
    }
}
