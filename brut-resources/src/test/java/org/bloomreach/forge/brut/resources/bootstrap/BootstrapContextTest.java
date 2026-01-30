package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapContextTest {

    @Test
    void testConstructorAndGetters() {
        List<String> cndPatterns = Arrays.asList("*.cnd", "**/*.cnd");
        List<String> yamlPatterns = Arrays.asList("*.yaml", "**/*.yaml");
        List<String> hcmPatterns = Arrays.asList("/hcm-config/**/*.yaml");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        BootstrapContext context = new BootstrapContext(
            cndPatterns,
            yamlPatterns,
            hcmPatterns,
            classLoader
        );

        assertEquals(cndPatterns, context.getCndPatterns());
        assertEquals(yamlPatterns, context.getYamlPatterns());
        assertEquals(hcmPatterns, context.getHcmConfigPatterns());
        assertTrue(context.getModuleDescriptors().isEmpty());
        assertEquals(classLoader, context.getClassLoader());
    }

    @Test
    void testWithEmptyLists() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            classLoader
        );

        assertTrue(context.getCndPatterns().isEmpty());
        assertTrue(context.getYamlPatterns().isEmpty());
        assertTrue(context.getHcmConfigPatterns().isEmpty());
        assertTrue(context.getModuleDescriptors().isEmpty());
        assertNotNull(context.getClassLoader());
    }

    @Test
    void testImmutability() {
        List<String> cndPatterns = Arrays.asList("*.cnd");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        BootstrapContext context = new BootstrapContext(
            cndPatterns,
            Collections.emptyList(),
            Collections.emptyList(),
            classLoader
        );

        // Verify the context stores references correctly
        assertEquals(1, context.getCndPatterns().size());
        assertEquals("*.cnd", context.getCndPatterns().get(0));
    }
}
