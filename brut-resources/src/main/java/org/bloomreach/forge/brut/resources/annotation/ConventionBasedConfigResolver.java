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
                : detectSpringConfigs(testClass.getPackage().getName().replace('.', '/'),
                    "custom-pagemodel.xml",
                    testClass.getClassLoader());
        if (annotation.useConfigService()) {
            String projectNamespace = ProjectDiscovery.resolveProjectNamespace(Paths.get(System.getProperty("user.dir")));
            String configPath = ConfigServiceSpringConfig.create(projectNamespace, List.of(), List.of());
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
            springConfigs = detectSpringConfigs(testClass.getPackage().getName().replace('.', '/'),
                "custom-jaxrs.xml",
                testClass.getClassLoader());
        }
        if (annotation.useConfigService()) {
            String projectNamespace = ProjectDiscovery.resolveProjectNamespace(Paths.get(System.getProperty("user.dir")));
            String configPath = ConfigServiceSpringConfig.create(projectNamespace, List.of(), List.of());
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

    private static List<String> detectSpringConfigs(String packagePath, String configFileName, ClassLoader classLoader) {
        String path = "/" + packagePath + "/" + configFileName;
        if (resourceExists(path, classLoader)) {
            LOG.debug("Auto-detected Spring config: {}", path);
            return Arrays.asList(path);
        }
        LOG.debug("No Spring config found at {}", path);
        return null;
    }

    private static boolean resourceExists(String path, ClassLoader classLoader) {
        return classLoader.getResource(path) != null;
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
