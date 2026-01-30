package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fallback strategy that derives bean packages from the test class package.
 * Appends common suffixes (.beans, .model, .domain) to the test class package.
 * Priority: 40 (lowest - last resort)
 */
public final class TestClassHeuristicStrategy implements BeanPackageStrategy {

    public static final int PRIORITY = 40;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Optional<List<String>> resolve(DiscoveryContext context) {
        Optional<String> testPackageOpt = context.getTestPackage();
        if (testPackageOpt.isEmpty()) {
            return Optional.empty();
        }

        String testPackage = testPackageOpt.get();
        if (testPackage.isBlank()) {
            return Optional.empty();
        }

        Set<String> packages = new LinkedHashSet<>();

        if (context.getOrder() == BeanPackageOrder.MODEL_FIRST) {
            packages.add(testPackage + ".model");
            packages.add(testPackage + ".beans");
        } else {
            packages.add(testPackage + ".beans");
            packages.add(testPackage + ".model");
        }

        if (context.isIncludeDomain()) {
            packages.add(testPackage + ".domain");
        }

        return Optional.of(new ArrayList<>(packages));
    }
}
