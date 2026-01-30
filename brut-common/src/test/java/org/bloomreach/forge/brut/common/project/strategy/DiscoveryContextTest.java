package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.bloomreach.forge.brut.common.project.ProjectSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryContextTest {

    @TempDir
    Path tempDir;

    @Test
    void builderCreatesContextWithDefaults() {
        DiscoveryContext context = DiscoveryContext.builder().build();

        assertNotNull(context.getStartPath());
        assertTrue(context.getProjectRoot().isEmpty());
        assertTrue(context.getTestPackage().isEmpty());
        assertEquals(BeanPackageOrder.BEANS_FIRST, context.getOrder());
        assertFalse(context.isIncludeDomain());
        assertTrue(context.getProjectSettings().isEmpty());
    }

    @Test
    void builderSetsStartPath() {
        DiscoveryContext context = DiscoveryContext.builder()
                .startPath(tempDir)
                .build();

        assertEquals(tempDir, context.getStartPath());
    }

    @Test
    void builderStartPathRejectsNull() {
        assertThrows(NullPointerException.class, () ->
                DiscoveryContext.builder().startPath(null));
    }

    @Test
    void builderSetsProjectRoot() {
        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .build();

        assertTrue(context.getProjectRoot().isPresent());
        assertEquals(tempDir, context.getProjectRoot().get());
    }

    @Test
    void builderSetsTestPackage() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testPackage("com.example.test")
                .build();

        assertTrue(context.getTestPackage().isPresent());
        assertEquals("com.example.test", context.getTestPackage().get());
    }

    @Test
    void builderSetsTestClassExtractsPackage() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testClass(DiscoveryContextTest.class)
                .build();

        assertTrue(context.getTestPackage().isPresent());
        assertEquals(getClass().getPackage().getName(), context.getTestPackage().get());
    }

    @Test
    void builderSetsOrder() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.MODEL_FIRST)
                .build();

        assertEquals(BeanPackageOrder.MODEL_FIRST, context.getOrder());
    }

    @Test
    void builderSetsIncludeDomain() {
        DiscoveryContext context = DiscoveryContext.builder()
                .includeDomain(true)
                .build();

        assertTrue(context.isIncludeDomain());
    }

    @Test
    void builderSetsProjectSettings() {
        ProjectSettings settings = new ProjectSettings(
                null, null, null, null, null, "com.example",
                null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectSettings(settings)
                .build();

        assertTrue(context.getProjectSettings().isPresent());
        assertEquals(settings, context.getProjectSettings().get());
    }

    @Test
    void contextIsImmutable() {
        ProjectSettings settings = new ProjectSettings(
                null, null, null, null, null, "com.example",
                null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .startPath(tempDir)
                .projectRoot(tempDir)
                .testPackage("com.test")
                .order(BeanPackageOrder.MODEL_FIRST)
                .includeDomain(true)
                .projectSettings(settings)
                .build();

        // Verify all values are accessible
        assertEquals(tempDir, context.getStartPath());
        assertEquals(tempDir, context.getProjectRoot().get());
        assertEquals("com.test", context.getTestPackage().get());
        assertEquals(BeanPackageOrder.MODEL_FIRST, context.getOrder());
        assertTrue(context.isIncludeDomain());
        assertEquals(settings, context.getProjectSettings().get());
    }
}
