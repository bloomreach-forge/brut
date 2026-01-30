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

import client.packagename.rest.HelloResource;
import client.packagename.rest.NewsResource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JaxrsResourceSpringConfigTest {

    @Test
    void create_withSingleResource_generatesValidXml() throws Exception {
        String configPath = JaxrsResourceSpringConfig.create(
            List.of(HelloResource.class)
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("customRestPlainResourceProviders"));
        assertTrue(xml.contains("client.packagename.rest.HelloResource"));
        assertTrue(xml.contains("SingletonResourceProvider"));
    }

    @Test
    void create_withMultipleResources_registersAll() throws Exception {
        String configPath = JaxrsResourceSpringConfig.create(
            List.of(HelloResource.class, NewsResource.class)
        );

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("client.packagename.rest.HelloResource"));
        assertTrue(xml.contains("client.packagename.rest.NewsResource"));
    }

    @Test
    void create_withEmptyList_generatesEmptyProviderList() throws Exception {
        String configPath = JaxrsResourceSpringConfig.create(List.of());

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("customRestPlainResourceProviders"));
        assertFalse(xml.contains("SingletonResourceProvider"));
    }

    @Test
    void create_importsJacksonSupport() throws Exception {
        String configPath = JaxrsResourceSpringConfig.create(List.of(HelloResource.class));

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.contains("SpringComponentManager-rest-jackson.xml"));
    }

    @Test
    void create_generatesValidSpringBeansXml() throws Exception {
        String configPath = JaxrsResourceSpringConfig.create(List.of(HelloResource.class));

        String xml = Files.readString(Path.of(new URI(configPath)));
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<beans xmlns=\"http://www.springframework.org/schema/beans\""));
        assertTrue(xml.contains("</beans>"));
    }

    @Test
    void create_returnsTempFileUri() {
        String configPath = JaxrsResourceSpringConfig.create(List.of(HelloResource.class));

        assertTrue(configPath.startsWith("file:"));
        assertTrue(configPath.contains("brut-jaxrs-resources-"));
        assertTrue(configPath.endsWith(".xml"));
    }
}
