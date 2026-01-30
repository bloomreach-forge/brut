/*
 * Copyright 2024 Bloomreach, Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.brut.resources.annotation;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePatternsSpringConfigTest {

    @Test
    void create_withYamlPatterns_generatesValidXml() throws Exception {
        String configPath = ResourcePatternsSpringConfig.create(
            List.of("classpath*:org/example/imports/**/*.yaml"),
            List.of()
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("contributedYamlResourcesPatterns"));
        assertTrue(xml.contains("classpath*:org/example/imports/**/*.yaml"));
    }

    @Test
    void create_withCndPatterns_generatesValidXml() throws Exception {
        String configPath = ResourcePatternsSpringConfig.create(
            List.of(),
            List.of("classpath*:org/example/namespaces/**/*.cnd")
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("contributedCndResourcesPatterns"));
        assertTrue(xml.contains("classpath*:org/example/namespaces/**/*.cnd"));
    }

    @Test
    void create_withBothPatterns_generatesValidXml() throws Exception {
        String configPath = ResourcePatternsSpringConfig.create(
            List.of("classpath*:org/example/imports/**/*.yaml"),
            List.of("classpath*:org/example/namespaces/**/*.cnd")
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("contributedYamlResourcesPatterns"));
        assertTrue(xml.contains("contributedCndResourcesPatterns"));
        assertTrue(xml.contains("classpath*:org/example/imports/**/*.yaml"));
        assertTrue(xml.contains("classpath*:org/example/namespaces/**/*.cnd"));
    }

    @Test
    void create_withEmptyLists_generatesEmptyBeans() throws Exception {
        String configPath = ResourcePatternsSpringConfig.create(List.of(), List.of());

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("contributedYamlResourcesPatterns"));
        assertTrue(xml.contains("contributedCndResourcesPatterns"));
        assertFalse(xml.contains("<value>"));
    }

    @Test
    void create_generatesValidSpringBeansXml() throws Exception {
        String configPath = ResourcePatternsSpringConfig.create(
            List.of("test.yaml"),
            List.of()
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<beans xmlns=\"http://www.springframework.org/schema/beans\""));
        assertTrue(xml.contains("</beans>"));
    }

    @Test
    void create_returnsTempFileUri() {
        String configPath = ResourcePatternsSpringConfig.create(List.of(), List.of());

        assertTrue(configPath.startsWith("file:"));
        assertTrue(configPath.contains("brut-resource-patterns-"));
        assertTrue(configPath.endsWith(".xml"));
    }
}
