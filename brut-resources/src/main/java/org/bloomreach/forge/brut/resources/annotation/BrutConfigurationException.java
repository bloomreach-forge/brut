package org.bloomreach.forge.brut.resources.annotation;

/**
 * Exception thrown when BRUT test configuration is invalid or incomplete.
 * Provides detailed context about what configuration was attempted and how to fix it.
 */
public class BrutConfigurationException extends RuntimeException {

    public BrutConfigurationException(String message) {
        super(message);
    }

    public BrutConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates exception for missing annotation.
     */
    public static BrutConfigurationException missingAnnotation(Class<?> testClass, String annotationName) {
        return new BrutConfigurationException(String.format(
            "Missing @%s annotation on test class: %s\n\n" +
            "To fix:\n" +
            "  1. Add @%s to your test class\n" +
            "  2. Ensure the annotation is imported: import org.bloomreach.forge.brut.resources.annotation.%s;\n" +
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
    public static BrutConfigurationException missingField(Class<?> testClass, String fieldType, String[] scannedFields) {
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

        return new BrutConfigurationException(message.toString());
    }

    /**
     * Creates exception for bootstrap failure with configuration context.
     */
    public static BrutConfigurationException bootstrapFailed(String phase, TestConfig config, Throwable cause) {
        String beanPatterns = config.getBeanPatterns() != null
            ? String.join(", ", config.getBeanPatterns())
            : "NONE";
        String springConfigs = config.getSpringConfigs() != null
            ? String.join(", ", config.getSpringConfigs())
            : "NONE";
        return new BrutConfigurationException(String.format(
            "Bootstrap failed during: %s\n\n" +
            "Configuration attempted:\n" +
            "  Bean patterns: %s\n" +
            "  Spring configs: %s\n" +
            "  HST root: %s\n\n" +
            "Root cause: %s\n\n" +
            "To fix:\n" +
            "  1. Check that all specified resources exist on classpath\n" +
            "  2. Verify bean packages contain valid Spring components\n" +
            "  3. Ensure Spring config files are well-formed XML\n" +
            "  4. Check logs above for more specific error details",
            phase,
            beanPatterns,
            springConfigs,
            config.getHstRoot(),
            cause.getMessage()
        ), cause);
    }

    /**
     * Creates exception for resource not found.
     */
    public static BrutConfigurationException resourceNotFound(String resourceType, String resourcePath, String suggestion) {
        return new BrutConfigurationException(String.format(
            "Required %s not found: %s\n\n" +
            "To fix:\n" +
            "  %s",
            resourceType, resourcePath, suggestion
        ));
    }

    /**
     * Creates exception for invalid state during test execution.
     */
    public static BrutConfigurationException invalidState(String state, String expectedCondition, String actualCondition) {
        return new BrutConfigurationException(String.format(
            "Invalid test state: %s\n\n" +
            "Expected: %s\n" +
            "Actual: %s\n\n" +
            "This indicates a bug in BRUT or misuse of test infrastructure.\n" +
            "Please report this issue with full stack trace.",
            state, expectedCondition, actualCondition
        ));
    }
}
