package org.bloomreach.forge.brut.components.annotation;

class ComponentTestConfig {

    private final String annotatedClassesResourcePath;
    private final String testResourcePath;
    private final String[] nodeTypes;
    private final String content;
    private final String contentRoot;
    private final Class<?> testClass;

    ComponentTestConfig(String annotatedClassesResourcePath, String testResourcePath,
                        String[] nodeTypes, String content, String contentRoot, Class<?> testClass) {
        this.annotatedClassesResourcePath = annotatedClassesResourcePath;
        this.testResourcePath = testResourcePath;
        this.nodeTypes = nodeTypes;
        this.content = content;
        this.contentRoot = contentRoot;
        this.testClass = testClass;
    }

    String getAnnotatedClassesResourcePath() {
        return annotatedClassesResourcePath;
    }

    String getTestResourcePath() {
        return testResourcePath;
    }

    String[] getNodeTypes() {
        return nodeTypes;
    }

    String getContent() {
        return content;
    }

    String getContentRoot() {
        return contentRoot;
    }

    boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    boolean hasContentRoot() {
        return contentRoot != null && !contentRoot.isEmpty();
    }

    Class<?> getTestClass() {
        return testClass;
    }
}
