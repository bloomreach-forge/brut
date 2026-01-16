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
package org.bloomreach.forge.brut.resources.annotation;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves test configuration using convention-over-configuration approach.
 * Auto-detects configuration values when not explicitly provided.
 */
class ConventionBasedConfigResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ConventionBasedConfigResolver.class);

    /**
     * Resolves test configuration from PageModel annotation and test class.
     *
     * @param annotation annotation instance
     * @param testClass test class
     * @return resolved configuration
     */
    static TestConfig resolve(BrxmPageModelTest annotation, Class<?> testClass) {
        // Bean patterns: explicit or auto-detect
        List<String> beanPatterns = annotation.beanPackages().length > 0
                ? toPatterns(annotation.beanPackages())
                : detectBeanPatterns(testClass);

        // HST root: explicit or auto-detect
        String hstRoot = !annotation.hstRoot().isEmpty()
                ? annotation.hstRoot()
                : detectHstRoot();

        // Spring config: explicit or auto-detect
        List<String> springConfigs = !annotation.springConfig().isEmpty()
                ? Arrays.asList(annotation.springConfig())
                : detectMultipleSpringConfigs(
                    testClass.getPackage().getName().replace('.', '/'),
                    "pagemodel",
                    testClass.getClassLoader()
                  );
        if (annotation.useConfigService()) {
            String projectNamespace = ProjectDiscovery.resolveProjectNamespace(Paths.get(System.getProperty("user.dir")));

            // Auto-detect CND and YAML patterns
            List<String> cndPatterns = detectCndPatterns(projectNamespace, true, testClass.getClassLoader());
            List<String> yamlPatterns = detectYamlPatterns(projectNamespace, true, testClass.getClassLoader());

            String configPath = ConfigServiceSpringConfig.create(projectNamespace, cndPatterns, yamlPatterns);
            springConfigs = prependSpringConfig(springConfigs, configPath);
        }

        // Addon modules: explicit or null
        List<String> addonModules = annotation.addonModules().length > 0
                ? Arrays.asList(annotation.addonModules())
                : null;

        LOG.info("Resolved configuration for {}: beanPatterns={}, hstRoot={}, springConfigs={}, addonModules={}",
                testClass.getSimpleName(), beanPatterns, hstRoot, springConfigs, addonModules);

        return new TestConfig(beanPatterns, hstRoot, springConfigs, addonModules);
    }

    /**
     * Resolves test configuration from JAX-RS annotation and test class.
     *
     * @param annotation annotation instance
     * @param testClass test class
     * @return resolved configuration
     */
    static TestConfig resolve(BrxmJaxrsTest annotation, Class<?> testClass) {
        // Bean patterns: explicit or auto-detect (model first, then beans for JAX-RS)
        List<String> beanPatterns = annotation.beanPackages().length > 0
                ? toPatterns(annotation.beanPackages())
                : detectJaxrsBeanPatterns(testClass);

        // HST root: explicit or auto-detect
        String hstRoot = !annotation.hstRoot().isEmpty()
                ? annotation.hstRoot()
                : detectHstRoot();

        // Spring configs: explicit or auto-detect
        List<String> springConfigs;
        if (annotation.springConfigs().length > 0) {
            springConfigs = Arrays.asList(annotation.springConfigs());
        } else if (!annotation.springConfig().isEmpty()) {
            springConfigs = Arrays.asList(annotation.springConfig());
        } else {
            springConfigs = detectMultipleSpringConfigs(
                testClass.getPackage().getName().replace('.', '/'),
                "jaxrs",
                testClass.getClassLoader()
            );
        }
        if (annotation.useConfigService()) {
            String projectNamespace = ProjectDiscovery.resolveProjectNamespace(Paths.get(System.getProperty("user.dir")));

            // Auto-detect CND and YAML patterns
            List<String> cndPatterns = detectCndPatterns(projectNamespace, true, testClass.getClassLoader());
            List<String> yamlPatterns = detectYamlPatterns(projectNamespace, true, testClass.getClassLoader());

            String configPath = ConfigServiceSpringConfig.create(projectNamespace, cndPatterns, yamlPatterns);
            springConfigs = prependSpringConfig(springConfigs, configPath);
        }

        // Addon modules: explicit or null
        List<String> addonModules = annotation.addonModules().length > 0
                ? Arrays.asList(annotation.addonModules())
                : null;

        LOG.info("Resolved configuration for {}: beanPatterns={}, hstRoot={}, springConfigs={}, addonModules={}",
                testClass.getSimpleName(), beanPatterns, hstRoot, springConfigs, addonModules);

        return new TestConfig(beanPatterns, hstRoot, springConfigs, addonModules);
    }

    private static List<String> toPatterns(String[] packages) {
        List<String> patterns = new ArrayList<>();
        for (String pkg : packages) {
            String pattern = "classpath*:" + pkg.replace('.', '/') + "/*.class,";
            patterns.add(pattern);
        }
        return patterns;
    }

    private static List<String> detectBeanPatterns(Class<?> testClass) {
        List<String> packages = ProjectDiscovery.resolveBeanPackages(
            testClass,
            ProjectDiscovery.BeanPackageOrder.BEANS_FIRST,
            false
        );
        List<String> patterns = ProjectDiscovery.toClasspathPatterns(packages, false, true);
        LOG.debug("Auto-detected bean patterns (PageModel): {}", patterns);
        return patterns;
    }

    private static List<String> detectJaxrsBeanPatterns(Class<?> testClass) {
        List<String> packages = ProjectDiscovery.resolveBeanPackages(
            testClass,
            ProjectDiscovery.BeanPackageOrder.MODEL_FIRST,
            false
        );
        List<String> patterns = ProjectDiscovery.toClasspathPatterns(packages, false, true);
        LOG.debug("Auto-detected bean patterns (JAX-RS): {}", patterns);
        return patterns;
    }

    private static String detectHstRoot() {
        String hstRoot = ProjectDiscovery.resolveHstRoot(Paths.get(System.getProperty("user.dir")));
        LOG.debug("Auto-detected HST root: {}", hstRoot);
        return hstRoot;
    }

    private static boolean resourceExists(String path, ClassLoader classLoader) {
        return classLoader.getResource(path) != null;
    }

    /**
     * Detects multiple Spring config files in the test package.
     * Checks for common config file names used across brXM projects.
     *
     * @param packagePath package path in classpath format (org/example)
     * @param testType "jaxrs" or "pagemodel" to determine file priority
     * @param classLoader classloader to check resource existence
     * @return list of detected config file paths, or null if none found
     */
    private static List<String> detectMultipleSpringConfigs(String packagePath, String testType, ClassLoader classLoader) {
        List<String> candidateFiles = new ArrayList<>();

        // Type-specific candidate files
        if ("jaxrs".equals(testType)) {
            candidateFiles.add("custom-jaxrs.xml");
            candidateFiles.add("annotation-jaxrs.xml");
            candidateFiles.add("rest-resources.xml");
            candidateFiles.add("jaxrs-config.xml");
        } else if ("pagemodel".equals(testType)) {
            candidateFiles.add("custom-pagemodel.xml");
            candidateFiles.add("annotation-pagemodel.xml");
            candidateFiles.add("custom-component.xml");
            candidateFiles.add("component-config.xml");
        }

        // Scan for existing files
        List<String> detectedConfigs = new ArrayList<>();
        for (String candidateFile : candidateFiles) {
            String path = "/" + packagePath + "/" + candidateFile;
            if (resourceExists(path, classLoader)) {
                detectedConfigs.add(path);
                LOG.debug("Auto-detected Spring config: {}", path);
            }
        }

        if (detectedConfigs.isEmpty()) {
            LOG.debug("No Spring configs found in package: {}", packagePath);
            return null;
        }

        LOG.info("Auto-detected {} Spring config files: {}", detectedConfigs.size(), detectedConfigs);
        return detectedConfigs;
    }

    /**
     * Auto-detects YAML resource patterns for repository initialization.
     * Only activates when ConfigService is disabled to avoid conflicts.
     *
     * @param projectNamespace project namespace (e.g., "myproject")
     * @param useConfigService whether ConfigService is enabled
     * @param classLoader classloader to verify pattern resources exist
     * @return list of detected YAML patterns, or empty list
     */
    private static List<String> detectYamlPatterns(String projectNamespace, boolean useConfigService, ClassLoader classLoader) {
        // Skip auto-detection if ConfigService is enabled (it handles YAML internally)
        if (useConfigService) {
            LOG.debug("Skipping YAML auto-detection: ConfigService is enabled");
            return List.of();
        }

        List<String> candidatePatterns = new ArrayList<>();

        // Standard HCM patterns
        candidatePatterns.add("classpath*:hcm-config/**/*.yaml");
        candidatePatterns.add("classpath*:hcm-content/**/*.yaml");

        // Test-specific patterns
        candidatePatterns.add("classpath*:test-repository-data/**/*.yaml");

        // Project-specific legacy pattern
        if (projectNamespace != null && !projectNamespace.isEmpty()) {
            candidatePatterns.add("classpath*:org/" + projectNamespace + "/imports/**/*.yaml");
        }

        // Verify at least one pattern has resources
        List<String> validPatterns = new ArrayList<>();
        for (String pattern : candidatePatterns) {
            if (patternHasResources(pattern, classLoader)) {
                validPatterns.add(pattern);
                LOG.debug("Auto-detected YAML pattern: {}", pattern);
            }
        }

        if (validPatterns.isEmpty()) {
            LOG.debug("No YAML resources found for auto-detection patterns");
            return List.of();
        }

        LOG.info("Auto-detected {} YAML patterns: {}", validPatterns.size(), validPatterns);
        return validPatterns;
    }

    /**
     * Auto-detects CND resource patterns for node type registration.
     * Only activates when ConfigService is disabled.
     *
     * @param projectNamespace project namespace (e.g., "myproject")
     * @param useConfigService whether ConfigService is enabled
     * @param classLoader classloader to verify pattern resources exist
     * @return list of detected CND patterns, or empty list
     */
    private static List<String> detectCndPatterns(String projectNamespace, boolean useConfigService, ClassLoader classLoader) {
        // Skip auto-detection if ConfigService is enabled
        if (useConfigService) {
            LOG.debug("Skipping CND auto-detection: ConfigService is enabled");
            return List.of();
        }

        List<String> candidatePatterns = new ArrayList<>();

        // Project-specific CND
        if (projectNamespace != null && !projectNamespace.isEmpty()) {
            candidatePatterns.add("classpath*:org/" + projectNamespace + "/namespaces/*.cnd");
            candidatePatterns.add("classpath*:**/" + projectNamespace + ".cnd");
        }

        // Standard locations
        candidatePatterns.add("classpath*:namespaces/**/*.cnd");

        List<String> validPatterns = new ArrayList<>();
        for (String pattern : candidatePatterns) {
            if (patternHasResources(pattern, classLoader)) {
                validPatterns.add(pattern);
                LOG.debug("Auto-detected CND pattern: {}", pattern);
            }
        }

        if (validPatterns.isEmpty()) {
            LOG.debug("No CND resources found for auto-detection patterns");
            return List.of();
        }

        LOG.info("Auto-detected {} CND patterns: {}", validPatterns.size(), validPatterns);
        return validPatterns;
    }

    /**
     * Checks if at least one resource matches the given classpath pattern.
     *
     * @param pattern Spring ResourceLoader pattern (classpath*:...)
     * @param classLoader classloader to resolve resources
     * @return true if at least one matching resource exists
     */
    private static boolean patternHasResources(String pattern, ClassLoader classLoader) {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
            Resource[] resources = resolver.getResources(pattern);
            return resources != null && resources.length > 0;
        } catch (IOException e) {
            LOG.debug("Failed to check pattern {}: {}", pattern, e.getMessage());
            return false;
        }
    }

    private static List<String> prependSpringConfig(List<String> configs, String configPath) {
        List<String> result = new ArrayList<>();
        if (configPath != null) {
            result.add(configPath);
        }
        if (configs != null) {
            result.addAll(configs);
        }
        return result;
    }
}
