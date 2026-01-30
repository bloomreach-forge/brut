package org.bloomreach.forge.brut.common.project.strategy;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Strategy that scans the classpath for classes annotated with @Node
 * and extracts their packages as bean package candidates.
 * Priority: 20
 *
 * <p>Scans bounded prefixes (com.*, org.*) with common bean package
 * suffixes (beans, model, domain) to limit scanning scope.</p>
 */
public final class ClasspathNodeAnnotationStrategy implements BeanPackageStrategy {

    public static final int PRIORITY = 20;

    private static final String NODE_ANNOTATION_CLASS = "org.hippoecm.hst.content.beans.Node";
    private static final List<String> SCAN_SUFFIXES = List.of("beans", "model", "domain");
    private static final List<String> SCAN_PREFIXES = List.of("com", "org", "nl", "net");

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Optional<List<String>> resolve(DiscoveryContext context) {
        Set<String> foundPackages = new LinkedHashSet<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Class<? extends Annotation> nodeAnnotation = loadNodeAnnotation(classLoader);
        if (nodeAnnotation == null) {
            return Optional.empty();
        }

        for (String prefix : SCAN_PREFIXES) {
            for (String suffix : SCAN_SUFFIXES) {
                scanPackagesWithSuffix(classLoader, prefix, suffix, nodeAnnotation, foundPackages);
            }
        }

        if (foundPackages.isEmpty()) {
            return Optional.empty();
        }

        List<String> packages = orderPackages(foundPackages, context);
        return Optional.of(packages);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadNodeAnnotation(ClassLoader classLoader) {
        try {
            return (Class<? extends Annotation>) classLoader.loadClass(NODE_ANNOTATION_CLASS);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void scanPackagesWithSuffix(ClassLoader classLoader, String prefix, String suffix,
                                        Class<? extends Annotation> nodeAnnotation,
                                        Set<String> foundPackages) {
        try {
            String resourcePath = prefix.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(resourcePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    scanDirectory(new File(resource.toURI()), prefix, suffix, nodeAnnotation, foundPackages);
                }
            }
        } catch (Exception ignored) {
            // Continue with other prefixes
        }
    }

    private void scanDirectory(File baseDir, String basePackage, String targetSuffix,
                               Class<? extends Annotation> nodeAnnotation,
                               Set<String> foundPackages) {
        if (!baseDir.isDirectory()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(baseDir.toPath(), 10)) {
            paths.filter(path -> path.toFile().isDirectory())
                 .filter(path -> path.getFileName().toString().equals(targetSuffix))
                 .forEach(suffixDir -> scanClassesInPackage(suffixDir, baseDir.toPath(),
                         basePackage, nodeAnnotation, foundPackages));
        } catch (IOException ignored) {
            // Continue with other directories
        }
    }

    private void scanClassesInPackage(Path packageDir, Path baseDir, String basePackage,
                                      Class<? extends Annotation> nodeAnnotation,
                                      Set<String> foundPackages) {
        try (Stream<Path> classFiles = Files.list(packageDir)) {
            boolean hasNodeAnnotatedClass = classFiles
                    .filter(path -> path.toString().endsWith(".class"))
                    .anyMatch(classFile -> isNodeAnnotated(classFile, baseDir, basePackage, nodeAnnotation));

            if (hasNodeAnnotatedClass) {
                String packageName = toPackageName(packageDir, baseDir, basePackage);
                foundPackages.add(packageName);
            }
        } catch (IOException ignored) {
            // Continue
        }
    }

    private boolean isNodeAnnotated(Path classFile, Path baseDir, String basePackage,
                                    Class<? extends Annotation> nodeAnnotation) {
        String className = toClassName(classFile, baseDir, basePackage);
        if (className == null) {
            return false;
        }

        try {
            Class<?> clazz = Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());
            return clazz.isAnnotationPresent(nodeAnnotation);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return false;
        }
    }

    private String toClassName(Path classFile, Path baseDir, String basePackage) {
        Path relativePath = baseDir.relativize(classFile);
        String pathString = relativePath.toString();
        if (!pathString.endsWith(".class")) {
            return null;
        }
        return basePackage + "." + pathString
                .substring(0, pathString.length() - ".class".length())
                .replace(File.separatorChar, '.');
    }

    private String toPackageName(Path packageDir, Path baseDir, String basePackage) {
        Path relativePath = baseDir.relativize(packageDir);
        return basePackage + "." + relativePath.toString().replace(File.separatorChar, '.');
    }

    private List<String> orderPackages(Set<String> packages, DiscoveryContext context) {
        List<String> beans = new ArrayList<>();
        List<String> model = new ArrayList<>();
        List<String> domain = new ArrayList<>();
        List<String> other = new ArrayList<>();

        for (String pkg : packages) {
            if (pkg.endsWith(".beans")) {
                beans.add(pkg);
            } else if (pkg.endsWith(".model")) {
                model.add(pkg);
            } else if (pkg.endsWith(".domain")) {
                domain.add(pkg);
            } else {
                other.add(pkg);
            }
        }

        List<String> result = new ArrayList<>();
        if (context.getOrder() == org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder.MODEL_FIRST) {
            result.addAll(model);
            result.addAll(beans);
        } else {
            result.addAll(beans);
            result.addAll(model);
        }

        if (context.isIncludeDomain()) {
            result.addAll(domain);
        }
        result.addAll(other);

        return result;
    }
}
