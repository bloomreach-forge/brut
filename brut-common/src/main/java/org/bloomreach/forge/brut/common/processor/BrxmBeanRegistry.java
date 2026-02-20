package org.bloomreach.forge.brut.common.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reads the compile-time bean registry files written by {@link BrxmBeanRegistryProcessor}.
 *
 * <p>Both methods return empty collections when the registry resource is absent,
 * preserving full backward compatibility with projects that do not have the
 * annotation processor on their compile classpath.
 */
public final class BrxmBeanRegistry {

    static final String BEANS_RESOURCE = "META-INF/brut-beans.list";

    private BrxmBeanRegistry() {
    }

    /**
     * Returns the fully-qualified class names of {@code @Node}-annotated beans
     * recorded at compile time. Empty when the registry is absent.
     *
     * @param classLoader classloader to search for the registry resource
     * @return insertion-ordered set of FQNs; never null
     */
    public static Set<String> loadBeanClassNames(ClassLoader classLoader) {
        return loadLines(classLoader, BEANS_RESOURCE, new LinkedHashSet<>());
    }

    private static <C extends Collection<String>> C loadLines(
            ClassLoader classLoader, String resource, C collection) {
        try {
            Enumeration<URL> urls = classLoader.getResources(resource);
            while (urls.hasMoreElements()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(urls.nextElement().openStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                          .map(String::trim)
                          .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                          .forEach(collection::add);
                }
            }
        } catch (IOException ignored) {
            // resource absent — caller falls back to classpath scan
        }
        return collection;
    }
}
