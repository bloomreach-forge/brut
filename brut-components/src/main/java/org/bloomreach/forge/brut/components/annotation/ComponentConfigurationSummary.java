package org.bloomreach.forge.brut.components.annotation;

import org.slf4j.Logger;

/**
 * Utility for logging configuration summary at component test initialization.
 * Provides clear visibility into what configuration was resolved.
 */
public class ComponentConfigurationSummary {

    private ComponentConfigurationSummary() {
    }

    /**
     * Logs a detailed configuration summary for a component test.
     */
    public static void log(Logger logger, Class<?> testClass, ComponentTestConfig config) {
        logger.info("========================================");
        logger.info("Component Test Configuration Summary");
        logger.info("========================================");
        logger.info("Test Class: {}", testClass.getName());
        logger.info("");
        logger.info("Bean Patterns:");
        logger.info("  {}", config.getAnnotatedClassesResourcePath());

        if (config.getTestResourcePath() != null) {
            logger.info("Test Resource Path: {}", config.getTestResourcePath());
        } else {
            logger.info("Test Resource Path: NONE");
        }

        logger.info("========================================");
        logger.info("Component Test Setup Starting");
        logger.info("========================================");
    }

    /**
     * Logs successful setup completion.
     */
    public static void logSuccess(Logger logger, Class<?> testClass) {
        logger.info("========================================");
        logger.info("Component Test Setup COMPLETE");
        logger.info("Test Class: {}", testClass.getName());
        logger.info("Ready for test execution");
        logger.info("========================================");
    }

    /**
     * Logs setup failure with context.
     */
    public static void logFailure(Logger logger, Class<?> testClass, Throwable error) {
        logger.error("========================================");
        logger.error("Component Test Setup FAILED");
        logger.error("Test Class: {}", testClass.getName());
        logger.error("Error: {}", error.getMessage());
        logger.error("========================================");
    }
}
