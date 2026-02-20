package org.bloomreach.forge.brut.resources.diagnostics;

import org.onehippo.cm.engine.ConfigurationRuntimeException;
import org.onehippo.cm.model.parser.ParserException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing HST configuration error messages.
 * Extracts structured information from ConfigurationRuntimeException messages to support diagnostics.
 *
 * @since 5.2.1
 */
public final class ConfigErrorParser {

    private static final Pattern PATH_PATTERN = Pattern.compile("in path ['\"]([^'\"]+)['\"]");
    private static final Pattern YAML_FILE_PATTERN = Pattern.compile("\\[config: ([^\\]]+)\\]|defined in \\[([^\\]]+)\\]");
    private static final Pattern NODE_TYPE_PATTERN = Pattern.compile("primary type ['\"]([^'\"]+)['\"]|type ['\"]([^'\"]+)['\"]");
    private static final Pattern PROPERTY_ISSUE_PATTERN = Pattern.compile("no matching property definition found for (.+)$");

    private ConfigErrorParser() {
        // Utility class
    }

    /**
     * Extracts the failed JCR path from an error message.
     * Pattern: "in path '/hst:...' "
     *
     * @param message the error message
     * @return the path if found
     */
    public static Optional<String> extractFailedPath(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = PATH_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * Extracts the YAML configuration file from an error message.
     * Pattern: "[config: hst/configurations/...yaml]" or "defined in [config: ...]"
     *
     * @param message the error message
     * @return the YAML file path if found
     */
    public static Optional<String> extractYamlFile(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = YAML_FILE_PATTERN.matcher(message);
        if (matcher.find()) {
            String yamlFile = matcher.group(1);
            if (yamlFile == null) {
                yamlFile = matcher.group(2);
            }
            if (yamlFile != null && yamlFile.startsWith("config: ")) {
                yamlFile = yamlFile.substring("config: ".length());
            }
            return Optional.ofNullable(yamlFile);
        }
        return Optional.empty();
    }

    /**
     * Extracts the node type from an error message.
     * Pattern: "primary type 'hst:component'" or "type 'hst:component'"
     *
     * @param message the error message
     * @return the node type if found
     */
    public static Optional<String> extractNodeType(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = NODE_TYPE_PATTERN.matcher(message);
        if (matcher.find()) {
            String nodeType = matcher.group(1);
            if (nodeType == null) {
                nodeType = matcher.group(2);
            }
            return Optional.ofNullable(nodeType);
        }
        return Optional.empty();
    }

    /**
     * Extracts property definition issue from error message.
     * Pattern: "no matching property definition found for {uri}property"
     *
     * @param message the error message
     * @return the property identifier if found
     */
    public static Optional<String> extractPropertyIssue(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = PROPERTY_ISSUE_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    /**
     * Finds a ConfigurationRuntimeException in the exception chain.
     * Walks the cause chain looking for ConfigurationRuntimeException.
     *
     * @param error the exception to search
     * @return the ConfigurationRuntimeException if found
     */
    public static Optional<ConfigurationRuntimeException> findConfigurationException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConfigurationRuntimeException configurationException) {
                return Optional.of(configurationException);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    /**
     * Finds a ParserException in the exception chain.
     * Walks the cause chain looking for ParserException (YAML authoring errors).
     *
     * @param error the exception to search
     * @return the ParserException if found
     */
    public static Optional<ParserException> findParserException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ParserException parserException) {
                return Optional.of(parserException);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    /**
     * Finds an exception in the cause chain matching the given simple class name.
     * Used for impl-package exceptions (e.g. CircularDependencyException, DuplicateNameException)
     * that have no stability contract on their fully-qualified name.
     *
     * @param error      the exception to search
     * @param simpleName the simple class name to match
     * @return the matching throwable if found
     */
    public static Optional<Throwable> findBySimpleName(Throwable error, String simpleName) {
        Throwable current = error;
        while (current != null) {
            if (simpleName.equals(current.getClass().getSimpleName())) {
                return Optional.of(current);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }
}
