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
    private static final String PAGEMODEL = "pagemodel";
    private static final String JAXRS = "jaxrs";

    static TestConfig resolve(BrxmPageModelTest annotation, Class<?> testClass) {
        return resolveConfig(
            annotation.beanPackages(),
            annotation.hstRoot(),
            resolveSpringConfigs(annotation.springConfig(), new String[0], testClass, PAGEMODEL),
            annotation.addonModules(),
            annotation.repositoryDataModules(),
            annotation.useConfigService(),
            ProjectDiscovery.BeanPackageOrder.BEANS_FIRST,
            testClass
        );
    }

    static TestConfig resolve(BrxmJaxrsTest annotation, Class<?> testClass) {
        return resolveConfig(
            annotation.beanPackages(),
            annotation.hstRoot(),
            resolveSpringConfigs(annotation.springConfig(), annotation.springConfigs(), testClass, JAXRS),
            annotation.addonModules(),
            annotation.repositoryDataModules(),
            annotation.useConfigService(),
            ProjectDiscovery.BeanPackageOrder.MODEL_FIRST,
            testClass
        );
    }

    private static TestConfig resolveConfig(String[] beanPackages,
                                            String hstRoot,
                                            List<String> springConfigs,
                                            String[] addonModulesArray,
                                            String[] repositoryDataModulesArray,
                                            boolean useConfigService,
                                            ProjectDiscovery.BeanPackageOrder beanOrder,
                                            Class<?> testClass) {
        List<String> beanPatterns = beanPackages.length > 0
                ? toPatterns(beanPackages)
                : detectBeanPatterns(testClass, beanOrder);

        String resolvedHstRoot = !hstRoot.isEmpty() ? hstRoot : detectHstRoot();

        List<String> repositoryDataModules = repositoryDataModulesArray.length > 0
                ? Arrays.asList(repositoryDataModulesArray)
                : List.of();

        if (useConfigService) {
            springConfigs = applyConfigService(springConfigs, repositoryDataModules, testClass);
        }

        List<String> addonModules = addonModulesArray.length > 0
                ? Arrays.asList(addonModulesArray)
                : null;

        LOG.debug("Resolved configuration for {}: beanPatterns={}, hstRoot={}, springConfigs={}, " +
                "addonModules={}, repositoryDataModules={}",
                testClass.getSimpleName(), beanPatterns, resolvedHstRoot, springConfigs,
                addonModules, repositoryDataModules);

        return new TestConfig(beanPatterns, resolvedHstRoot, springConfigs, addonModules, repositoryDataModules);
    }

    private static List<String> resolveSpringConfigs(String singleConfig, String[] multiConfigs,
                                                     Class<?> testClass, String testType) {
        if (multiConfigs.length > 0) {
            return Arrays.asList(multiConfigs);
        }
        if (!singleConfig.isEmpty()) {
            return Arrays.asList(singleConfig);
        }
        return detectMultipleSpringConfigs(
            testClass.getPackage().getName().replace('.', '/'),
            testType,
            testClass.getClassLoader()
        );
    }

    private static List<String> applyConfigService(List<String> springConfigs,
                                                   List<String> repositoryDataModules,
                                                   Class<?> testClass) {
        String projectNamespace = ProjectDiscovery.resolveProjectNamespace(
            Paths.get(System.getProperty("user.dir")));
        List<String> cndPatterns = detectCndPatterns(projectNamespace, true, testClass.getClassLoader());
        List<String> yamlPatterns = detectYamlPatterns(projectNamespace, true, testClass.getClassLoader());

        String configPath = ConfigServiceSpringConfig.create(
            projectNamespace, cndPatterns, yamlPatterns, repositoryDataModules);
        return prependSpringConfig(springConfigs, configPath);
    }

    private static List<String> toPatterns(String[] packages) {
        List<String> patterns = new ArrayList<>();
        for (String pkg : packages) {
            patterns.add("classpath*:" + pkg.replace('.', '/') + "/*.class,");
        }
        return patterns;
    }

    private static List<String> detectBeanPatterns(Class<?> testClass, ProjectDiscovery.BeanPackageOrder order) {
        List<String> packages = ProjectDiscovery.resolveBeanPackages(testClass, order, false);
        List<String> patterns = ProjectDiscovery.toClasspathPatterns(packages, false, true);
        LOG.debug("Auto-detected bean patterns ({}): {}", order, patterns);
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

    private static List<String> detectMultipleSpringConfigs(String packagePath, String testType,
                                                            ClassLoader classLoader) {
        List<String> candidateFiles = new ArrayList<>();

        if (JAXRS.equals(testType)) {
            candidateFiles.add("custom-jaxrs.xml");
            candidateFiles.add("annotation-jaxrs.xml");
            candidateFiles.add("rest-resources.xml");
            candidateFiles.add("jaxrs-config.xml");
        } else if (PAGEMODEL.equals(testType)) {
            candidateFiles.add("custom-pagemodel.xml");
            candidateFiles.add("annotation-pagemodel.xml");
            candidateFiles.add("custom-component.xml");
            candidateFiles.add("component-config.xml");
        }

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

        LOG.debug("Auto-detected {} Spring config(s): {}", detectedConfigs.size(), detectedConfigs);
        return detectedConfigs;
    }

    private static List<String> detectYamlPatterns(String projectNamespace, boolean useConfigService,
                                                   ClassLoader classLoader) {
        if (useConfigService) {
            LOG.debug("Skipping YAML auto-detection: ConfigService is enabled");
            return List.of();
        }

        List<String> candidatePatterns = new ArrayList<>();
        candidatePatterns.add("classpath*:hcm-config/**/*.yaml");
        candidatePatterns.add("classpath*:hcm-content/**/*.yaml");
        candidatePatterns.add("classpath*:test-repository-data/**/*.yaml");

        if (projectNamespace != null && !projectNamespace.isEmpty()) {
            candidatePatterns.add("classpath*:org/" + projectNamespace + "/imports/**/*.yaml");
        }

        return filterValidPatterns(candidatePatterns, classLoader, "YAML");
    }

    private static List<String> detectCndPatterns(String projectNamespace, boolean useConfigService,
                                                  ClassLoader classLoader) {
        if (useConfigService) {
            LOG.debug("Skipping CND auto-detection: ConfigService is enabled");
            return List.of();
        }

        List<String> candidatePatterns = new ArrayList<>();

        if (projectNamespace != null && !projectNamespace.isEmpty()) {
            candidatePatterns.add("classpath*:org/" + projectNamespace + "/namespaces/*.cnd");
            candidatePatterns.add("classpath*:**/" + projectNamespace + ".cnd");
        }

        candidatePatterns.add("classpath*:namespaces/**/*.cnd");

        return filterValidPatterns(candidatePatterns, classLoader, "CND");
    }

    private static List<String> filterValidPatterns(List<String> candidatePatterns, ClassLoader classLoader,
                                                    String patternType) {
        List<String> validPatterns = new ArrayList<>();
        for (String pattern : candidatePatterns) {
            if (patternHasResources(pattern, classLoader)) {
                validPatterns.add(pattern);
                LOG.debug("Auto-detected {} pattern: {}", patternType, pattern);
            }
        }

        if (validPatterns.isEmpty()) {
            LOG.debug("No {} resources found for auto-detection patterns", patternType);
            return List.of();
        }

        LOG.debug("Auto-detected {} {} pattern(s): {}", validPatterns.size(), patternType, validPatterns);
        return validPatterns;
    }

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
