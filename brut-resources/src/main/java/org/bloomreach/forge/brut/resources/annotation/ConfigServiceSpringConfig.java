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
        return SpringXmlGenerator.createTempConfig("brut-configservice-", xml);
    }

    private static String buildXml(String projectNamespace,
                                   List<String> cndPatterns,
                                   List<String> yamlPatterns,
                                   List<String> repositoryDataModules) {
        StringBuilder builder = new StringBuilder();
        builder.append(SpringXmlGenerator.XML_HEADER);

        SpringXmlGenerator.appendListBean(builder, "contributedCndResourcesPatterns", cndPatterns);
        SpringXmlGenerator.appendListBean(builder, "contributedYamlResourcesPatterns", yamlPatterns);
        SpringXmlGenerator.appendListBean(builder, "repositoryDataModules", repositoryDataModules);

        builder.append("  <bean id=\"javax.jcr.Repository\"\n");
        builder.append("        class=\"org.bloomreach.forge.brut.resources.ConfigServiceRepository\"\n");
        builder.append("        init-method=\"init\"\n");
        builder.append("        destroy-method=\"close\">\n");
        builder.append("    <constructor-arg ref=\"cndResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg ref=\"contributedCndResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg ref=\"yamlResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg ref=\"contributedYamlResourcesPatterns\"/>\n");
        builder.append("    <constructor-arg value=\"").append(SpringXmlGenerator.escapeXml(projectNamespace)).append("\"/>\n");
        builder.append("    <property name=\"additionalRepositoryModules\" ref=\"repositoryDataModules\"/>\n");
        builder.append("  </bean>\n");
        builder.append(SpringXmlGenerator.XML_FOOTER);

        return builder.toString();
    }
}
