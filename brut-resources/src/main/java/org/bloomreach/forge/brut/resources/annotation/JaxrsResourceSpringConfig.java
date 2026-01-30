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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates Spring XML configuration for JAX-RS resources at runtime.
 * Eliminates the need for verbose Spring XML files by creating them programmatically.
 */
class JaxrsResourceSpringConfig {

    private static final String XML_HEADER = """
        <?xml version="1.0" encoding="UTF-8"?>
        <beans xmlns="http://www.springframework.org/schema/beans"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.springframework.org/schema/beans
               http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">

        """;

    private static final String JACKSON_IMPORT =
        "  <import resource=\"classpath:/org/hippoecm/hst/site/optional/jaxrs/SpringComponentManager-rest-jackson.xml\"/>\n\n";

    private static final String XML_FOOTER = "</beans>\n";

    static String create(List<Class<?>> resourceClasses) {
        String xml = buildXml(resourceClasses);
        try {
            Path tempFile = Files.createTempFile("brut-jaxrs-resources-", ".xml");
            Files.writeString(tempFile, xml, StandardCharsets.UTF_8);
            tempFile.toFile().deleteOnExit();
            return tempFile.toUri().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create JAX-RS resource spring configuration", e);
        }
    }

    private static String buildXml(List<Class<?>> resourceClasses) {
        StringBuilder builder = new StringBuilder();
        builder.append(XML_HEADER);
        builder.append(JACKSON_IMPORT);
        appendResourceProviders(builder, resourceClasses);
        builder.append(XML_FOOTER);
        return builder.toString();
    }

    private static void appendResourceProviders(StringBuilder builder, List<Class<?>> resourceClasses) {
        builder.append("  <bean id=\"customRestPlainResourceProviders\"\n");
        builder.append("        class=\"org.springframework.beans.factory.config.ListFactoryBean\">\n");
        builder.append("    <property name=\"sourceList\">\n");
        builder.append("      <list>\n");

        for (Class<?> resourceClass : resourceClasses) {
            builder.append("        <bean class=\"org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider\">\n");
            builder.append("          <constructor-arg>\n");
            builder.append("            <bean class=\"").append(resourceClass.getName()).append("\" />\n");
            builder.append("          </constructor-arg>\n");
            builder.append("        </bean>\n");
        }

        builder.append("      </list>\n");
        builder.append("    </property>\n");
        builder.append("  </bean>\n");
    }
}
