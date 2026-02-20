package org.bloomreach.forge.brut.components.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for ComponentTestConfig.computeFingerprint().
 */
class ComponentTestConfigFingerprintTest {

    @Test
    void identicalConfigs_produceSameFingerprint() {
        ComponentTestConfig c1 = new ComponentTestConfig(
            "classpath*:org/**/*.class", "/skeleton.yaml", new String[0], null, null, Dummy.class);
        ComponentTestConfig c2 = new ComponentTestConfig(
            "classpath*:org/**/*.class", "/skeleton.yaml", new String[0], null, null, Dummy.class);
        assertEquals(c1.computeFingerprint(), c2.computeFingerprint());
    }

    @Test
    void differentAnnotatedClasses_produceDifferentFingerprint() {
        ComponentTestConfig c1 = new ComponentTestConfig(
            "classpath*:com/**/*.class", "/skeleton.yaml", new String[0], null, null, Dummy.class);
        ComponentTestConfig c2 = new ComponentTestConfig(
            "classpath*:org/**/*.class", "/skeleton.yaml", new String[0], null, null, Dummy.class);
        assertNotEquals(c1.computeFingerprint(), c2.computeFingerprint());
    }

    @Test
    void differentContent_produceDifferentFingerprint() {
        ComponentTestConfig c1 = new ComponentTestConfig(
            "classpath*:org/**/*.class", "/skeleton.yaml", new String[0], "/content-a.yaml", "/content", Dummy.class);
        ComponentTestConfig c2 = new ComponentTestConfig(
            "classpath*:org/**/*.class", "/skeleton.yaml", new String[0], "/content-b.yaml", "/content", Dummy.class);
        assertNotEquals(c1.computeFingerprint(), c2.computeFingerprint());
    }

    @Test
    void differentTestResource_produceDifferentFingerprint() {
        ComponentTestConfig c1 = new ComponentTestConfig(
            "classpath*:org/**/*.class", "/skeleton.yaml", new String[0], null, null, Dummy.class);
        ComponentTestConfig c2 = new ComponentTestConfig(
            "classpath*:org/**/*.class", "/other.yaml", new String[0], null, null, Dummy.class);
        assertNotEquals(c1.computeFingerprint(), c2.computeFingerprint());
    }

    @Test
    void fingerprintIsNeverNull() {
        ComponentTestConfig c = new ComponentTestConfig(null, null, new String[0], null, null, Dummy.class);
        assertNotNull(c.computeFingerprint());
    }

    private static class Dummy {}
}
