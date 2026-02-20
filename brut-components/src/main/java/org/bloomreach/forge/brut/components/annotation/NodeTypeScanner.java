package org.bloomreach.forge.brut.components.annotation;

import org.bloomreach.forge.brut.common.processor.BrxmBeanRegistry;
import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Scans classpath for classes annotated with {@code @Node} and extracts JCR type names.
 */
final class NodeTypeScanner {

    private static final Logger LOG = LoggerFactory.getLogger(NodeTypeScanner.class);
    private static final String CLASS_SUFFIX = ".class";

    private NodeTypeScanner() {
    }

    /**
     * Scans comma-separated classpath patterns for @Node-annotated classes and extracts JCR types.
     *
     * @param classpathPatterns comma-separated patterns
     * @return set of JCR type names
     */
    static Set<String> scanForNodeTypes(String classpathPatterns) {
        Set<String> fromRegistry = loadFromRegistry();
        if (!fromRegistry.isEmpty()) {
            LOG.debug("Using compile-time registry: {} node type(s): {}", fromRegistry.size(), fromRegistry);
            return fromRegistry;
        }

        Set<String> nodeTypes = new LinkedHashSet<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String pattern : classpathPatterns.split(",\\s*")) {
            if (!pattern.endsWith(CLASS_SUFFIX)) {
                continue;
            }
            scanPattern(resolver, pattern.trim(), nodeTypes);
        }

        LOG.debug("Auto-detected {} node type(s) from @Node annotations: {}", nodeTypes.size(), nodeTypes);
        return nodeTypes;
    }

    private static Set<String> loadFromRegistry() {
        Set<String> nodeTypes = new LinkedHashSet<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String fqn : BrxmBeanRegistry.loadBeanClassNames(cl)) {
            try {
                Class<?> clazz = Class.forName(fqn, false, cl);
                Node nodeAnnotation = clazz.getAnnotation(Node.class);
                if (nodeAnnotation != null && !nodeAnnotation.jcrType().isEmpty()) {
                    nodeTypes.add(nodeAnnotation.jcrType());
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOG.debug("Registry entry '{}' not loadable: {}", fqn, e.getMessage());
            }
        }
        return nodeTypes;
    }

    private static void scanPattern(ResourcePatternResolver resolver, String pattern, Set<String> nodeTypes) {
        try {
            Resource[] resources = resolver.getResources(pattern);
            LOG.debug("Scanning pattern '{}': found {} resources", pattern, resources.length);
            for (Resource resource : resources) {
                extractNodeType(resource, pattern, nodeTypes);
            }
        } catch (IOException e) {
            LOG.warn("Failed to scan pattern '{}': {}", pattern, e.getMessage());
        }
    }

    private static void extractNodeType(Resource resource, String pattern, Set<String> nodeTypes) {
        String className = resolveClassName(resource, pattern);
        if (className == null) {
            LOG.debug("Could not resolve class name for resource: {}", resource);
            return;
        }

        try {
            Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            Node nodeAnnotation = clazz.getAnnotation(Node.class);
            if (nodeAnnotation != null && !nodeAnnotation.jcrType().isEmpty()) {
                nodeTypes.add(nodeAnnotation.jcrType());
                LOG.debug("Found @Node(jcrType=\"{}\") on {}", nodeAnnotation.jcrType(), className);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOG.debug("Skipping class '{}': {}", className, e.getMessage());
        }
    }

    private static String resolveClassName(Resource resource, String pattern) {
        try {
            String path = resource.getURL().getPath();
            int classesIdx = path.indexOf("/classes/");
            int testClassesIdx = path.indexOf("/test-classes/");

            int startIdx;
            if (testClassesIdx >= 0) {
                startIdx = testClassesIdx + "/test-classes/".length();
            } else if (classesIdx >= 0) {
                startIdx = classesIdx + "/classes/".length();
            } else {
                // JAR entry: extract from pattern base
                return resolveFromJarPath(path);
            }

            String classPath = path.substring(startIdx);
            if (classPath.endsWith(CLASS_SUFFIX)) {
                classPath = classPath.substring(0, classPath.length() - CLASS_SUFFIX.length());
            }
            return classPath.replace('/', '.');
        } catch (IOException e) {
            LOG.trace("Could not resolve class name for resource: {}", resource);
            return null;
        }
    }

    private static String resolveFromJarPath(String path) {
        // Format: file:/path/to/jar.jar!/org/example/MyClass.class
        int jarSeparator = path.indexOf("!/");
        if (jarSeparator >= 0) {
            String classPath = path.substring(jarSeparator + 2);
            if (classPath.endsWith(CLASS_SUFFIX)) {
                classPath = classPath.substring(0, classPath.length() - CLASS_SUFFIX.length());
            }
            return classPath.replace('/', '.');
        }
        return null;
    }
}
