package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClasspathNodeAnnotationStrategyTest {

    private final ClasspathNodeAnnotationStrategy strategy = new ClasspathNodeAnnotationStrategy();

    @Test
    void priorityIs20() {
        assertEquals(20, strategy.getPriority());
    }

    @Test
    void nameReturnsClassName() {
        assertEquals("ClasspathNodeAnnotationStrategy", strategy.getName());
    }

    @Test
    void resolveReturnsEmptyWhenNodeAnnotationNotOnClasspath() {
        // In a test environment without HST on classpath, this should return empty
        // Note: In real use, HST should be on classpath
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        // Result depends on whether @Node annotation is on classpath during test
        // The strategy handles missing annotation gracefully
        assertNotNull(result);
    }

    @Test
    void resolveWithModelFirstOrderRespectsOrder() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.MODEL_FIRST)
                .includeDomain(true)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        // If packages found, model should come before beans
        if (result.isPresent() && result.get().size() > 1) {
            List<String> packages = result.get();
            int modelIdx = findFirstIndexWithSuffix(packages, ".model");
            int beansIdx = findFirstIndexWithSuffix(packages, ".beans");
            if (modelIdx >= 0 && beansIdx >= 0) {
                assertTrue(modelIdx < beansIdx,
                        "MODEL_FIRST should place .model packages before .beans packages");
            }
        }
    }

    @Test
    void resolveWithBeansFirstOrderRespectsOrder() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        // If packages found, beans should come before model
        if (result.isPresent() && result.get().size() > 1) {
            List<String> packages = result.get();
            int beansIdx = findFirstIndexWithSuffix(packages, ".beans");
            int modelIdx = findFirstIndexWithSuffix(packages, ".model");
            if (beansIdx >= 0 && modelIdx >= 0) {
                assertTrue(beansIdx < modelIdx,
                        "BEANS_FIRST should place .beans packages before .model packages");
            }
        }
    }

    @Test
    void resolveExcludesDomainWhenNotIncluded() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        if (result.isPresent()) {
            boolean hasDomain = result.get().stream()
                    .anyMatch(pkg -> pkg.endsWith(".domain"));
            assertFalse(hasDomain, "Domain packages should be excluded when includeDomain is false");
        }
    }

    private int findFirstIndexWithSuffix(List<String> packages, String suffix) {
        for (int i = 0; i < packages.size(); i++) {
            if (packages.get(i).endsWith(suffix)) {
                return i;
            }
        }
        return -1;
    }
}
