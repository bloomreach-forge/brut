package org.bloomreach.forge.brut.resources.annotation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ConfigServiceSpringConfig {

    static String create(String projectNamespace,
                         List<String> cndPatterns,
                         List<String> yamlPatterns,
                         List<String> repositoryDataModules) {
        String namespace = projectNamespace != null && !projectNamespace.isBlank()
            ? projectNamespace
            : "project";

        String xml = buildXml(namespace, cndPatterns, yamlPatterns, repositoryDataModules);
        try {
            Path tempFile = Files.createTempFile("brut-configservice-", ".xml");
            Files.writeString(tempFile, xml, StandardCharsets.UTF_8);
            tempFile.toFile().deleteOnExit();
            return tempFile.toUri().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create ConfigService spring configuration", e);
        }
    }

    private static String buildXml(String projectNamespace,
                                   List<String> cndPatterns,
                                   List<String> yamlPatterns,
                                   List<String> repositoryDataModules) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<beans xmlns=\"http://www.springframework.org/schema/beans\"\n");
        builder.append("       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        builder.append("       xsi:schemaLocation=\"http://www.springframework.org/schema/beans ");
        builder.append("http://www.springframework.org/schema/beans/spring-beans-4.1.xsd\">\n\n");

        appendListBean(builder, "contributedCndResourcesPatterns", cndPatterns);
        appendListBean(builder, "contributedYamlResourcesPatterns", yamlPatterns);
        appendListBean(builder, "repositoryDataModules", repositoryDataModules);

        builder.append("  <bean id=\"javax.jcr.Repository\"\n");
        builder.append("        class=\"org.bloomreach.forge.brut.resources.ConfigServiceRepository\"\n");
        builder.append("        init-method=\"init\"\n");
        builder.append("        destroy-method=\"close\">\n");
        builder.append("    <constructor-arg ref=\"cndResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg ref=\"contributedCndResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg ref=\"yamlResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg ref=\"contributedYamlResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg value=\"").append(escapeXml(projectNamespace)).append("\"/>\n");
        builder.append("    <property name=\"additionalRepositoryModules\" ref=\"repositoryDataModules\"/>\n");
        builder.append("  </bean>\n");
        builder.append("</beans>\n");

        return builder.toString();
    }

    private static void appendListBean(StringBuilder builder, String beanId, List<String> values) {
        builder.append("  <bean id=\"").append(beanId).append("\" class=\"java.util.ArrayList\">\n");
        builder.append("    <constructor-arg>\n");
        builder.append("      <list>\n");
        if (values != null) {
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                builder.append("        <value>").append(escapeXml(value)).append("</value>\n");
            }
        }
        builder.append("      </list>\n");
        builder.append("    </constructor-arg>\n");
        builder.append("  </bean>\n\n");
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
