package org.bloomreach.forge.brut.resources.bootstrap;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Context object holding configuration needed for JCR bootstrap.
 * <p>
 * Immutable value object passed to {@link JcrBootstrapStrategy} implementations.
 *
 * @since 5.2.0
 */
public class BootstrapContext {

    private final List<String> cndPatterns;
    private final List<String> yamlPatterns;
    private final List<String> hcmConfigPatterns;
    private final List<Path> moduleDescriptors;
    private final ClassLoader classLoader;

    /**
     * Creates a new bootstrap context.
     *
     * @param cndPatterns CND resource patterns (e.g., "classpath*:org/example/**\/*.cnd")
     * @param yamlPatterns YAML content resource patterns
     * @param hcmConfigPatterns HCM config resource patterns (optional, may be null or empty)
     * @param classLoader ClassLoader to use for resource loading
     */
    public BootstrapContext(List<String> cndPatterns,
                            List<String> yamlPatterns,
                            List<String> hcmConfigPatterns,
                            ClassLoader classLoader) {
        this(cndPatterns, yamlPatterns, hcmConfigPatterns, Collections.emptyList(), classLoader);
    }

    /**
     * Creates a new bootstrap context with explicit module descriptors.
     *
     * @param cndPatterns CND resource patterns (e.g., "classpath*:org/example/**\/*.cnd")
     * @param yamlPatterns YAML content resource patterns
     * @param hcmConfigPatterns HCM config resource patterns (optional, may be null or empty)
     * @param moduleDescriptors explicit hcm-module.yaml paths (optional)
     * @param classLoader ClassLoader to use for resource loading
     */
    public BootstrapContext(List<String> cndPatterns,
                            List<String> yamlPatterns,
                            List<String> hcmConfigPatterns,
                            List<Path> moduleDescriptors,
                            ClassLoader classLoader) {
        this.cndPatterns = cndPatterns != null ? List.copyOf(cndPatterns) : Collections.emptyList();
        this.yamlPatterns = yamlPatterns != null ? List.copyOf(yamlPatterns) : Collections.emptyList();
        this.hcmConfigPatterns = hcmConfigPatterns != null ? List.copyOf(hcmConfigPatterns) : Collections.emptyList();
        this.moduleDescriptors = moduleDescriptors != null ? List.copyOf(moduleDescriptors) : Collections.emptyList();
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

    /**
     * @return CND resource patterns for node type registration
     */
    public List<String> getCndPatterns() {
        return cndPatterns;
    }

    /**
     * @return YAML content resource patterns
     */
    public List<String> getYamlPatterns() {
        return yamlPatterns;
    }

    /**
     * @return HCM config resource patterns (used by manual strategy)
     */
    public List<String> getHcmConfigPatterns() {
        return hcmConfigPatterns;
    }

    /**
     * @return explicit hcm-module.yaml paths
     */
    public List<Path> getModuleDescriptors() {
        return moduleDescriptors;
    }

    /**
     * @return ClassLoader for resource loading
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
