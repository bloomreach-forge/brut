package org.bloomreach.forge.brut.components.annotation;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery;

import java.util.ArrayList;
import java.util.List;

class ComponentConfigResolver {

    private static final List<String> CORE_BEAN_PATTERNS = List.of(
        "classpath*:org/onehippo/forge/**/*.class",
        "classpath*:com/onehippo/**/*.class",
        "classpath*:org/onehippo/cms7/hst/beans/**/*.class"
    );

    static ComponentTestConfig resolve(BrxmComponentTest annotation, Class<?> testClass) {
        List<String> packages = annotation.beanPackages().length > 0
            ? List.of(annotation.beanPackages())
            : ProjectDiscovery.resolveBeanPackages(
                testClass, ProjectDiscovery.BeanPackageOrder.BEANS_FIRST, true);

        List<String> patterns = new ArrayList<>(CORE_BEAN_PATTERNS);
        patterns.addAll(ProjectDiscovery.toClasspathPatterns(packages, true, false));

        String annotatedClassesResourcePath = String.join(", ", patterns);
        String testResourcePath = annotation.testResourcePath().isEmpty()
            ? null
            : annotation.testResourcePath();

        String[] nodeTypes = annotation.nodeTypes();
        String content = annotation.content();
        String contentRoot = annotation.contentRoot();

        return new ComponentTestConfig(annotatedClassesResourcePath, testResourcePath,
                nodeTypes, content, contentRoot, testClass);
    }
}
