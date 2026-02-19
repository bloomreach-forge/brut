package org.bloomreach.forge.brut.resources.diagnostics;

import org.junit.jupiter.api.Test;
import org.onehippo.cm.engine.ConfigurationRuntimeException;
import org.onehippo.cm.model.parser.ParserException;

import javax.jcr.RepositoryException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationDiagnosticsTest {

    @Test
    void diagnoseConfigurationError_withMissingPropertyDefinition() {
        String errorMessage = "Failed to set primary type 'hst:component' in path " +
                "'/hst:petbase/hst:configurations/petbase-landing/hst:pages/homepage/main' " +
                "defined in [config: hst/configurations/petbase-landing/pages/homepage.yaml]: " +
                "no matching property definition found for {http://www.onehippo.org/jcr/hippo/nt/2.0.4}identifier";

        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException(errorMessage);
        RepositoryException repoEx = new RepositoryException("Wrapper", configEx);

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(repoEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("hst:component"));
        assertTrue(result.message().contains("/hst:petbase/hst:configurations/petbase-landing/hst:pages/homepage/main"));
        assertFalse(result.recommendations().isEmpty());
        assertTrue(result.recommendations().stream()
                .anyMatch(r -> r.contains("hst/configurations/petbase-landing/pages/homepage.yaml")));
        assertTrue(result.recommendations().stream()
                .anyMatch(r -> r.contains("identifier")));
    }

    @Test
    void diagnoseConfigurationError_withInvalidNodeType() {
        String errorMessage = "Failed to set primary type 'invalid:type' in path " +
                "'/hst:test/hst:configurations/test/hst:pages/test' " +
                "defined in [config: hst/configurations/test/pages/test.yaml]";

        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException(errorMessage);

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(configEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("invalid:type"));
        assertFalse(result.recommendations().isEmpty());
    }

    @Test
    void diagnoseConfigurationError_genericError() {
        Exception genericEx = new RuntimeException("Some other configuration error");

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(genericEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("configuration error"));
        assertFalse(result.recommendations().isEmpty());
    }

    @Test
    void diagnoseConfigurationError_nullException() {
        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(null);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("Unknown"));
        assertFalse(result.recommendations().isEmpty());
    }

    @Test
    void diagnoseConfigurationError_noConfigurationException() {
        RepositoryException repoEx = new RepositoryException("Generic repository error");

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(repoEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("configuration error") || result.message().contains("repository error"));
    }

    @Test
    void diagnoseConfigurationError_withPathOnly() {
        String errorMessage = "Error in path '/hst:test/hst:pages/home' defined in [config: hst/test.yaml]";
        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException(errorMessage);

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(configEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("/hst:test/hst:pages/home"));
    }

    @Test
    void diagnoseConfigurationError_handlesMultipleIssues() {
        String errorMessage = "Failed to set primary type 'hst:component' in path '/hst:test' " +
                "defined in [config: hst/test.yaml]: " +
                "no matching property definition found for {http://example.org}property";

        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException(errorMessage);

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(configEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        // Should mention both path and property issues
        String fullDiagnostic = result.toString();
        assertTrue(fullDiagnostic.contains("/hst:test"));
        assertTrue(fullDiagnostic.contains("property"));
    }

    @Test
    void diagnoseConfigurationError_propertyIssueWithoutNodeType() {
        String errorMessage = "Property definition error: " +
                "no matching property definition found for {http://www.onehippo.org/jcr/hippo/nt/2.0.4}identifier " +
                "in path '/hst:test/path'";

        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException(errorMessage);

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(configEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.recommendations().stream()
                .anyMatch(r -> r.contains("identifier") || r.contains("property")));
    }

    @Test
    void toStringFormat_includesEmojiAndRecommendations() {
        String errorMessage = "Failed to set primary type 'hst:component' in path '/hst:test' " +
                "defined in [config: hst/test.yaml]";
        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException(errorMessage);

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(configEx);
        String formatted = result.toString();

        assertTrue(formatted.contains("[ERROR]"));
        assertTrue(formatted.contains("RECOMMENDATIONS:"));
        assertTrue(formatted.contains("•"));
    }

    @Test
    void diagnoseConfigurationError_withParserException_noNode() {
        ParserException parserEx = new ParserException("unexpected token: expected '}'");

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(parserEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("YAML"));
        assertFalse(result.recommendations().isEmpty());
    }

    @Test
    void diagnoseConfigurationError_withParserException_withSource() {
        ParserException parserEx = new ParserException("unexpected token");
        parserEx.setSource("hst/configurations/test.yaml");

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(parserEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("hst/configurations/test.yaml"));
        assertTrue(result.recommendations().stream()
                .anyMatch(r -> r.contains("hst/configurations/test.yaml")));
    }

    @Test
    void diagnoseConfigurationError_withCircularDependencyBySimpleName() {
        RuntimeException circularEx = new CircularDependencyException(
                "Module A depends on Module B which depends on Module A");

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(circularEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("Circular"));
        assertTrue(result.recommendations().stream()
                .anyMatch(r -> r.contains("hcm-module.yaml")));
    }

    @Test
    void diagnoseConfigurationError_withDuplicateNameBySimpleName() {
        RuntimeException duplicateEx = new DuplicateNameException("Duplicate node 'hst:pages'");

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(duplicateEx);

        assertEquals(DiagnosticSeverity.ERROR, result.severity());
        assertTrue(result.message().contains("Duplicate"));
        assertTrue(result.recommendations().stream()
                .anyMatch(r -> r.contains("duplicate") || r.contains("Duplicate")));
    }

    @Test
    void diagnoseConfigurationError_unmatchedCre_emitsWarning() {
        ConfigurationRuntimeException cre =
                new ConfigurationRuntimeException("Some completely unknown error format xyz");

        DiagnosticResult result = ConfigurationDiagnostics.diagnoseConfigurationError(cre);

        assertEquals(DiagnosticSeverity.WARNING, result.severity());
        assertTrue(result.message().contains("did not match"));
        assertTrue(result.message().contains("Some completely unknown error format xyz"));
    }

    private static class CircularDependencyException extends RuntimeException {
        CircularDependencyException(String message) {
            super(message);
        }
    }

    private static class DuplicateNameException extends RuntimeException {
        DuplicateNameException(String message) {
            super(message);
        }
    }
}
