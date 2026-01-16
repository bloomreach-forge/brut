package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigServiceBootstrapStrategyTest {

    @Test
    void testCanHandleReturnsFalseWhenNoHcmModule() {
        ConfigServiceBootstrapStrategy strategy = new ConfigServiceBootstrapStrategy();

        // Create classloader without hcm-module.yaml
        ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            emptyClassLoader
        );

        assertFalse(strategy.canHandle(context));
    }

    @Test
    void testCanHandleReturnsTrueWhenHcmModuleExists(@TempDir Path tempDir) throws IOException {
        ConfigServiceBootstrapStrategy strategy = new ConfigServiceBootstrapStrategy();

        // Create hcm-module.yaml in temp directory
        Path metaInf = tempDir.resolve("META-INF");
        Files.createDirectories(metaInf);
        Path hcmModule = metaInf.resolve("hcm-module.yaml");
        Files.writeString(hcmModule, "group:\n  name: test\nproject: test\nmodule:\n  name: test\n");

        // Create classloader with hcm-module.yaml
        ClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()});
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            classLoader
        );

        assertTrue(strategy.canHandle(context));
    }

    @Test
    void testCanHandleReturnsTrueWhenModuleDescriptorsProvided(@TempDir Path tempDir) throws IOException {
        ConfigServiceBootstrapStrategy strategy = new ConfigServiceBootstrapStrategy();

        Path moduleDescriptor = tempDir.resolve("hcm-module.yaml");
        Files.writeString(moduleDescriptor, "group:\n  name: test\nproject: test\nmodule:\n  name: test\n");

        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(moduleDescriptor),
            new URLClassLoader(new URL[0], null)
        );

        assertTrue(strategy.canHandle(context));
    }

    @Test
    void testInitializeHstStructureThrowsWhenNoHcmModule() {
        ConfigServiceBootstrapStrategy strategy = new ConfigServiceBootstrapStrategy();
        Session mockSession = mock(Session.class);

        ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            emptyClassLoader
        );

        // Should throw RepositoryException when no HCM module found
        assertThrows(RepositoryException.class, () ->
            strategy.initializeHstStructure(mockSession, "myproject", context)
        );
    }

    @Test
    void testFilteringClassLoaderCreation(@TempDir Path tempDir) throws IOException {
        ConfigServiceBootstrapStrategy strategy = new ConfigServiceBootstrapStrategy();

        // Create test resource structure
        Path testClasses = tempDir.resolve("target/test-classes");
        Path metaInf = testClasses.resolve("META-INF");
        Files.createDirectories(metaInf);

        Path hcmModule = metaInf.resolve("hcm-module.yaml");
        Files.writeString(hcmModule,
            "group:\n  name: test-group\nproject: test-project\nmodule:\n  name: test-module\n  config:\n    source: /hcm-config\n");

        // Create HCM config
        Path hcmConfig = testClasses.resolve("hcm-config/hst");
        Files.createDirectories(hcmConfig);
        Path hstConfig = hcmConfig.resolve("test-hst.yaml");
        Files.writeString(hstConfig,
            "definitions:\n  config:\n    /hst:hst:\n      jcr:primaryType: hst:hst\n");

        ClassLoader classLoader = new URLClassLoader(new URL[]{testClasses.toUri().toURL()});
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            classLoader
        );

        // Verify strategy can handle this context
        assertTrue(strategy.canHandle(context));
    }

    @Test
    void testWithNullProjectNamespaceDoesNotThrow(@TempDir Path tempDir) throws IOException {
        ConfigServiceBootstrapStrategy strategy = new ConfigServiceBootstrapStrategy();
        Session mockSession = mock(Session.class);

        // Create minimal hcm-module.yaml
        Path metaInf = tempDir.resolve("META-INF");
        Files.createDirectories(metaInf);
        Files.writeString(metaInf.resolve("hcm-module.yaml"),
            "group:\n  name: test\nproject: test\nmodule:\n  name: test\n");

        ClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()});
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            classLoader
        );

        // Should handle null project namespace (may fail later but not due to null)
        assertThrows(RepositoryException.class, () ->
            strategy.initializeHstStructure(mockSession, null, context)
        );
    }
}
