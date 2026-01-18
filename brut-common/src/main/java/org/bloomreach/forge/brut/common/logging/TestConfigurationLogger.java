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
package org.bloomreach.forge.brut.common.logging;

import org.slf4j.Logger;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility for logging configuration summary at test initialization.
 * Provides clear visibility into what configuration was resolved and how.
 */
public final class TestConfigurationLogger {

    private TestConfigurationLogger() {
    }

    /**
     * Logs a configuration summary for a test with custom config formatter.
     *
     * @param log logger instance
     * @param testClass test class
     * @param framework framework name (e.g., "PageModel", "JAX-RS", "Component")
     * @param configFormatter consumer to log framework-specific configuration details
     */
    public static void logConfiguration(Logger log, Class<?> testClass, String framework,
                                        Consumer<Logger> configFormatter) {
        if (log == null || !log.isDebugEnabled()) {
            return;
        }
        log.debug("{} test: {} - resolving configuration", framework,
                testClass != null ? testClass.getSimpleName() : "Unknown");

        if (configFormatter != null) {
            configFormatter.accept(log);
        }
    }

    /**
     * Logs successful initialization completion.
     */
    public static void logSuccess(Logger log, String framework, Class<?> testClass) {
        if (log == null) {
            return;
        }
        log.info("{} test initialized: {}", framework,
                testClass != null ? testClass.getSimpleName() : "Unknown");
    }

    /**
     * Logs initialization failure with context.
     */
    public static void logFailure(Logger log, String framework, Class<?> testClass, Throwable error) {
        if (log == null) {
            return;
        }
        log.error("{} test failed to initialize: {} - {}",
                framework,
                testClass != null ? testClass.getSimpleName() : "Unknown",
                error != null ? error.getMessage() : "Unknown error");
    }

    /**
     * Logs bean patterns configuration.
     */
    public static void logBeanPatterns(Logger log, List<String> patterns) {
        if (log == null || !log.isDebugEnabled()) {
            return;
        }
        if (patterns == null || patterns.isEmpty()) {
            log.warn("No bean patterns configured");
        } else {
            log.debug("Bean patterns: {}", patterns);
        }
    }

    /**
     * Logs Spring configs.
     */
    public static void logSpringConfigs(Logger log, List<String> springConfigs) {
        if (log == null || !log.isDebugEnabled()) {
            return;
        }
        if (springConfigs == null || springConfigs.isEmpty()) {
            log.debug("No Spring configs specified");
        } else {
            log.debug("Spring configs: {}", springConfigs);
        }
    }

    /**
     * Logs HST root configuration.
     */
    public static void logHstRoot(Logger log, String hstRoot) {
        if (log == null || !log.isDebugEnabled()) {
            return;
        }
        log.debug("HST root: {}", hstRoot != null && !hstRoot.isEmpty() ? hstRoot : "/hst:hst");
    }

    /**
     * Logs list of modules with a label.
     */
    public static void logModules(Logger log, String label, List<String> modules) {
        if (log == null || !log.isDebugEnabled() || modules == null || modules.isEmpty()) {
            return;
        }
        log.debug("{}: {}", label, modules);
    }
}
