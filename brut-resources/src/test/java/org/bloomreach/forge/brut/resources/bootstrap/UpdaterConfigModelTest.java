package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.parser.ModuleReader;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdaterConfigModelTest {

    @Test
    void updaterRegistryDefinitionsBuildWithMinimalFramework(@TempDir Path tempDir) throws Exception {
        ModuleReader moduleReader = new ModuleReader();
        ModuleImpl minimalFramework = moduleReader.read(resolveMinimalFrameworkModule(), false).getModule();

        Path moduleRoot = tempDir.resolve("project-module");
        Files.createDirectories(moduleRoot);
        Files.writeString(moduleRoot.resolve("hcm-module.yaml"),
            "group:\n  name: test-group\nproject: test-project\nmodule:\n  name: test-module\n");

        Path configRoot = moduleRoot.resolve("hcm-config/configuration/update/registry");
        Files.createDirectories(configRoot);
        Files.writeString(configRoot.resolve("test-updater.yaml"),
            "definitions:\n" +
                "  config:\n" +
                "    /hippo:configuration/hippo:update/hippo:registry/Test Updater:\n" +
                "      jcr:primaryType: hipposys:updaterinfo\n" +
                "      hipposys:description: \"test\"\n" +
                "      hipposys:loglevel: INFO\n");

        ModuleImpl projectModule = moduleReader.read(moduleRoot.resolve("hcm-module.yaml"), false).getModule();

        ConfigurationModelImpl model = new ConfigurationModelImpl();
        model.addModule(minimalFramework);
        model.addModule(projectModule);

        assertDoesNotThrow(model::build);
    }

    private Path resolveMinimalFrameworkModule() throws Exception {
        String resourcePath = "org/bloomreach/forge/brut/resources/config-service/minimal-framework/hcm-module.yaml";
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        assertNotNull(resource, "Minimal framework module not found on classpath");
        if ("file".equals(resource.getProtocol())) {
            return Paths.get(resource.toURI());
        }
        if ("jar".equals(resource.getProtocol())) {
            return copyModuleFromJar(resource);
        }
        throw new IllegalStateException("Unsupported resource protocol: " + resource.getProtocol());
    }

    private Path copyModuleFromJar(URL resource) throws Exception {
        String url = resource.toString();
        int separator = url.indexOf("!/");
        if (separator < 0) {
            throw new IllegalStateException("Invalid jar URL: " + url);
        }
        URI jarUri = URI.create(url.substring(0, separator));
        String entryPath = url.substring(separator + 2);
        Path tempDir = Files.createTempDirectory("brut-minimal-framework-test");
        tempDir.toFile().deleteOnExit();
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, java.util.Map.of())) {
            Path sourceRoot = fs.getPath(entryPath).getParent();
            if (sourceRoot == null) {
                throw new IllegalStateException("Missing source root for resource: " + url);
            }
            copyDirectory(sourceRoot, tempDir);
        }
        return tempDir.resolve("hcm-module.yaml");
    }

    private void copyDirectory(Path sourceRoot, Path targetRoot) throws Exception {
        try (var stream = Files.walk(sourceRoot)) {
            for (Path source : (Iterable<Path>) stream::iterator) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative.toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target);
                    target.toFile().deleteOnExit();
                }
            }
        }
    }
}
