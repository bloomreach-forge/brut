package org.bloomreach.forge.brut.resources.annotation;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Utility for logging configuration summary at test initialization.
 * Provides clear visibility into what configuration was resolved and how.
 */
public class ConfigurationSummary {

    private ConfigurationSummary() {
    }

    /**
     * Logs a detailed configuration summary for a test.
     */
    public static void log(Logger logger, Class<?> testClass, String testType, TestConfig config) {
        logger.info("========================================");
        logger.info("{} Configuration Summary", testType);
        logger.info("========================================");
        logger.info("Test Class: {}", testClass.getName());
        logger.info("");

        logBeanPackages(logger, config);
        logSpringConfigs(logger, config);
        logHstRoot(logger, config);
        logRepositoryData(logger, config);
        logConfigService(logger, config);
        logAddonModules(logger, config);

        logger.info("========================================");
        logger.info("{} Initialization Starting", testType);
        logger.info("========================================");
    }

    private static void logBeanPackages(Logger logger, TestConfig config) {
        List<String> patterns = config.getBeanPatterns();
        if (patterns.isEmpty()) {
            logger.warn("Bean Patterns: NONE (may cause Spring context to be empty)");
        } else {
            logger.info("Bean Patterns:");
            patterns.forEach(pattern -> logger.info("  - {}", pattern));
        }
    }

    private static void logSpringConfigs(Logger logger, TestConfig config) {
        List<String> springConfigs = config.getSpringConfigs();
        if (springConfigs == null || springConfigs.isEmpty()) {
            logger.warn("Spring Configs: NONE (using minimal context)");
        } else {
            logger.info("Spring Configs:");
            springConfigs.forEach(cfg -> {
                String marker = cfg.contains("auto-detected") ? " [AUTO-DETECTED]" : "";
                logger.info("  - {}{}", cfg, marker);
            });
        }
    }

    private static void logHstRoot(Logger logger, TestConfig config) {
        String hstRoot = config.getHstRoot();
        if (hstRoot == null || hstRoot.isEmpty()) {
            logger.info("HST Root: /hst:hst [DEFAULT]");
        } else {
            logger.info("HST Root: {}", hstRoot);
        }
    }

    private static void logRepositoryData(Logger logger, TestConfig config) {
        List<String> modules = config.getRepositoryDataModules();
        if (modules != null && !modules.isEmpty()) {
            logger.info("Repository Data Modules:");
            modules.forEach(module -> logger.info("  - {}", module));
        }
    }

    private static void logConfigService(Logger logger, TestConfig config) {
        // ConfigService usage is determined by the framework and logged elsewhere
    }

    private static void logAddonModules(Logger logger, TestConfig config) {
        List<String> modules = config.getAddonModules();
        if (modules != null && !modules.isEmpty()) {
            logger.info("Addon Modules:");
            modules.forEach(module -> logger.info("  - {}", module));
        }
    }

    /**
     * Logs successful initialization completion.
     */
    public static void logSuccess(Logger logger, String testType, Class<?> testClass) {
        logger.info("========================================");
        logger.info("{} Initialization COMPLETE", testType);
        logger.info("Test Class: {}", testClass.getName());
        logger.info("Ready for test execution");
        logger.info("========================================");
    }

    /**
     * Logs initialization failure with context.
     */
    public static void logFailure(Logger logger, String testType, Class<?> testClass, Throwable error) {
        logger.error("========================================");
        logger.error("{} Initialization FAILED", testType);
        logger.error("Test Class: {}", testClass.getName());
        logger.error("Error: {}", error.getMessage());
        logger.error("========================================");
    }
}
