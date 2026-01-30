package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.bloomreach.forge.brut.common.project.ProjectSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BeanPackageStrategyChainTest {

    @Test
    void createDefaultContainsAllStrategies() {
        BeanPackageStrategyChain chain = BeanPackageStrategyChain.createDefault();

        List<String> info = chain.getStrategyInfo();

        assertEquals(4, info.size());
        assertTrue(info.get(0).contains("ProjectSettingsStrategy"));
        assertTrue(info.get(0).contains("priority=10"));
        assertTrue(info.get(1).contains("ClasspathNodeAnnotationStrategy"));
        assertTrue(info.get(1).contains("priority=20"));
        assertTrue(info.get(2).contains("PomGroupIdStrategy"));
        assertTrue(info.get(2).contains("priority=30"));
        assertTrue(info.get(3).contains("TestClassHeuristicStrategy"));
        assertTrue(info.get(3).contains("priority=40"));
    }

    @Test
    void resolveUsesFirstMatchingStrategy() {
        ProjectSettings settings = new ProjectSettings(
                null, null, null, null, null, "com.example",
                null, null, null, null, null);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectSettings(settings)
                .testPackage("com.test")
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        BeanPackageStrategyChain chain = BeanPackageStrategyChain.createDefault();
        List<String> packages = chain.resolve(context);

        // ProjectSettingsStrategy should win (priority 10)
        assertEquals(2, packages.size());
        assertEquals("com.example.beans", packages.get(0));
        assertEquals("com.example.model", packages.get(1));
    }

    @Test
    void resolveFallsBackToLowerPriorityStrategy() {
        DiscoveryContext context = DiscoveryContext.builder()
                .testPackage("com.fallback.test")
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        BeanPackageStrategyChain chain = BeanPackageStrategyChain.createDefault();
        List<String> packages = chain.resolve(context);

        // Should fall back to TestClassHeuristicStrategy (priority 40)
        // when no project settings and no classpath scanning results
        assertTrue(packages.contains("com.fallback.test.beans"));
        assertTrue(packages.contains("com.fallback.test.model"));
    }

    @Test
    void resolveReturnsEmptyListWhenNoStrategyMatches() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        // Create chain with only ProjectSettingsStrategy which won't match
        BeanPackageStrategyChain chain = BeanPackageStrategyChain.of(
                List.of(new ProjectSettingsStrategy()));

        List<String> packages = chain.resolve(context);

        assertTrue(packages.isEmpty());
    }

    @Test
    void ofSortsStrategiesByPriority() {
        BeanPackageStrategy low = new TestStrategy(100, "low");
        BeanPackageStrategy high = new TestStrategy(1, "high");
        BeanPackageStrategy mid = new TestStrategy(50, "mid");

        BeanPackageStrategyChain chain = BeanPackageStrategyChain.of(List.of(low, high, mid));

        List<String> info = chain.getStrategyInfo();
        assertTrue(info.get(0).contains("priority=1"));
        assertTrue(info.get(1).contains("priority=50"));
        assertTrue(info.get(2).contains("priority=100"));
    }

    @Test
    void resolveStopsAtFirstMatchingStrategy() {
        BeanPackageStrategy first = new TestStrategy(10, "first") {
            @Override
            public Optional<List<String>> resolve(DiscoveryContext context) {
                return Optional.of(List.of("first.package"));
            }
        };
        BeanPackageStrategy second = new TestStrategy(20, "second") {
            @Override
            public Optional<List<String>> resolve(DiscoveryContext context) {
                throw new AssertionError("Should not be called");
            }
        };

        BeanPackageStrategyChain chain = BeanPackageStrategyChain.of(List.of(first, second));
        DiscoveryContext context = DiscoveryContext.builder().build();

        List<String> packages = chain.resolve(context);

        assertEquals(1, packages.size());
        assertEquals("first.package", packages.get(0));
    }

    private static class TestStrategy implements BeanPackageStrategy {
        private final int priority;
        private final String name;

        TestStrategy(int priority, String name) {
            this.priority = priority;
            this.name = name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public Optional<List<String>> resolve(DiscoveryContext context) {
            return Optional.empty();
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
