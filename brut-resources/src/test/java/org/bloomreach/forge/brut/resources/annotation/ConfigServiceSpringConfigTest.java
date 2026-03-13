package org.bloomreach.forge.brut.resources.annotation;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigServiceSpringConfigTest {

    @Test
    void create_includesDependencyHcmModulesBeanAndProperty() throws Exception {
        String configPath = ConfigServiceSpringConfig.create(
            "myproject",
            List.of("classpath*:org/example/namespaces/*.cnd"),
            List.of("classpath*:org/example/imports/*.yaml"),
            List.of("application"),
            List.of("brxm-discovery-cms"),
            List.of()
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("repositoryDataModules"));
        assertTrue(xml.contains("dependencyHcmModules"));
        assertTrue(xml.contains("brxm-discovery-cms"));
        assertTrue(xml.contains("<property name=\"dependencyHcmModules\" ref=\"dependencyHcmModules\"/>"));
    }

    @Test
    void create_includesExcludeDependencyHcmModulesBeanAndProperty() throws Exception {
        String configPath = ConfigServiceSpringConfig.create(
            "myproject",
            List.of("classpath*:org/example/namespaces/*.cnd"),
            List.of("classpath*:org/example/imports/*.yaml"),
            List.of("application"),
            List.of(),
            List.of("some-conflicting-module")
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("excludeDependencyHcmModules"));
        assertTrue(xml.contains("some-conflicting-module"));
        assertTrue(xml.contains("<property name=\"excludeDependencyHcmModules\" ref=\"excludeDependencyHcmModules\"/>"));
    }
}
