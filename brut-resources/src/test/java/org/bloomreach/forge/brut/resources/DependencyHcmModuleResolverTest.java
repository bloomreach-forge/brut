package org.bloomreach.forge.brut.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyHcmModuleResolverTest {

    @Test
    void readModuleName_extractsModuleBlockName() throws Exception {
        String yaml = """
            group:
              name: brxm-discovery
            project:
              name: brxm-discovery
            module:
              name: brxm-discovery-cms
            """;

        String moduleName = DependencyHcmModuleResolver.readModuleName(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("brxm-discovery-cms", moduleName);
    }

    @Test
    void resolve_returnsMatchingFileDescriptor(@TempDir Path tempDir) throws Exception {
        Path moduleRoot = tempDir.resolve("dependency-module");
        Path metaInf = moduleRoot.resolve("META-INF");
        Files.createDirectories(metaInf);
        Path descriptor = metaInf.resolve("hcm-module.yaml");
        Files.writeString(descriptor, """
            group:
              name: dep
            project:
              name: dep
            module:
              name: discovery-addon
            """);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{moduleRoot.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolve(List.of("discovery-addon"), classLoader);

            assertEquals(List.of(descriptor.toAbsolutePath().normalize()), resolved);
        }
    }

    @Test
    void resolve_returnsMatchingRootFileDescriptor(@TempDir Path tempDir) throws Exception {
        Path moduleRoot = tempDir.resolve("dependency-module");
        Files.createDirectories(moduleRoot);
        Path descriptor = moduleRoot.resolve("hcm-module.yaml");
        Files.writeString(descriptor, """
            group:
              name: dep
            project:
              name: dep
            module:
              name: discovery-addon
            """);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{moduleRoot.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolve(List.of("discovery-addon"), classLoader);

            assertEquals(List.of(descriptor.toAbsolutePath().normalize()), resolved);
        }
    }

    @Test
    void resolve_copiesMatchingJarBackedModule(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("dependency-addon.jar");
        writeJarModule(jarPath,
            "hcm-module.yaml",
            """
                group:
                  name: dep
                project:
                  name: dep
                module:
                  name: brxm-discovery-cms
                """,
            "hcm-config/brxdis-types.cnd",
            "<'brxdis'='http://example.com/discovery/nt/1.0'>");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolve(List.of("brxm-discovery-cms"), classLoader);

            assertEquals(1, resolved.size());
            Path descriptor = resolved.get(0);
            assertTrue(Files.exists(descriptor));
            assertTrue(Files.exists(descriptor.getParent().resolve("hcm-config/brxdis-types.cnd")));
        }
    }

    @Test
    void resolve_copiesMatchingMetaInfJarBackedModule(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("dependency-addon.jar");
        writeJarModule(jarPath,
            "META-INF/hcm-module.yaml",
            """
                group:
                  name: dep
                project:
                  name: dep
                module:
                  name: brxm-discovery-cms
                """,
            "hcm-config/brxdis-types.cnd",
            "<'brxdis'='http://example.com/discovery/nt/1.0'>");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolve(List.of("brxm-discovery-cms"), classLoader);

            assertEquals(1, resolved.size());
            Path descriptor = resolved.get(0);
            assertTrue(Files.exists(descriptor));
            assertTrue(Files.exists(descriptor.getParent().getParent().resolve("hcm-config/brxdis-types.cnd")));
        }
    }

    @Test
    void resolve_preservesRequestedOrder(@TempDir Path tempDir) throws Exception {
        Path firstJar = tempDir.resolve("first.jar");
        Path secondJar = tempDir.resolve("second.jar");
        writeJarModule(firstJar, "hcm-module.yaml", """
                group:
                  name: dep
                project:
                  name: dep
                module:
                  name: module-a
                """, "hcm-config/a.yaml", "definitions: {}");
        writeJarModule(secondJar, "hcm-module.yaml", """
                group:
                  name: dep
                project:
                  name: dep
                module:
                  name: module-b
                """, "hcm-config/b.yaml", "definitions: {}");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{
            secondJar.toUri().toURL(),
            firstJar.toUri().toURL()
        }, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolve(List.of("module-a", "module-b"), classLoader);

            assertEquals(2, resolved.size());
            assertEquals("module-a", DependencyHcmModuleResolver.readModuleName(Files.newInputStream(resolved.get(0))));
            assertEquals("module-b", DependencyHcmModuleResolver.readModuleName(Files.newInputStream(resolved.get(1))));
        }
    }

    @Test
    void readGroupName_extractsGroupBlockName() throws Exception {
        String yaml = """
            group:
              name: brxm-discovery
            project:
              name: brxm-discovery
            module:
              name: brxm-discovery-cms
            """;

        String groupName = DependencyHcmModuleResolver.readGroupName(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("brxm-discovery", groupName);
    }

    @Test
    void readGroupName_returnsNullWhenNoGroupBlock() throws Exception {
        String yaml = """
            module:
              name: some-module
            """;

        String groupName = DependencyHcmModuleResolver.readGroupName(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertNull(groupName);
    }

    @Test
    void resolveAll_excludesPlatformGroups(@TempDir Path tempDir) throws Exception {
        Path hippoJar = tempDir.resolve("hippo-cms.jar");
        Path addonJar = tempDir.resolve("addon.jar");
        writeJarModule(hippoJar, "hcm-module.yaml",
            """
            group:
              name: hippo-cms
            module:
              name: hippo-cms
            """, "hcm-config/a.yaml", "definitions: {}");
        writeJarModule(addonJar, "hcm-module.yaml",
            """
            group:
              name: brxm-discovery
            module:
              name: brxm-discovery-cms
            """, "hcm-config/b.yaml", "definitions: {}");

        try (URLClassLoader cl = new URLClassLoader(new URL[]{hippoJar.toUri().toURL(), addonJar.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolveAll(
                cl, Set.of("hippo", "onehippo"), Set.of(), Set.of());

            assertEquals(1, resolved.size());
            assertEquals("brxm-discovery-cms",
                DependencyHcmModuleResolver.readModuleName(Files.newInputStream(resolved.get(0))));
        }
    }

    @Test
    void resolveAll_excludesProjectGroups(@TempDir Path tempDir) throws Exception {
        Path projectJar = tempDir.resolve("project.jar");
        Path addonJar = tempDir.resolve("addon.jar");
        writeJarModule(projectJar, "hcm-module.yaml",
            """
            group:
              name: petbase
            module:
              name: petbase-site
            """, "hcm-config/p.yaml", "definitions: {}");
        writeJarModule(addonJar, "hcm-module.yaml",
            """
            group:
              name: brxm-discovery
            module:
              name: brxm-discovery-cms
            """, "hcm-config/b.yaml", "definitions: {}");

        try (URLClassLoader cl = new URLClassLoader(new URL[]{projectJar.toUri().toURL(), addonJar.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolveAll(
                cl, Set.of("hippo", "onehippo"), Set.of("petbase"), Set.of());

            assertEquals(1, resolved.size());
            assertEquals("brxm-discovery-cms",
                DependencyHcmModuleResolver.readModuleName(Files.newInputStream(resolved.get(0))));
        }
    }

    @Test
    void resolveAll_honorsExcludeModuleNames(@TempDir Path tempDir) throws Exception {
        Path addonJar = tempDir.resolve("addon.jar");
        writeJarModule(addonJar, "hcm-module.yaml",
            """
            group:
              name: brxm-discovery
            module:
              name: brxm-discovery-cms
            """, "hcm-config/b.yaml", "definitions: {}");

        try (URLClassLoader cl = new URLClassLoader(new URL[]{addonJar.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolveAll(
                cl, Set.of("hippo", "onehippo"), Set.of(), Set.of("brxm-discovery-cms"));

            assertTrue(resolved.isEmpty());
        }
    }

    @Test
    void resolveAll_returnsEmptyWhenNoAddons(@TempDir Path tempDir) throws Exception {
        Path hippoJar = tempDir.resolve("hippo.jar");
        writeJarModule(hippoJar, "hcm-module.yaml",
            """
            group:
              name: hippo-cms
            module:
              name: hippo-cms
            """, "hcm-config/a.yaml", "definitions: {}");

        try (URLClassLoader cl = new URLClassLoader(new URL[]{hippoJar.toUri().toURL()}, null)) {
            List<Path> resolved = DependencyHcmModuleResolver.resolveAll(
                cl, Set.of("hippo", "onehippo"), Set.of(), Set.of());

            assertTrue(resolved.isEmpty());
        }
    }

    private void writeJarModule(Path jarPath,
                                String descriptorEntryName,
                                String descriptorContent,
                                String extraEntryName,
                                String extraEntryContent)
        throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath))) {
            out.putNextEntry(new JarEntry(descriptorEntryName));
            out.write(descriptorContent.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();

            out.putNextEntry(new JarEntry(extraEntryName));
            out.write(extraEntryContent.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }
}
