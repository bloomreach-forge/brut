package org.bloomreach.forge.brut.components.annotation;

/**
 * Exception thrown when BRUT component test configuration is invalid or incomplete.
 * Provides detailed context about what configuration was attempted and how to fix it.
 */
public class ComponentConfigurationException extends RuntimeException {

    public ComponentConfigurationException(String message) {
        super(message);
    }

    public ComponentConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates exception for missing annotation.
     */
    public static ComponentConfigurationException missingAnnotation(Class<?> testClass, String annotationName) {
        return new ComponentConfigurationException(String.format(
            "Missing @%s annotation on test class: %s\n\n" +
            "To fix:\n" +
            "  1. Add @%s to your test class\n" +
            "  2. Ensure the annotation is imported: import org.bloomreach.forge.brut.components.annotation.%s;\n" +
            "  3. If using inheritance, the annotation must be on the actual test class, not parent\n\n" +
            "Example:\n" +
            "  @%s\n" +
            "  public class %s { ... }",
            annotationName, testClass.getName(),
            annotationName, annotationName,
            annotationName, testClass.getSimpleName()
        ));
    }

    /**
     * Creates exception for missing test field injection.
     */
    public static ComponentConfigurationException missingField(Class<?> testClass, String fieldType, String[] scannedFields) {
        StringBuilder message = new StringBuilder(String.format(
            "Missing required field of type %s in test class: %s\n\n" +
            "To fix:\n" +
            "  Add a field of type %s to your test class (any visibility, any name)\n\n" +
            "Example:\n" +
            "  private %s brxm;\n\n",
            fieldType, testClass.getName(),
            fieldType,
            fieldType
        ));

        if (scannedFields.length > 0) {
            message.append("Fields scanned: ").append(String.join(", ", scannedFields)).append("\n");
            message.append("None matched required type: ").append(fieldType);
        } else {
            message.append("No fields found in test class. Did you declare any fields?");
        }

        return new ComponentConfigurationException(message.toString());
    }

    /**
     * Creates exception for setup/initialization failure.
     */
    public static ComponentConfigurationException setupFailed(String phase, ComponentTestConfig config, Throwable cause) {
        return new ComponentConfigurationException(String.format(
            "Component test setup failed during: %s\n\n" +
            "Configuration attempted:\n" +
            "  Bean patterns: %s\n" +
            "  Test resource path: %s\n\n" +
            "Root cause: %s\n\n" +
            "To fix:\n" +
            "  1. Check that all specified resources exist on classpath\n" +
            "  2. Verify bean packages contain valid components\n" +
            "  3. Check logs above for more specific error details",
            phase,
            config.getAnnotatedClassesResourcePath(),
            config.getTestResourcePath() != null ? config.getTestResourcePath() : "NONE",
            cause.getMessage()
        ), cause);
    }

    /**
     * Creates exception for invalid state during test execution.
     */
    public static ComponentConfigurationException invalidState(String state, String expectedCondition, String actualCondition) {
        return new ComponentConfigurationException(String.format(
            "Invalid test state: %s\n\n" +
            "Expected: %s\n" +
            "Actual: %s\n\n" +
            "This indicates a bug in BRUT or misuse of test infrastructure.\n" +
            "Please report this issue with full stack trace.",
            state, expectedCondition, actualCondition
        ));
    }
}
