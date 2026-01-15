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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
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
        String packagePath = testClass.getPackage().getName().replace('.', '/');

        // Bean patterns: explicit or auto-detect
        List<String> beanPatterns = annotation.beanPackages().length > 0
                ? toPatterns(annotation.beanPackages())
                : detectBeanPatterns(packagePath);

        // HST root: explicit or auto-detect
        String hstRoot = !annotation.hstRoot().isEmpty()
                ? annotation.hstRoot()
                : detectHstRoot();

        // Spring config: explicit or auto-detect
        List<String> springConfigs = !annotation.springConfig().isEmpty()
                ? Arrays.asList(annotation.springConfig())
                : detectSpringConfigs(packagePath, "custom-pagemodel.xml", testClass.getClassLoader());

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
        String packagePath = testClass.getPackage().getName().replace('.', '/');

        // Bean patterns: explicit or auto-detect (model first, then beans for JAX-RS)
        List<String> beanPatterns = annotation.beanPackages().length > 0
                ? toPatterns(annotation.beanPackages())
                : detectJaxrsBeanPatterns(packagePath);

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
            springConfigs = detectSpringConfigs(packagePath, "custom-jaxrs.xml", testClass.getClassLoader());
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

    private static List<String> detectBeanPatterns(String packagePath) {
        List<String> patterns = new ArrayList<>();
        patterns.add("classpath*:" + packagePath + "/beans/*.class,");
        patterns.add("classpath*:" + packagePath + "/model/*.class,");
        LOG.debug("Auto-detected bean patterns (PageModel): {}", patterns);
        return patterns;
    }

    private static List<String> detectJaxrsBeanPatterns(String packagePath) {
        List<String> patterns = new ArrayList<>();
        // JAX-RS typically uses model first
        patterns.add("classpath*:" + packagePath + "/model/*.class,");
        patterns.add("classpath*:" + packagePath + "/beans/*.class,");
        LOG.debug("Auto-detected bean patterns (JAX-RS): {}", patterns);
        return patterns;
    }

    private static String detectHstRoot() {
        String projectName = detectProjectName();
        String hstRoot = "/hst:" + projectName;
        LOG.debug("Auto-detected HST root: {}", hstRoot);
        return hstRoot;
    }

    private static String detectProjectName() {
        // Try to find pom.xml and parse artifactId
        File pom = findPomFile(new File(System.getProperty("user.dir")));
        if (pom != null) {
            String artifactId = parsePomArtifactId(pom);
            if (artifactId != null) {
                LOG.debug("Detected project name from pom.xml: {}", artifactId);
                return artifactId;
            }
        }

        // Fallback: use directory name
        String dirName = new File(System.getProperty("user.dir")).getName();
        LOG.debug("Using directory name as project name: {}", dirName);
        return dirName;
    }

    private static File findPomFile(File dir) {
        if (dir == null || !dir.exists()) {
            return null;
        }

        File pom = new File(dir, "pom.xml");
        if (pom.exists()) {
            return pom;
        }

        // Search parent directories
        return findPomFile(dir.getParentFile());
    }

    private static String parsePomArtifactId(File pomFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);

            NodeList artifactIdNodes = doc.getElementsByTagName("artifactId");
            if (artifactIdNodes.getLength() > 0) {
                String artifactId = artifactIdNodes.item(0).getTextContent().trim();
                // Remove common suffixes
                if (artifactId.endsWith("-components")) {
                    artifactId = artifactId.substring(0, artifactId.length() - "-components".length());
                } else if (artifactId.endsWith("-site")) {
                    artifactId = artifactId.substring(0, artifactId.length() - "-site".length());
                }
                return artifactId;
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse pom.xml at {}: {}", pomFile, e.getMessage());
        }
        return null;
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
}
