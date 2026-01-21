package org.bloomreach.forge.brut.components.annotation;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for HST component tests with zero-configuration defaults.
 *
 * <p>Tests using this annotation automatically get:</p>
 * <ul>
 *   <li>Lifecycle management (setup/teardown)</li>
 *   <li>Auto-detected bean scanning from project settings or test package</li>
 *   <li>Default skeleton repository data when no test resource is specified</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(BrxmComponentTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface BrxmComponentTest {

    /**
     * Bean class package patterns for HST content bean scanning.
     *
     * <p>If empty (default), auto-detects packages based on project settings
     * or test package conventions.</p>
     *
     * @return bean package names
     */
    String[] beanPackages() default {};

    /**
     * YAML test resource path to import before each test.
     *
     * <p>If empty (default), uses the framework skeleton repository data.</p>
     *
     * @return test resource path
     */
    String testResourcePath() default "";

    /**
     * Node types to register before test execution.
     *
     * <p><strong>Auto-detection:</strong> When empty (default), node types are automatically
     * detected by scanning {@link #beanPackages()} for classes annotated with
     * {@code @Node(jcrType="...")}. This eliminates the need to manually specify types
     * that are already defined in your bean classes.</p>
     *
     * <p><strong>Explicit override:</strong> Specify types manually when you need:</p>
     * <ul>
     *   <li>Type inheritance: {@code "ns:ChildType extends ns:ParentType"}</li>
     *   <li>Types not defined in scanned bean packages</li>
     * </ul>
     *
     * <p>Example with inheritance: {@code {"ns:NewsPage extends ns:AnotherType", "ns:AnotherType"}}</p>
     *
     * @return node type definitions to register (empty for auto-detection)
     */
    String[] nodeTypes() default {};

    /**
     * YAML content resource path to import before each test.
     *
     * <p>Example: "/news.yaml"</p>
     *
     * @return content resource path
     */
    String content() default "";

    /**
     * Target path where content is imported. Also sets site content base path.
     *
     * <p>Example: "/content/documents/mychannel"</p>
     *
     * @return content root path
     */
    String contentRoot() default "";
}
