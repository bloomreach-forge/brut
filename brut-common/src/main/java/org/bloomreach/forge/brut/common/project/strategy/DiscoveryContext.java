package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.bloomreach.forge.brut.common.project.ProjectSettings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable context containing all information strategies may need for bean package discovery.
 */
public final class DiscoveryContext {

    private final Path startPath;
    private final Path projectRoot;
    private final String testPackage;
    private final BeanPackageOrder order;
    private final boolean includeDomain;
    private final ProjectSettings projectSettings;

    private DiscoveryContext(Builder builder) {
        this.startPath = builder.startPath;
        this.projectRoot = builder.projectRoot;
        this.testPackage = builder.testPackage;
        this.order = builder.order != null ? builder.order : BeanPackageOrder.BEANS_FIRST;
        this.includeDomain = builder.includeDomain;
        this.projectSettings = builder.projectSettings;
    }

    public Path getStartPath() {
        return startPath;
    }

    public Optional<Path> getProjectRoot() {
        return Optional.ofNullable(projectRoot);
    }

    public Optional<String> getTestPackage() {
        return Optional.ofNullable(testPackage);
    }

    public BeanPackageOrder getOrder() {
        return order;
    }

    public boolean isIncludeDomain() {
        return includeDomain;
    }

    public Optional<ProjectSettings> getProjectSettings() {
        return Optional.ofNullable(projectSettings);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path startPath = Paths.get(System.getProperty("user.dir"));
        private Path projectRoot;
        private String testPackage;
        private BeanPackageOrder order;
        private boolean includeDomain;
        private ProjectSettings projectSettings;

        private Builder() {
        }

        public Builder startPath(Path startPath) {
            this.startPath = Objects.requireNonNull(startPath, "startPath must not be null");
            return this;
        }

        public Builder projectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder testPackage(String testPackage) {
            this.testPackage = testPackage;
            return this;
        }

        public Builder testClass(Class<?> testClass) {
            if (testClass != null && testClass.getPackage() != null) {
                this.testPackage = testClass.getPackage().getName();
            }
            return this;
        }

        public Builder order(BeanPackageOrder order) {
            this.order = order;
            return this;
        }

        public Builder includeDomain(boolean includeDomain) {
            this.includeDomain = includeDomain;
            return this;
        }

        public Builder projectSettings(ProjectSettings projectSettings) {
            this.projectSettings = projectSettings;
            return this;
        }

        public DiscoveryContext build() {
            return new DiscoveryContext(this);
        }
    }
}
