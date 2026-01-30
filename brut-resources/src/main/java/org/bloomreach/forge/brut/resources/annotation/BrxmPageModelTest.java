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

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for PageModel API tests that eliminates boilerplate configuration.
 *
 * <p>Tests using this annotation automatically get:</p>
 * <ul>
 *   <li>Lifecycle management (init/destroy/beforeEach)</li>
 *   <li>HST request setup with default headers (Accept: application/json, GET method)</li>
 *   <li>Auto-detected bean scanning from test package</li>
 *   <li>Auto-detected HST root from project name</li>
 *   <li>Auto-detected Spring configuration (if present)</li>
 * </ul>
 *
 * <h3>Zero-Configuration Example:</h3>
 * <pre>{@code
 * @BrxmPageModelTest
 * public class MyPageModelTest {
 *     private DynamicPageModelTest brxm;  // Injected automatically
 *
 *     @Test
 *     void testPageModel() {
 *         brxm.getHstRequest().setRequestURI("/site/resourceapi/news");
 *         String response = brxm.invokeFilter();
 *         assertNotNull(response);
 *     }
 * }
 * }</pre>
 *
 * <h3>Explicit Configuration Example:</h3>
 * <pre>{@code
 * @BrxmPageModelTest(
 *     beanPackages = {"org.example.model", "org.example.beans"},
 *     hstRoot = "/hst:customproject",
 *     springConfig = "/org/example/custom-spring.xml"
 * )
 * public class MyCustomTest {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(BrxmPageModelTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface BrxmPageModelTest {

    /**
     * Bean class package patterns for HST content bean scanning.
     *
     * <p>If empty (default), auto-detects patterns based on test package:</p>
     * <ul>
     *   <li>{@code classpath*:<testPackage>/beans/*.class}</li>
     *   <li>{@code classpath*:<testPackage>/model/*.class}</li>
     * </ul>
     *
     * @return bean package patterns
     */
    String[] beanPackages() default {};

    /**
     * HST configuration root path in the repository.
     *
     * <p>If empty (default), auto-detects: {@code /hst:<projectName>}</p>
     * <p>Project name is determined from pom.xml artifactId or directory name.</p>
     *
     * @return HST configuration root path
     */
    String hstRoot() default "";

    /**
     * Spring configuration file location.
     *
     * <p>If empty (default), auto-detects: {@code /<testPackage>/custom-pagemodel.xml}</p>
     * <p>If the file doesn't exist, no Spring configuration is loaded.</p>
     *
     * @return Spring configuration file location
     */
    String springConfig() default "";

    /**
     * Automatically discover and load HCM content from project modules.
     *
     * <p>Enabled by default. When enabled, BRUT scans repository-data modules
     * and loads HST configuration and content into the test repository.</p>
     *
     * <p>Set to false for simpler tests that don't need project content.</p>
     *
     * @return true to enable content discovery (default: true)
     */
    boolean loadProjectContent() default true;

    /**
     * HST addon module paths (rarely needed).
     *
     * <p>If empty (default), no addon modules are loaded.</p>
     *
     * @return addon module paths
     */
    String[] addonModules() default {};

    /**
     * Additional repository-data module names to include when ConfigService is enabled.
     *
     * <p>Defaults to standard brXM modules discovered by BRUT. Add entries to include
     * extra repository-data submodules.</p>
     *
     * @return repository-data module names
     */
    String[] repositoryDataModules() default {};
}
