package org.bloomreach.forge.brut.resources.diagnostics;

import org.onehippo.cm.engine.ConfigurationRuntimeException;
import org.onehippo.cm.model.parser.ParserException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic utilities for analyzing HST configuration errors during bootstrap.
 * Provides actionable recommendations when ConfigService encounters configuration issues.
 *
 * @since 5.2.1
 */
public final class ConfigurationDiagnostics {

    private static final Field PARSER_SOURCE_FIELD = resolveParserSourceField();

    private ConfigurationDiagnostics() {
        // Utility class
    }

    private record ChainScan(
        ParserException parserException,
        Throwable circularDependency,
        Throwable duplicateName,
        ConfigurationRuntimeException configurationException
    ) {}

    private static ChainScan scanChain(Throwable error) {
        ParserException foundParserException = null;
        Throwable foundCircularDependency = null;
        Throwable foundDuplicateName = null;
        ConfigurationRuntimeException foundConfigException = null;

        Throwable current = error;
        while (current != null && (foundParserException == null || foundCircularDependency == null
                || foundDuplicateName == null || foundConfigException == null)) {
            if (foundParserException == null && current instanceof ParserException parserException) {
                foundParserException = parserException;
            } else if (foundCircularDependency == null && "CircularDependencyException".equals(current.getClass().getSimpleName())) {
                foundCircularDependency = current;
            } else if (foundDuplicateName == null && "DuplicateNameException".equals(current.getClass().getSimpleName())) {
                foundDuplicateName = current;
            } else if (foundConfigException == null && current instanceof ConfigurationRuntimeException configException) {
                foundConfigException = configException;
            }
            current = current.getCause();
        }

        return new ChainScan(foundParserException, foundCircularDependency, foundDuplicateName, foundConfigException);
    }

    /**
     * Diagnoses a configuration error by analyzing the exception chain and extracting
     * actionable information.
     *
     * @param error the exception to diagnose
     * @return diagnostic result with severity and recommendations
     */
    public static DiagnosticResult diagnoseConfigurationError(Throwable error) {
        if (error == null) {
            return DiagnosticResult.error(
                "Unknown configuration error",
                List.of("Check logs for ConfigService bootstrap failures",
                        "Verify YAML configuration files syntax")
            );
        }

        ChainScan scan = scanChain(error);

        if (scan.parserException() != null) {
            return diagnoseParserException(scan.parserException());
        }
        if (scan.circularDependency() != null) {
            return diagnoseCircularDependency(scan.circularDependency());
        }
        if (scan.duplicateName() != null) {
            return diagnoseDuplicateName(scan.duplicateName());
        }

        ConfigurationRuntimeException configEx = scan.configurationException();
        if (configEx == null) {
            return diagnoseGenericConfigError(error);
        }

        String message = configEx.getMessage();
        if (message == null) {
            return diagnoseGenericConfigError(error);
        }

        String path = ConfigErrorParser.extractFailedPath(message).orElse(null);
        String yamlFile = ConfigErrorParser.extractYamlFile(message).orElse(null);
        String nodeType = ConfigErrorParser.extractNodeType(message).orElse(null);
        String propertyIssue = ConfigErrorParser.extractPropertyIssue(message).orElse(null);

        if (propertyIssue != null) {
            return diagnoseMissingPropertyDefinition(path, yamlFile, nodeType, propertyIssue);
        }
        if (nodeType != null) {
            return diagnoseInvalidNodeType(path, yamlFile, nodeType);
        }
        if (path != null || yamlFile != null) {
            return diagnoseConfigPathError(path, yamlFile, message);
        }

        return DiagnosticResult.warning(
            "Exception message did not match any known pattern. Raw: " + message,
            List.of());
    }

    private static DiagnosticResult diagnoseParserException(ParserException parserException) {
        String source = extractParserSource(parserException);
        StringBuilder messageBuilder = new StringBuilder("YAML parse error");
        if (source != null) {
            messageBuilder.append(" in ").append(source);
        }
        messageBuilder.append(": ").append(parserException.getMessage() != null ? parserException.getMessage() : "(no message)");

        List<String> recommendations = new ArrayList<>();
        if (source != null) {
            recommendations.add("Check YAML file: " + source);
        }

        var yamlNode = parserException.getNode();
        if (yamlNode != null) {
            var startMark = yamlNode.getStartMark();
            if (startMark != null) {
                recommendations.add("Error at line " + (startMark.getLine() + 1) +
                    ", column " + (startMark.getColumn() + 1));
            }
        }

        recommendations.add("Verify YAML syntax: indentation, quoting, and property types");
        recommendations.add("Check referenced paths and node identifiers exist");

        return DiagnosticResult.error(messageBuilder.toString(), recommendations);
    }

    private static Field resolveParserSourceField() {
        try {
            Field sourceField = ParserException.class.getDeclaredField("source");
            sourceField.setAccessible(true);
            return sourceField;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String extractParserSource(ParserException parserException) {
        if (PARSER_SOURCE_FIELD == null) {
            return null;
        }
        try {
            return (String) PARSER_SOURCE_FIELD.get(parserException);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static DiagnosticResult diagnoseCircularDependency(Throwable circularException) {
        List<String> recommendations = new ArrayList<>();
        if (circularException.getMessage() != null) {
            recommendations.add(circularException.getMessage());
        }
        recommendations.add("Check 'after:' declarations in hcm-module.yaml for circular references");
        recommendations.add("Verify module dependency graph has no cycles");

        return DiagnosticResult.error("Circular dependency in module load order", recommendations);
    }

    private static DiagnosticResult diagnoseDuplicateName(Throwable duplicateException) {
        List<String> recommendations = new ArrayList<>();
        if (duplicateException.getMessage() != null) {
            recommendations.add(duplicateException.getMessage());
        }
        recommendations.add("Check for duplicate node definitions across YAML files in the same module");
        recommendations.add("Ensure each node path is defined only once per module");

        return DiagnosticResult.error("Duplicate node name in module configuration", recommendations);
    }

    /**
     * Diagnoses missing property definition errors.
     * Occurs when a node type doesn't define a required property.
     */
    private static DiagnosticResult diagnoseMissingPropertyDefinition(
            String path,
            String yamlFile,
            String nodeType,
            String property) {

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Property definition mismatch");

        if (nodeType != null) {
            messageBuilder.append(" for node type '").append(nodeType).append("'");
        }

        if (path != null) {
            messageBuilder.append(" in path '").append(path).append("'");
        }

        List<String> recommendations = new ArrayList<>();

        if (yamlFile != null) {
            recommendations.add("Check YAML file: " + yamlFile);
        }

        String propertyName = extractPropertyName(property);
        if (propertyName != null) {
            recommendations.add("Property '" + propertyName + "' not defined for node type" +
                (nodeType != null ? " '" + nodeType + "'" : ""));

            if (property.startsWith("{")) {
                String namespace = extractNamespace(property);
                if (namespace != null) {
                    recommendations.add("Verify namespace '" + namespace + "' is registered");
                }
            }
        }

        if (nodeType != null) {
            recommendations.add("Review CND files to ensure '" + nodeType + "' node type definition includes this property");
            recommendations.add("Consider using a different node type that supports '" + propertyName + "'");
        } else {
            recommendations.add("Verify the node type definition includes the required property");
        }

        recommendations.add("Check if property name or namespace prefix is correct in YAML configuration");

        return DiagnosticResult.error(messageBuilder.toString(), recommendations);
    }

    /**
     * Diagnoses invalid node type errors.
     */
    private static DiagnosticResult diagnoseInvalidNodeType(
            String path,
            String yamlFile,
            String nodeType) {

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Node type issue with '").append(nodeType).append("'");

        if (path != null) {
            messageBuilder.append(" in path '").append(path).append("'");
        }

        List<String> recommendations = new ArrayList<>();

        if (yamlFile != null) {
            recommendations.add("Check YAML file: " + yamlFile);
        }

        recommendations.add("Verify node type '" + nodeType + "' is defined in CND files");

        int colonIndex = nodeType.indexOf(':');
        if (colonIndex > 0) {
            String prefix = nodeType.substring(0, colonIndex);
            recommendations.add("Ensure namespace prefix '" + prefix + "' is registered");
            recommendations.add("Check that namespace URI is correctly defined");
        }

        recommendations.add("Verify the primary type is appropriate for this node");
        recommendations.add("Check for typos in node type name");

        if (path != null) {
            recommendations.add("Review parent node constraints - verify '" + nodeType + "' is allowed as child");
        }

        return DiagnosticResult.error(messageBuilder.toString(), recommendations);
    }

    /**
     * Diagnoses generic configuration path errors.
     */
    private static DiagnosticResult diagnoseConfigPathError(
            String path,
            String yamlFile,
            String message) {

        StringBuilder messageBuilder = new StringBuilder("Configuration error");

        if (path != null) {
            messageBuilder.append(" in path '").append(path).append("'");
        }

        List<String> recommendations = new ArrayList<>();

        if (yamlFile != null) {
            recommendations.add("Check YAML file: " + yamlFile);
        }

        if (path != null) {
            recommendations.add("Verify path structure: " + path);
            recommendations.add("Check parent nodes exist before child node creation");
        }

        recommendations.add("Review YAML syntax and node structure");
        recommendations.add("Verify all required properties are defined");
        recommendations.add("Check for namespace or node type issues");

        return DiagnosticResult.error(messageBuilder.toString(), recommendations);
    }

    /**
     * Diagnoses generic configuration errors when specific details can't be extracted.
     */
    private static DiagnosticResult diagnoseGenericConfigError(Throwable error) {
        String errorClass = error.getClass().getSimpleName();
        String errorMessage = error.getMessage() != null ? error.getMessage() : "No error message";

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Review ConfigService bootstrap logs for detailed stack trace");
        recommendations.add("Verify YAML configuration files syntax and structure");
        recommendations.add("Check that all referenced node types and namespaces are defined");
        recommendations.add("Ensure CND files are properly loaded");
        recommendations.add("Verify parent-child node type constraints");

        if (errorMessage.contains("namespace")) {
            recommendations.add(0, "NOTE: Namespace-related error detected - verify namespace prefixes and URIs");
        } else if (errorMessage.contains("node type") || errorMessage.contains("primary type")) {
            recommendations.add(0, "NOTE: Node type error detected - verify node type definitions in CND files");
        } else if (errorMessage.contains("property")) {
            recommendations.add(0, "NOTE: Property-related error detected - check property definitions");
        }

        return DiagnosticResult.error(
            "HST configuration error during bootstrap: " + errorClass,
            recommendations
        );
    }

    /**
     * Extracts property name from expanded or prefixed format.
     * Handles: {uri}localName or prefix:localName
     */
    private static String extractPropertyName(String property) {
        if (property == null) {
            return null;
        }

        // Handle {uri}localName format
        if (property.startsWith("{")) {
            int braceEnd = property.indexOf('}');
            if (braceEnd > 0 && braceEnd < property.length() - 1) {
                return property.substring(braceEnd + 1);
            }
        }

        // Handle prefix:localName format
        int colonIndex = property.indexOf(':');
        if (colonIndex > 0 && colonIndex < property.length() - 1) {
            return property.substring(colonIndex + 1);
        }

        return property;
    }

    /**
     * Extracts namespace URI from expanded format: {uri}localName
     */
    private static String extractNamespace(String property) {
        if (property == null || !property.startsWith("{")) {
            return null;
        }

        int braceEnd = property.indexOf('}');
        if (braceEnd > 1) {
            return property.substring(1, braceEnd);
        }

        return null;
    }
}
