package org.bloomreach.forge.brut.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Resolves opt-in dependency HCM modules from the test classpath.
 *
 * <p>BRUT intentionally avoids broad classpath HCM scanning. This resolver keeps that behavior
 * while allowing tests to opt into specific dependency-provided HCM modules by module name.
 */
final class DependencyHcmModuleResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyHcmModuleResolver.class);
    private static final List<String> HCM_MODULE_DESCRIPTORS = List.of(
        "META-INF/hcm-module.yaml",
        "hcm-module.yaml"
    );

    private DependencyHcmModuleResolver() {
    }

    static List<Path> resolve(List<String> requestedModuleNames, ClassLoader classLoader) throws IOException {
        if (requestedModuleNames == null || requestedModuleNames.isEmpty()) {
            return List.of();
        }

        Set<String> requested = new LinkedHashSet<>();
        for (String requestedModuleName : requestedModuleNames) {
            if (requestedModuleName != null && !requestedModuleName.isBlank()) {
                requested.add(requestedModuleName.trim());
            }
        }
        if (requested.isEmpty()) {
            return List.of();
        }

        ClassLoader effectiveClassLoader = classLoader != null
            ? classLoader
            : Thread.currentThread().getContextClassLoader();

        Map<String, Path> resolved = new HashMap<>();
        for (String descriptorLocation : HCM_MODULE_DESCRIPTORS) {
            Enumeration<URL> resources = effectiveClassLoader.getResources(descriptorLocation);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String moduleName = readModuleName(resource);
                if (moduleName == null || !requested.contains(moduleName) || resolved.containsKey(moduleName)) {
                    continue;
                }

                Path descriptor = materializeDescriptor(resource);
                if (descriptor != null) {
                    resolved.put(moduleName, descriptor.toAbsolutePath().normalize());
                    LOG.info("Resolved dependency HCM module '{}' from {}", moduleName, resource);
                }
            }
        }

        List<Path> descriptors = new ArrayList<>();
        for (String requestedModule : requested) {
            Path descriptor = resolved.get(requestedModule);
            if (descriptor != null) {
                descriptors.add(descriptor);
            } else {
                LOG.warn("Dependency HCM module '{}' was requested but no matching hcm-module.yaml " +
                    "was found on the test classpath", requestedModule);
            }
        }
        return descriptors;
    }

    static List<Path> resolveAll(ClassLoader classLoader,
                                 Set<String> platformGroupPrefixes,
                                 Set<String> projectGroupNames,
                                 Set<String> excludeModuleNames) throws IOException {
        ClassLoader effectiveClassLoader = classLoader != null
            ? classLoader
            : Thread.currentThread().getContextClassLoader();

        // module name → descriptor path; use TreeMap for deterministic ordering
        Map<String, Path> discovered = new TreeMap<>();
        for (String descriptorLocation : HCM_MODULE_DESCRIPTORS) {
            Enumeration<URL> resources = effectiveClassLoader.getResources(descriptorLocation);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String moduleName = readModuleName(resource);
                String groupName = readGroupName(resource);

                if (moduleName == null || discovered.containsKey(moduleName)) {
                    continue;
                }
                if (excludeModuleNames != null && excludeModuleNames.contains(moduleName)) {
                    continue;
                }
                if (isExcludedGroup(groupName, platformGroupPrefixes, projectGroupNames)) {
                    continue;
                }

                Path descriptor = materializeDescriptor(resource);
                if (descriptor != null) {
                    discovered.put(moduleName, descriptor.toAbsolutePath().normalize());
                    LOG.info("Auto-discovered addon HCM module '{}' (group='{}') from {}",
                        moduleName, groupName, resource);
                }
            }
        }
        return List.copyOf(discovered.values());
    }

    private static boolean isExcludedGroup(String groupName,
                                           Set<String> platformPrefixes,
                                           Set<String> projectGroupNames) {
        if (groupName == null) {
            return false;
        }
        if (projectGroupNames != null && projectGroupNames.contains(groupName)) {
            return true;
        }
        if (platformPrefixes != null) {
            for (String prefix : platformPrefixes) {
                if (groupName.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    static String readModuleName(URL descriptorUrl) throws IOException {
        try (InputStream stream = descriptorUrl.openStream()) {
            return readModuleName(stream);
        }
    }

    static String readModuleName(InputStream descriptorStream) throws IOException {
        return readFieldFromBlock(descriptorStream, "module");
    }

    static String readGroupName(URL descriptorUrl) throws IOException {
        try (InputStream stream = descriptorUrl.openStream()) {
            return readGroupName(stream);
        }
    }

    static String readGroupName(InputStream descriptorStream) throws IOException {
        return readFieldFromBlock(descriptorStream, "group");
    }

    private static String readFieldFromBlock(InputStream descriptorStream, String blockKey) throws IOException {
        List<String> lines;
        try (descriptorStream) {
            lines = new String(descriptorStream.readAllBytes()).lines().toList();
        }

        String blockMarker = blockKey + ":";
        boolean inBlock = false;
        int blockIndent = -1;
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int indent = indentation(line);
            if (!inBlock) {
                if (blockMarker.equals(trimmed)) {
                    inBlock = true;
                    blockIndent = indent;
                }
                continue;
            }

            if (indent <= blockIndent) {
                inBlock = false;
                blockIndent = -1;
                if (blockMarker.equals(trimmed)) {
                    inBlock = true;
                    blockIndent = indent;
                }
                continue;
            }

            if (trimmed.startsWith("name:")) {
                return unquote(trimmed.substring("name:".length()).trim());
            }
        }

        return null;
    }

    private static int indentation(String line) {
        int indent = 0;
        while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
            indent++;
        }
        return indent;
    }

    private static String unquote(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Path materializeDescriptor(URL resource) throws IOException {
        try {
            if ("file".equals(resource.getProtocol())) {
                return Paths.get(resource.toURI());
            }
            if ("jar".equals(resource.getProtocol())) {
                return copyDescriptorFromJar(resource);
            }
        } catch (Exception e) {
            throw new IOException("Failed to resolve dependency HCM module from " + resource, e);
        }
        return null;
    }

    private static Path copyDescriptorFromJar(URL resource) throws Exception {
        String url = resource.toString();
        int separator = url.indexOf("!/");
        if (separator < 0) {
            return null;
        }

        URI jarUri = URI.create(url.substring(0, separator));
        String entryPath = url.substring(separator + 2);
        Path tempDir = Files.createTempDirectory("brut-dependency-hcm-");
        tempDir.toFile().deleteOnExit();

        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of())) {
            Path jarRoot = fs.getPath("/");
            try (var entries = Files.list(jarRoot)) {
                for (Path entry : (Iterable<Path>) entries::iterator) {
                    String fileName = entry.getFileName() != null ? entry.getFileName().toString() : "";
                    if (fileName.isBlank()) {
                        continue;
                    }
                    copyPath(entry, tempDir.resolve(fileName));
                }
            }
        }
        Path descriptor = tempDir.resolve(entryPath);
        return Files.exists(descriptor) ? descriptor : null;
    }

    private static void copyPath(Path sourceRoot, Path targetRoot) throws IOException {
        try (var stream = Files.walk(sourceRoot)) {
            for (Path source : (Iterable<Path>) stream::iterator) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative.toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    target.toFile().deleteOnExit();
                }
            }
        }
    }
}
