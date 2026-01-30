package org.bloomreach.forge.brut.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrutTestConfigurationExceptionTest {

    @Test
    void missingAnnotation_createsDescriptiveMessage() {
        BrutTestConfigurationException ex = BrutTestConfigurationException.missingAnnotation(
            BrutTestConfigurationExceptionTest.class,
            "BrxmPageModelTest",
            "org.bloomreach.forge.brut.resources.annotation"
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Missing @BrxmPageModelTest"));
        assertTrue(ex.getMessage().contains("BrutTestConfigurationExceptionTest"));
    }

    @Test
    void missingField_createsDescriptiveMessage() {
        String[] scannedFields = {"logger (Logger)", "testName (String)"};

        BrutTestConfigurationException ex = BrutTestConfigurationException.missingField(
            BrutTestConfigurationExceptionTest.class,
            "DynamicPageModelTest",
            scannedFields
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Missing required field of type DynamicPageModelTest"));
        assertTrue(ex.getMessage().contains("logger (Logger)"));
    }

    @Test
    void missingField_handlesEmptyScannedFields() {
        BrutTestConfigurationException ex = BrutTestConfigurationException.missingField(
            BrutTestConfigurationExceptionTest.class,
            "DynamicPageModelTest",
            new String[]{}
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("No fields found"));
    }

    @Test
    void invalidState_createsDescriptiveMessage() {
        BrutTestConfigurationException ex = BrutTestConfigurationException.invalidState(
            "Test instance unavailable",
            "Non-null instance",
            "Null"
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Test instance unavailable"));
        assertTrue(ex.getMessage().contains("Expected: Non-null instance"));
        assertTrue(ex.getMessage().contains("Actual: Null"));
    }

    @Test
    void setupFailed_includesCauseAndContext() {
        Exception cause = new RuntimeException("Original error");

        BrutTestConfigurationException ex = BrutTestConfigurationException.setupFailed(
            "Component initialization",
            "  Config: test-config.xml",
            cause
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Component initialization"));
        assertTrue(ex.getMessage().contains("test-config.xml"));
        assertTrue(ex.getMessage().contains("Original error"));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void bootstrapFailed_includesAllConfigDetails() {
        List<String> beanPatterns = Arrays.asList("classpath*:beans/*.class");
        List<String> springConfigs = Arrays.asList("/spring-config.xml");
        Exception cause = new RuntimeException("Bootstrap error");

        BrutTestConfigurationException ex = BrutTestConfigurationException.bootstrapFailed(
            "HST initialization",
            beanPatterns,
            springConfigs,
            "/hst:myproject",
            cause
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("HST initialization"));
        assertTrue(ex.getMessage().contains("classpath*:beans/*.class"));
        assertTrue(ex.getMessage().contains("/spring-config.xml"));
        assertTrue(ex.getMessage().contains("/hst:myproject"));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void bootstrapFailed_handlesNullLists() {
        BrutTestConfigurationException ex = BrutTestConfigurationException.bootstrapFailed(
            "Test phase",
            null,
            null,
            null,
            null
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("NONE"));
    }

    @Test
    void resourceNotFound_createsDescriptiveMessage() {
        BrutTestConfigurationException ex = BrutTestConfigurationException.resourceNotFound(
            "CND file",
            "classpath:namespaces/project.cnd",
            "Ensure the CND file exists in src/main/resources"
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("CND file not found"));
        assertTrue(ex.getMessage().contains("classpath:namespaces/project.cnd"));
        assertTrue(ex.getMessage().contains("Ensure the CND file exists"));
    }

    @Test
    void constructor_withMessage() {
        BrutTestConfigurationException ex = new BrutTestConfigurationException("Test message");
        assertEquals("Test message", ex.getMessage());
    }

    @Test
    void constructor_withMessageAndCause() {
        Exception cause = new RuntimeException("Cause");
        BrutTestConfigurationException ex = new BrutTestConfigurationException("Test message", cause);

        assertEquals("Test message", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
