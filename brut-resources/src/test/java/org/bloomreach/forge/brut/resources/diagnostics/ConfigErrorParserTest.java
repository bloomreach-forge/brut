package org.bloomreach.forge.brut.resources.diagnostics;

import org.junit.jupiter.api.Test;
import org.onehippo.cm.engine.ConfigurationRuntimeException;
import org.onehippo.cm.model.parser.ParserException;

import javax.jcr.RepositoryException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigErrorParserTest {

    @Test
    void extractFailedPath_fromTypicalMessage() {
        String message = "Failed to set primary type 'hst:component' in path " +
                "'/hst:petbase/hst:configurations/petbase-landing/hst:pages/homepage/main' " +
                "defined in [config: hst/configurations/petbase-landing/pages/homepage.yaml]";

        Optional<String> path = ConfigErrorParser.extractFailedPath(message);

        assertTrue(path.isPresent());
        assertEquals("/hst:petbase/hst:configurations/petbase-landing/hst:pages/homepage/main", path.get());
    }

    @Test
    void extractFailedPath_whenNoPathPresent() {
        String message = "Some generic error without path";

        Optional<String> path = ConfigErrorParser.extractFailedPath(message);

        assertFalse(path.isPresent());
    }

    @Test
    void extractYamlFile_fromTypicalMessage() {
        String message = "Failed to set primary type 'hst:component' in path " +
                "'/hst:petbase/hst:configurations/petbase-landing/hst:pages/homepage/main' " +
                "defined in [config: hst/configurations/petbase-landing/pages/homepage.yaml]";

        Optional<String> yamlFile = ConfigErrorParser.extractYamlFile(message);

        assertTrue(yamlFile.isPresent());
        assertEquals("hst/configurations/petbase-landing/pages/homepage.yaml", yamlFile.get());
    }

    @Test
    void extractYamlFile_alternateFormat() {
        String message = "Error in configuration [config: hst/workspace/test.yaml]: invalid node";

        Optional<String> yamlFile = ConfigErrorParser.extractYamlFile(message);

        assertTrue(yamlFile.isPresent());
        assertEquals("hst/workspace/test.yaml", yamlFile.get());
    }

    @Test
    void extractYamlFile_whenNoFilePresent() {
        String message = "Some error without yaml reference";

        Optional<String> yamlFile = ConfigErrorParser.extractYamlFile(message);

        assertFalse(yamlFile.isPresent());
    }

    @Test
    void extractNodeType_fromTypicalMessage() {
        String message = "Failed to set primary type 'hst:component' in path '/hst:test'";

        Optional<String> nodeType = ConfigErrorParser.extractNodeType(message);

        assertTrue(nodeType.isPresent());
        assertEquals("hst:component", nodeType.get());
    }

    @Test
    void extractNodeType_whenNoTypePresent() {
        String message = "Some error without node type";

        Optional<String> nodeType = ConfigErrorParser.extractNodeType(message);

        assertFalse(nodeType.isPresent());
    }

    @Test
    void extractPropertyIssue_fromTypicalMessage() {
        String message = "Failed to set primary type 'hst:component' in path '/hst:test': " +
                "no matching property definition found for {http://www.onehippo.org/jcr/hippo/nt/2.0.4}identifier";

        Optional<String> property = ConfigErrorParser.extractPropertyIssue(message);

        assertTrue(property.isPresent());
        assertEquals("{http://www.onehippo.org/jcr/hippo/nt/2.0.4}identifier", property.get());
    }

    @Test
    void extractPropertyIssue_whenNoPropertyIssue() {
        String message = "Some other error";

        Optional<String> property = ConfigErrorParser.extractPropertyIssue(message);

        assertFalse(property.isPresent());
    }

    @Test
    void findConfigurationException_directException() {
        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException("Config error");

        Optional<ConfigurationRuntimeException> found = ConfigErrorParser.findConfigurationException(configEx);

        assertTrue(found.isPresent());
        assertEquals(configEx, found.get());
    }

    @Test
    void findConfigurationException_wrappedInRepositoryException() {
        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException("Config error");
        RepositoryException repoEx = new RepositoryException("Wrapper", configEx);

        Optional<ConfigurationRuntimeException> found = ConfigErrorParser.findConfigurationException(repoEx);

        assertTrue(found.isPresent());
        assertEquals(configEx, found.get());
    }

    @Test
    void findConfigurationException_deeplyNested() {
        ConfigurationRuntimeException configEx = new ConfigurationRuntimeException("Config error");
        RuntimeException level1 = new RuntimeException("Level 1", configEx);
        RepositoryException level2 = new RepositoryException("Level 2", level1);

        Optional<ConfigurationRuntimeException> found = ConfigErrorParser.findConfigurationException(level2);

        assertTrue(found.isPresent());
        assertEquals(configEx, found.get());
    }

    @Test
    void findConfigurationException_notPresent() {
        RepositoryException repoEx = new RepositoryException("No config exception");

        Optional<ConfigurationRuntimeException> found = ConfigErrorParser.findConfigurationException(repoEx);

        assertFalse(found.isPresent());
    }

    @Test
    void findConfigurationException_nullThrowable() {
        Optional<ConfigurationRuntimeException> found = ConfigErrorParser.findConfigurationException(null);

        assertFalse(found.isPresent());
    }

    @Test
    void findParserException_directException() {
        ParserException parserEx = new ParserException("YAML error");

        Optional<ParserException> found = ConfigErrorParser.findParserException(parserEx);

        assertTrue(found.isPresent());
        assertEquals(parserEx, found.get());
    }

    @Test
    void findParserException_wrappedInRuntimeException() {
        ParserException parserEx = new ParserException("YAML error");
        RuntimeException wrapper = new RuntimeException("Wrapper", parserEx);

        Optional<ParserException> found = ConfigErrorParser.findParserException(wrapper);

        assertTrue(found.isPresent());
        assertEquals(parserEx, found.get());
    }

    @Test
    void findParserException_notPresent() {
        RuntimeException ex = new RuntimeException("Not a parser exception");

        Optional<ParserException> found = ConfigErrorParser.findParserException(ex);

        assertFalse(found.isPresent());
    }

    @Test
    void findParserException_nullThrowable() {
        Optional<ParserException> found = ConfigErrorParser.findParserException(null);

        assertFalse(found.isPresent());
    }

    @Test
    void findBySimpleName_directMatch() {
        RuntimeException ex = new StubCircularDependencyException("Circular ref");

        Optional<Throwable> found = ConfigErrorParser.findBySimpleName(ex, "StubCircularDependencyException");

        assertTrue(found.isPresent());
        assertEquals(ex, found.get());
    }

    @Test
    void findBySimpleName_wrappedMatch() {
        RuntimeException inner = new StubCircularDependencyException("Circular ref");
        RuntimeException wrapper = new RuntimeException("Wrapper", inner);

        Optional<Throwable> found = ConfigErrorParser.findBySimpleName(wrapper, "StubCircularDependencyException");

        assertTrue(found.isPresent());
        assertEquals(inner, found.get());
    }

    @Test
    void findBySimpleName_notPresent() {
        RuntimeException ex = new RuntimeException("Not a circular dependency");

        Optional<Throwable> found = ConfigErrorParser.findBySimpleName(ex, "StubCircularDependencyException");

        assertFalse(found.isPresent());
    }

    @Test
    void findBySimpleName_nullThrowable() {
        Optional<Throwable> found = ConfigErrorParser.findBySimpleName(null, "StubCircularDependencyException");

        assertFalse(found.isPresent());
    }

    private static class StubCircularDependencyException extends RuntimeException {
        StubCircularDependencyException(String message) {
            super(message);
        }
    }
}
