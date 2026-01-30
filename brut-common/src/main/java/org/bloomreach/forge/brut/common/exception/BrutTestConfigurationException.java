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
package org.bloomreach.forge.brut.common.exception;

import java.util.List;
import java.util.function.Function;

/**
 * Exception thrown when BRUT test configuration is invalid or incomplete.
 * Provides detailed context about what configuration was attempted and how to fix it.
 */
public class BrutTestConfigurationException extends RuntimeException {

    public BrutTestConfigurationException(String message) {
        super(message);
    }

    public BrutTestConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates exception for missing annotation.
     */
    public static BrutTestConfigurationException missingAnnotation(Class<?> testClass, String annotationName,
                                                                   String importPackage) {
        return new BrutTestConfigurationException(String.format(
            "Missing @%s annotation on test class: %s%n%n" +
            "To fix:%n" +
            "  1. Add @%s to your test class%n" +
            "  2. Ensure the annotation is imported: import %s.%s;%n" +
            "  3. If using inheritance, the annotation must be on the actual test class, not parent%n%n" +
            "Example:%n" +
            "  @%s%n" +
            "  public class %s { ... }",
            annotationName, testClass.getName(),
            annotationName, importPackage, annotationName,
            annotationName, testClass.getSimpleName()
        ));
    }

    /**
     * Creates exception for missing test field injection.
     */
    public static BrutTestConfigurationException missingField(Class<?> testClass, String fieldType,
                                                              String[] scannedFields) {
        StringBuilder message = new StringBuilder(String.format(
            "Missing required field of type %s in test class: %s%n%n" +
            "To fix:%n" +
            "  Add a field of type %s to your test class (any visibility, any name)%n%n" +
            "Example:%n" +
            "  private %s brxm;%n%n",
            fieldType, testClass.getName(),
            fieldType,
            fieldType
        ));

        if (scannedFields != null && scannedFields.length > 0) {
            message.append("Fields scanned: ").append(String.join(", ", scannedFields)).append("\n");
            message.append("None matched required type: ").append(fieldType);
        } else {
            message.append("No fields found in test class. Did you declare any fields?");
        }

        return new BrutTestConfigurationException(message.toString());
    }

    /**
     * Creates exception for invalid state during test execution.
     */
    public static BrutTestConfigurationException invalidState(String state, String expectedCondition,
                                                              String actualCondition) {
        return new BrutTestConfigurationException(String.format(
            "Invalid test state: %s%n%n" +
            "Expected: %s%n" +
            "Actual: %s%n%n" +
            "This indicates a bug in BRUT or misuse of test infrastructure.%n" +
            "Please report this issue with full stack trace.",
            state, expectedCondition, actualCondition
        ));
    }

    /**
     * Creates exception for setup/initialization failure with generic config.
     *
     * @param phase phase description
     * @param configDescription formatted config description
     * @param cause root cause
     */
    public static BrutTestConfigurationException setupFailed(String phase, String configDescription,
                                                             Throwable cause) {
        return new BrutTestConfigurationException(String.format(
            "Test setup failed during: %s%n%n" +
            "Configuration attempted:%n%s%n%n" +
            "Root cause: %s%n%n" +
            "To fix:%n" +
            "  1. Check that all specified resources exist on classpath%n" +
            "  2. Verify packages contain valid components%n" +
            "  3. Check logs above for more specific error details",
            phase,
            configDescription,
            cause != null ? cause.getMessage() : "Unknown"
        ), cause);
    }

    /**
     * Creates exception for bootstrap failure with bean/spring/hst config context.
     */
    public static BrutTestConfigurationException bootstrapFailed(String phase, List<String> beanPatterns,
                                                                 List<String> springConfigs, String hstRoot,
                                                                 Throwable cause) {
        String beanPatternsStr = beanPatterns != null ? String.join(", ", beanPatterns) : "NONE";
        String springConfigsStr = springConfigs != null ? String.join(", ", springConfigs) : "NONE";

        return new BrutTestConfigurationException(String.format(
            "Bootstrap failed during: %s%n%n" +
            "Configuration attempted:%n" +
            "  Bean patterns: %s%n" +
            "  Spring configs: %s%n" +
            "  HST root: %s%n%n" +
            "Root cause: %s%n%n" +
            "To fix:%n" +
            "  1. Check that all specified resources exist on classpath%n" +
            "  2. Verify bean packages contain valid Spring components%n" +
            "  3. Ensure Spring config files are well-formed XML%n" +
            "  4. Check logs above for more specific error details",
            phase,
            beanPatternsStr,
            springConfigsStr,
            hstRoot != null ? hstRoot : "/hst:hst",
            cause != null ? cause.getMessage() : "Unknown"
        ), cause);
    }

    /**
     * Creates exception for resource not found.
     */
    public static BrutTestConfigurationException resourceNotFound(String resourceType, String resourcePath,
                                                                  String suggestion) {
        return new BrutTestConfigurationException(String.format(
            "Required %s not found: %s%n%n" +
            "To fix:%n  %s",
            resourceType, resourcePath, suggestion
        ));
    }
}
