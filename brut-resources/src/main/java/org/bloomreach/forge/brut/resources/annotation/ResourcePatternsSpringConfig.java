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

/**
 * Generates Spring XML configuration for YAML and CND resource patterns at runtime.
 * Eliminates the need for boilerplate Spring XML files that just define pattern lists.
 */
class ResourcePatternsSpringConfig {

    static String create(List<String> yamlPatterns, List<String> cndPatterns) {
        String xml = buildXml(yamlPatterns, cndPatterns);
        return SpringXmlGenerator.createTempConfig("brut-resource-patterns-", xml);
    }

    private static String buildXml(List<String> yamlPatterns, List<String> cndPatterns) {
        StringBuilder builder = new StringBuilder();
        builder.append(SpringXmlGenerator.XML_HEADER);
        SpringXmlGenerator.appendListBean(builder, "contributedCndResourcesPatterns", cndPatterns);
        SpringXmlGenerator.appendListBean(builder, "contributedYamlResourcesPatterns", yamlPatterns);
        builder.append(SpringXmlGenerator.XML_FOOTER);
        return builder.toString();
    }
}
