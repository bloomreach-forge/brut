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

    /**
     * Returns a stable string that uniquely identifies the structural configuration.
     * Two configs with the same fingerprint are safe to share a single
     * {@link org.bloomreach.forge.brut.common.repository.BrxmTestingRepository}.
     */
    String computeFingerprint() {
        return String.join("|",
            annotatedClassesResourcePath != null ? annotatedClassesResourcePath : "",
            testResourcePath != null ? testResourcePath : "",
            content != null ? content : "",
            contentRoot != null ? contentRoot : ""
        );
    }
}
