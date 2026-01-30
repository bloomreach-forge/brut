package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.bloomreach.forge.brut.common.project.ProjectSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSettingsStrategyTest {

    private ProjectSettingsStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ProjectSettingsStrategy();
    }

    @Test
    void priorityIs10() {
        assertEquals(10, strategy.getPriority());
    }

    @Test
    void nameReturnsClassName() {
        assertEquals("ProjectSettingsStrategy", strategy.getName());
    }

    @Test
    void resolveReturnsEmptyWhenNoProjectSettings() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveReturnsEmptyWhenNoProjectPackage() {
        ProjectSettings settings = new ProjectSettings(
                null, null, null, null, null, null, null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectSettings(settings)
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveUsesProjectPackageWithBeansFirst() {
        ProjectSettings settings = new ProjectSettings(
                null, null, null, null, null, "com.example.myproject",
                null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectSettings(settings)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        List<String> packages = result.get();
        assertEquals(2, packages.size());
        assertEquals("com.example.myproject.beans", packages.get(0));
        assertEquals("com.example.myproject.model", packages.get(1));
    }

    @Test
    void resolveUsesProjectPackageWithModelFirst() {
        ProjectSettings settings = new ProjectSettings(
                null, null, null, null, null, "com.example.myproject",
                null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectSettings(settings)
                .order(BeanPackageOrder.MODEL_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        List<String> packages = result.get();
        assertEquals(2, packages.size());
        assertEquals("com.example.myproject.model", packages.get(0));
        assertEquals("com.example.myproject.beans", packages.get(1));
    }

    @Test
    void resolvePrefersSelectedBeansPackage() {
        ProjectSettings settings = new ProjectSettings(
                null, null, "com.custom.beans", null, null, "com.example.myproject",
                null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectSettings(settings)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        List<String> packages = result.get();
        assertEquals("com.custom.beans", packages.get(0));
        assertEquals("com.example.myproject.model", packages.get(1));
    }

    @Test
    void resolveIncludesDomainWhenRequested() {
        ProjectSettings settings = new ProjectSettings(
                null, null, null, null, null, "com.example",
                null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectSettings(settings)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(true)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        assertEquals(3, result.get().size());
        assertTrue(result.get().contains("com.example.domain"));
    }
}
