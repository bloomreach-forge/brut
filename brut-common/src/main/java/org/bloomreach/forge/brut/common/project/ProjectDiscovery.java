package org.bloomreach.forge.brut.common.project;

import org.bloomreach.forge.brut.common.project.strategy.BeanPackageStrategyChain;
import org.bloomreach.forge.brut.common.project.strategy.DiscoveryContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class ProjectDiscovery {

    public enum BeanPackageOrder {
        BEANS_FIRST,
        MODEL_FIRST
    }

    private static final List<String> DEFAULT_REPOSITORY_MODULES = List.of(
        "application",
        "site",
        "development",
        "site-development",
        "webfiles"
    );

    private static final String PROJECT_SETTINGS_FILENAME = "project-settings.xml";

    private ProjectDiscovery() {
    }

    public static Optional<Path> findProjectRoot(Path start) {
        if (start == null) {
            return Optional.empty();
        }

        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (isProjectRootCandidate(current)) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    public static Optional<ProjectSettings> loadProjectSettings(Path start) {
        Optional<Path> projectRoot = findProjectRoot(start);
        if (projectRoot.isEmpty()) {
            return Optional.empty();
        }
        Optional<Path> settingsPath = findProjectSettingsPath(projectRoot.get());
        return settingsPath.flatMap(ProjectSettingsReader::read);
    }

    public static String resolveHstRoot(Path start) {
        ProjectSettings settings = loadProjectSettings(start).orElse(null);
        if (settings != null && settings.getHstRoot() != null) {
            return normalizeHstRoot(settings.getHstRoot());
        }

        String projectName = resolveProjectNameFromHcmModule(start);
        if (projectName == null) {
            projectName = resolveProjectNameFromPom(start);
        }
        if (projectName == null) {
            projectName = resolveProjectNameFromDirectory(start);
        }
        return normalizeHstRoot(projectName);
    }

    public static String resolveProjectNamespace(Path start) {
        ProjectSettings settings = loadProjectSettings(start).orElse(null);
        if (settings != null && settings.getProjectNamespace() != null) {
            return settings.getProjectNamespace();
        }

        String namespace = resolveNamespaceFromCnd(start);
        if (namespace != null) {
            return namespace;
        }

        String projectName = resolveProjectNameFromHcmModule(start);
        if (projectName != null) {
            return projectName;
        }

        projectName = resolveProjectNameFromPom(start);
        if (projectName != null) {
            return projectName;
        }

        return resolveProjectNameFromDirectory(start);
    }

    public static List<String> resolveBeanPackages(Class<?> testClass,
                                                   BeanPackageOrder order,
                                                   boolean includeDomain) {
        Path start = Paths.get(System.getProperty("user.dir"));
        return resolveBeanPackages(start, testClass, order, includeDomain);
    }

    public static List<String> resolveBeanPackages(Path start,
                                                   String testPackage,
                                                   BeanPackageOrder order,
                                                   boolean includeDomain) {
        return resolveBeanPackagesWithContext(start, testPackage, order, includeDomain);
    }

    public static List<String> resolveBeanPackages(Path start,
                                                   Class<?> testClass,
                                                   BeanPackageOrder order,
                                                   boolean includeDomain) {
        String testPackage = testClass != null && testClass.getPackage() != null
            ? testClass.getPackage().getName()
            : null;
        return resolveBeanPackagesWithContext(start, testPackage, order, includeDomain);
    }

    private static List<String> resolveBeanPackagesWithContext(Path start,
                                                               String testPackage,
                                                               BeanPackageOrder order,
                                                               boolean includeDomain) {
        Optional<Path> projectRoot = findProjectRoot(start);
        ProjectSettings settings = projectRoot.isPresent()
            ? loadProjectSettings(projectRoot.get()).orElse(null)
            : null;

        DiscoveryContext context = DiscoveryContext.builder()
            .startPath(start)
            .projectRoot(projectRoot.orElse(null))
            .testPackage(testPackage)
            .order(order)
            .includeDomain(includeDomain)
            .projectSettings(settings)
            .build();

        return BeanPackageStrategyChain.createDefault().resolve(context);
    }

    public static List<String> toClasspathPatterns(List<String> packages, boolean recursive, boolean trailingComma) {
        List<String> patterns = new ArrayList<>();
        if (packages == null) {
            return patterns;
        }
        String suffix = recursive ? "/**/*.class" : "/*.class";
        for (String pkg : packages) {
            if (pkg == null || pkg.isBlank()) {
                continue;
            }
            String pattern = "classpath*:" + pkg.replace('.', '/') + suffix;
            patterns.add(trailingComma ? pattern + "," : pattern);
        }
        return patterns;
    }

    public static List<Path> discoverRepositoryModuleDescriptors(Path start) {
        return discoverRepositoryModuleDescriptors(start, List.of());
    }

    public static List<Path> discoverRepositoryModuleDescriptors(Path start, List<String> requestedModules) {
        Path projectRoot = findProjectRoot(start).orElse(start.toAbsolutePath().normalize());
        ProjectSettings settings = loadProjectSettings(projectRoot).orElse(null);
        String repositoryDataModule = settings != null && settings.getRepositoryDataModule() != null
            ? settings.getRepositoryDataModule()
            : "repository-data";

        Path repositoryDataRoot = projectRoot.resolve(repositoryDataModule);
        if (!Files.isDirectory(repositoryDataRoot)) {
            return List.of();
        }

        Set<String> moduleNames = new LinkedHashSet<>();

        // If explicit modules are requested, use ONLY those (exclusive filter)
        if (requestedModules != null && !requestedModules.isEmpty()) {
            for (String module : requestedModules) {
                addModuleName(moduleNames, module);
            }
        } else {
            // Auto-discover from settings and defaults
            if (settings != null) {
                addModuleName(moduleNames, settings.getApplicationSubModule());
                addModuleName(moduleNames, settings.getSiteModule());
                addModuleName(moduleNames, settings.getDevelopmentSubModule());
                if (settings.getDevelopmentSubModule() != null) {
                    addModuleName(moduleNames, "site-" + settings.getDevelopmentSubModule());
                }
                addModuleName(moduleNames, settings.getWebfilesSubModule());
            }
            moduleNames.addAll(DEFAULT_REPOSITORY_MODULES);
        }

        Set<Path> descriptors = new LinkedHashSet<>();
        for (String moduleName : moduleNames) {
            Path moduleRoot = repositoryDataRoot.resolve(moduleName);
            addModuleDescriptor(descriptors, moduleRoot);
        }

        return new ArrayList<>(descriptors);
    }

    private static void addModuleDescriptor(Set<Path> descriptors, Path moduleRoot) {
        if (moduleRoot == null) {
            return;
        }
        resolveCompiledOrSourcePath(moduleRoot, "hcm-module.yaml")
            .ifPresent(path -> descriptors.add(path.toAbsolutePath().normalize()));
    }

    private static Optional<Path> resolveCompiledOrSourcePath(Path moduleRoot, String relativePath) {
        Path compiled = moduleRoot.resolve("target/classes/" + relativePath);
        if (Files.exists(compiled)) {
            return Optional.of(compiled);
        }
        Path source = moduleRoot.resolve("src/main/resources/" + relativePath);
        if (Files.exists(source)) {
            return Optional.of(source);
        }
        return Optional.empty();
    }

    private static Optional<Path> findProjectSettingsPath(Path projectRoot) {
        Path essentialsSettings = projectRoot.resolve("essentials/src/main/resources/" + PROJECT_SETTINGS_FILENAME);
        if (Files.exists(essentialsSettings)) {
            return Optional.of(essentialsSettings);
        }

        Path defaultSettings = projectRoot.resolve("src/main/resources/" + PROJECT_SETTINGS_FILENAME);
        if (Files.exists(defaultSettings)) {
            return Optional.of(defaultSettings);
        }

        try (Stream<Path> stream = Files.find(projectRoot, 5,
            (path, attrs) -> PROJECT_SETTINGS_FILENAME.equals(path.getFileName().toString()))) {
            return stream.findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static boolean isProjectRootCandidate(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        Path repositoryData = dir.resolve("repository-data");
        Path essentialsSettings = dir.resolve("essentials/src/main/resources/" + PROJECT_SETTINGS_FILENAME);
        return Files.isDirectory(repositoryData) || Files.exists(essentialsSettings);
    }

    private static String resolveProjectNameFromHcmModule(Path start) {
        Path projectRoot = findProjectRoot(start).orElse(start.toAbsolutePath().normalize());
        ProjectSettings settings = loadProjectSettings(projectRoot).orElse(null);

        String repositoryDataModule = settings != null && settings.getRepositoryDataModule() != null
            ? settings.getRepositoryDataModule()
            : "repository-data";
        String siteModule = settings != null && settings.getSiteModule() != null
            ? settings.getSiteModule()
            : "site";

        Path siteModuleRoot = projectRoot.resolve(repositoryDataModule).resolve(siteModule);

        return resolveCompiledOrSourcePath(siteModuleRoot, "hcm-module.yaml")
            .map(ProjectDiscovery::parseProjectNameFromHcmModule)
            .orElse(null);
    }

    private static String parseProjectNameFromHcmModule(Path hcmModulePath) {
        try (BufferedReader reader = Files.newBufferedReader(hcmModulePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("project:")) {
                    String value = trimmed.substring("project:".length()).trim();
                    return stripQuotes(value);
                }
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private static String resolveNamespaceFromCnd(Path start) {
        Path projectRoot = findProjectRoot(start).orElse(start.toAbsolutePath().normalize());
        ProjectSettings settings = loadProjectSettings(projectRoot).orElse(null);
        String repositoryDataModule = settings != null && settings.getRepositoryDataModule() != null
            ? settings.getRepositoryDataModule()
            : "repository-data";

        Path searchRoot = projectRoot.resolve(repositoryDataModule);
        if (!Files.exists(searchRoot)) {
            searchRoot = projectRoot;
        }

        try (Stream<Path> stream = Files.walk(searchRoot, 8)) {
            return stream.filter(path -> path.toString().endsWith(".cnd"))
                .map(ProjectDiscovery::parseNamespaceFromCnd)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String parseNamespaceFromCnd(Path cndFile) {
        try (BufferedReader reader = Files.newBufferedReader(cndFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("<") && trimmed.contains("=")) {
                    int equalsIndex = trimmed.indexOf('=');
                    if (equalsIndex > 1) {
                        return trimmed.substring(1, equalsIndex).trim();
                    }
                }
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private static String resolveProjectNameFromPom(Path start) {
        Path projectRoot = findProjectRoot(start).orElse(start.toAbsolutePath().normalize());
        Path pom = projectRoot.resolve("pom.xml");
        if (!Files.exists(pom)) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(pom, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("<artifactId>") && trimmed.endsWith("</artifactId>")) {
                    return trimmed.replace("<artifactId>", "").replace("</artifactId>", "").trim();
                }
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private static String resolveProjectNameFromDirectory(Path start) {
        Path projectRoot = findProjectRoot(start).orElse(start.toAbsolutePath().normalize());
        Path fileName = projectRoot.getFileName();
        return fileName != null ? fileName.toString() : "project";
    }

    private static String normalizeHstRoot(String hstRoot) {
        if (hstRoot == null || hstRoot.isBlank()) {
            return "/hst:project";
        }
        if (hstRoot.startsWith("/hst:")) {
            return hstRoot;
        }
        return "/hst:" + hstRoot;
    }

    private static void addPackage(Set<String> packages, String value) {
        if (value != null && !value.isBlank()) {
            packages.add(value);
        }
    }

    private static void addModuleName(Set<String> names, String value) {
        if (value != null && !value.isBlank()) {
            names.add(value);
        }
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
