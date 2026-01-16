package org.bloomreach.forge.brut.components.annotation;

class ComponentTestConfig {

    private final String annotatedClassesResourcePath;
    private final String testResourcePath;

    ComponentTestConfig(String annotatedClassesResourcePath, String testResourcePath) {
        this.annotatedClassesResourcePath = annotatedClassesResourcePath;
        this.testResourcePath = testResourcePath;
    }

    String getAnnotatedClassesResourcePath() {
        return annotatedClassesResourcePath;
    }

    String getTestResourcePath() {
        return testResourcePath;
    }
}
