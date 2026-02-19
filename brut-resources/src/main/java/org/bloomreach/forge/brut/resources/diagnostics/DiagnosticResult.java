package org.bloomreach.forge.brut.resources.diagnostics;

import java.util.List;

/**
 * Structured diagnostic result containing severity, message, and recommendations.
 *
 * @param severity       the severity level
 * @param message        the main diagnostic message
 * @param recommendations list of actionable recommendations
 */
public record DiagnosticResult(
    DiagnosticSeverity severity,
    String message,
    List<String> recommendations
) {
    /**
     * Creates a success diagnostic result.
     */
    public static DiagnosticResult success(String message) {
        return new DiagnosticResult(DiagnosticSeverity.SUCCESS, message, List.of());
    }

    /**
     * Creates an error diagnostic result with recommendations.
     */
    public static DiagnosticResult error(String message, List<String> recommendations) {
        return new DiagnosticResult(DiagnosticSeverity.ERROR, message, recommendations);
    }

    /**
     * Creates a warning diagnostic result with recommendations.
     */
    public static DiagnosticResult warning(String message, List<String> recommendations) {
        return new DiagnosticResult(DiagnosticSeverity.WARNING, message, recommendations);
    }

    /**
     * Formats the diagnostic result as a readable string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(switch (severity) {
            case SUCCESS -> "[OK] ";
            case INFO -> "[INFO] ";
            case WARNING -> "[WARN] ";
            case ERROR -> "[ERROR] ";
        });
        sb.append(message);

        if (!recommendations.isEmpty()) {
            sb.append("\n\nRECOMMENDATIONS:\n");
            recommendations.forEach(rec -> sb.append("   • ").append(rec).append("\n"));
        }

        return sb.toString();
    }
}
