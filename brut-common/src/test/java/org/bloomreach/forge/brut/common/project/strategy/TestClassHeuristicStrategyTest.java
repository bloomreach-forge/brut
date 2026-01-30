package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestClassHeuristicStrategyTest {

    private TestClassHeuristicStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TestClassHeuristicStrategy();
    }

    @Test
    void priorityIs40() {
        assertEquals(40, strategy.getPriority());
    }

    @Test
    void nameReturnsClassName() {
        assertEquals("TestClassHeuristicStrategy", strategy.getName());
    }

    @Test
    void resolveReturnsEmptyWhenNoTestPackage() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveReturnsEmptyWhenTestPackageIsBlank() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testPackage("   ")
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveDerivesPackagesFromTestPackageBeansFirst() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testPackage("com.example.test")
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        List<String> packages = result.get();
        assertEquals(2, packages.size());
        assertEquals("com.example.test.beans", packages.get(0));
        assertEquals("com.example.test.model", packages.get(1));
    }

    @Test
    void resolveDerivesPackagesFromTestPackageModelFirst() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testPackage("com.example.test")
                .order(BeanPackageOrder.MODEL_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        List<String> packages = result.get();
        assertEquals(2, packages.size());
        assertEquals("com.example.test.model", packages.get(0));
        assertEquals("com.example.test.beans", packages.get(1));
    }

    @Test
    void resolveIncludesDomainWhenRequested() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testPackage("com.example.test")
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(true)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        assertEquals(3, result.get().size());
        assertTrue(result.get().contains("com.example.test.domain"));
    }

    @Test
    void resolveUsesTestClassPackage() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testClass(TestClassHeuristicStrategyTest.class)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        String expectedPackage = getClass().getPackage().getName();
        assertEquals(expectedPackage + ".beans", result.get().get(0));
        assertEquals(expectedPackage + ".model", result.get().get(1));
    }
}
