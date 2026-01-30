package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.bloomreach.forge.brut.common.project.ProjectSettings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Strategy that resolves bean packages from project-settings.xml.
 * Priority: 10 (highest)
 */
public final class ProjectSettingsStrategy implements BeanPackageStrategy {

    public static final int PRIORITY = 10;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Optional<List<String>> resolve(DiscoveryContext context) {
        Optional<ProjectSettings> settingsOpt = context.getProjectSettings();
        if (settingsOpt.isEmpty()) {
            return Optional.empty();
        }

        ProjectSettings settings = settingsOpt.get();
        String projectPackage = settings.getSelectedProjectPackage();
        if (projectPackage == null || projectPackage.isBlank()) {
            return Optional.empty();
        }

        Set<String> packages = new LinkedHashSet<>();
        String selectedBeansPackage = settings.getSelectedBeansPackage();
        String modelPackage = projectPackage + ".model";
        String beansPackageFallback = projectPackage + ".beans";
        String domainPackage = projectPackage + ".domain";

        if (context.getOrder() == BeanPackageOrder.MODEL_FIRST) {
            addPackage(packages, modelPackage);
            addPackage(packages, selectedBeansPackage != null ? selectedBeansPackage : beansPackageFallback);
        } else {
            addPackage(packages, selectedBeansPackage != null ? selectedBeansPackage : beansPackageFallback);
            addPackage(packages, modelPackage);
        }

        if (context.isIncludeDomain()) {
            addPackage(packages, domainPackage);
        }

        return packages.isEmpty() ? Optional.empty() : Optional.of(new ArrayList<>(packages));
    }

    private static void addPackage(Set<String> packages, String value) {
        if (value != null && !value.isBlank()) {
            packages.add(value);
        }
    }
}
