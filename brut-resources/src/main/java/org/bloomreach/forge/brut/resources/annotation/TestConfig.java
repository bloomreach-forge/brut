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
 * Immutable configuration holder for annotation-based tests.
 * Stores resolved configuration values (either from annotation parameters or auto-detected).
 */
class TestConfig {
    private final List<String> beanPatterns;
    private final String hstRoot;
    private final List<String> springConfigs;
    private final List<String> addonModules;
    private final List<String> repositoryDataModules;

    TestConfig(List<String> beanPatterns,
               String hstRoot,
               List<String> springConfigs,
               List<String> addonModules,
               List<String> repositoryDataModules) {
        this.beanPatterns = beanPatterns;
        this.hstRoot = hstRoot;
        this.springConfigs = springConfigs;
        this.addonModules = addonModules;
        this.repositoryDataModules = repositoryDataModules;
    }

    List<String> getBeanPatterns() {
        return beanPatterns;
    }

    String getHstRoot() {
        return hstRoot;
    }

    List<String> getSpringConfigs() {
        return springConfigs;
    }

    List<String> getAddonModules() {
        return addonModules;
    }

    List<String> getRepositoryDataModules() {
        return repositoryDataModules;
    }
}
