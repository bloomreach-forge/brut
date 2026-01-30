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
 * Utility for generating Spring XML configuration files at runtime.
 */
final class SpringXmlGenerator {

    static final String XML_HEADER = """
        <?xml version="1.0" encoding="UTF-8"?>
        <beans xmlns="http://www.springframework.org/schema/beans"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.springframework.org/schema/beans
               http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

        """;

    static final String XML_FOOTER = "</beans>\n";

    private SpringXmlGenerator() {
    }

    static String createTempConfig(String prefix, String xmlContent) {
        try {
            Path tempFile = Files.createTempFile(prefix, ".xml");
            Files.writeString(tempFile, xmlContent, StandardCharsets.UTF_8);
            tempFile.toFile().deleteOnExit();
            return tempFile.toUri().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create spring configuration: " + prefix, e);
        }
    }

    static void appendListBean(StringBuilder builder, String beanId, List<String> values) {
        builder.append("  <bean id=\"").append(beanId).append("\" class=\"java.util.ArrayList\">\n");
        builder.append("    <constructor-arg>\n");
        builder.append("      <list>\n");
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    builder.append("        <value>").append(escapeXml(value)).append("</value>\n");
                }
            }
        }
        builder.append("      </list>\n");
        builder.append("    </constructor-arg>\n");
        builder.append("  </bean>\n\n");
    }

    static String escapeXml(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
