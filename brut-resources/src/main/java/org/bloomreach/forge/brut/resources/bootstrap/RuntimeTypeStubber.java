package org.bloomreach.forge.brut.resources.bootstrap;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.StringReader;
import java.util.Set;

/**
 * Handles runtime stubbing of missing JCR namespaces and node types.
 * <p>
 * During content import, the bootstrap process may encounter node types that don't exist
 * in the test repository (e.g., CMS-tier types like hippofacnav:facetnavigation).
 * This class creates minimal stub definitions to allow content import to proceed.
 * <p>
 * <strong>Note:</strong> Stubbed types are placeholders only. They satisfy JCR validation
 * but don't provide actual type functionality.
 *
 * @since 5.2.0
 */
public final class RuntimeTypeStubber {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeTypeStubber.class);

    private static final String STUB_MISSING_NAMESPACES_PROPERTY = "brut.configservice.stubMissingNamespaces";
    private static final String STUB_MISSING_NODE_TYPES_PROPERTY = "brut.configservice.stubMissingNodeTypes";

    private RuntimeTypeStubber() {
        // Utility class
    }

    /**
     * Registers a stub namespace if the given prefix is not already registered.
     *
     * @param session the JCR session
     * @param prefix  the namespace prefix to register
     * @throws RepositoryException if registration fails
     */
    public static void registerStubNamespace(Session session, String prefix) throws RepositoryException {
        String uri = "urn:brut:stub:" + prefix;
        try {
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
            LOG.warn("Stubbed missing namespace '{}' -> '{}' for delivery-tier tests. " +
                "Disable with -D{}=false", prefix, uri, STUB_MISSING_NAMESPACES_PROPERTY);
        } catch (NamespaceException e) {
            LOG.debug("Namespace '{}' already registered or conflict: {}", prefix, e.getMessage());
        }
    }

    /**
     * Registers a stub node type definition.
     * <p>
     * Creates a minimal CND that extends nt:base and allows any properties/children.
     *
     * @param session           the JCR session
     * @param nodeType          the node type name (prefix:localName or {uri}localName)
     * @param stubbedNamespaces set of already-stubbed namespaces (updated if needed)
     * @throws RepositoryException if registration fails
     */
    public static void registerStubNodeType(Session session, String nodeType, Set<String> stubbedNamespaces)
            throws RepositoryException {
        String prefix;
        String uri;
        String localName;
        String qualifiedName;

        // Handle expanded URI format: {uri}localName
        if (nodeType.startsWith("{")) {
            int braceEnd = nodeType.indexOf('}');
            if (braceEnd <= 1) {
                return;
            }
            uri = nodeType.substring(1, braceEnd);
            localName = nodeType.substring(braceEnd + 1);
            prefix = derivePrefixFromUri(uri);
            qualifiedName = prefix + ":" + localName;
        } else {
            // Handle prefix:localName format
            int colonIdx = nodeType.indexOf(':');
            if (colonIdx <= 0) {
                return;
            }
            prefix = nodeType.substring(0, colonIdx);
            localName = nodeType.substring(colonIdx + 1);
            uri = "urn:brut:stub:" + prefix;
            qualifiedName = nodeType;
        }

        try {
            session.getNamespaceURI(prefix);
        } catch (NamespaceException e) {
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
            stubbedNamespaces.add(prefix);
        }

        String cnd = String.format(
            "<%s='%s'>\n[%s] > nt:base\n  - * (UNDEFINED) multiple\n  + * (nt:base) = nt:base sns",
            prefix, uri, qualifiedName);

        try {
            CndImporter.registerNodeTypes(new StringReader(cnd), session);
            LOG.warn("Stubbed missing node type '{}' for delivery-tier tests. " +
                "Disable with -D{}=false", qualifiedName, STUB_MISSING_NODE_TYPES_PROPERTY);
        } catch (Exception e) {
            LOG.debug("Failed to register stub node type '{}': {}", qualifiedName, e.getMessage());
        }
    }

    /**
     * Extracts a namespace prefix from an exception message.
     *
     * @param message the exception message
     * @return the extracted prefix, or null if not found
     */
    public static String extractNamespacePrefix(String message) {
        if (message == null) {
            return null;
        }
        int colonIdx = message.indexOf(':');
        if (colonIdx > 0 && colonIdx < 50) {
            return message.substring(0, colonIdx).trim();
        }
        return null;
    }

    /**
     * Extracts a node type name from an exception message.
     * <p>
     * Handles both formats:
     * <ul>
     *   <li>Expanded URI: {http://example.org}localName</li>
     *   <li>Prefixed: prefix:localName</li>
     * </ul>
     *
     * @param message the exception message
     * @return the extracted node type, or null if not found
     */
    public static String extractNodeType(String message) {
        if (message == null) {
            return null;
        }

        // Handle expanded URI format: {http://...}localName
        int braceStart = message.indexOf('{');
        int braceEnd = message.indexOf('}', braceStart + 1);
        if (braceStart >= 0 && braceEnd > braceStart) {
            String uri = message.substring(braceStart + 1, braceEnd);
            int localEnd = braceEnd + 1;
            while (localEnd < message.length() && !Character.isWhitespace(message.charAt(localEnd))) {
                localEnd++;
            }
            String localName = message.substring(braceEnd + 1, localEnd);
            if (!localName.isEmpty() && isValidNCName(localName)) {
                return "{" + uri + "}" + localName;
            }
        }

        // Handle prefix:localName format
        int colonIdx = message.indexOf(':');
        if (colonIdx > 0) {
            int start = message.lastIndexOf(' ', colonIdx);
            String candidate = message.substring(start < 0 ? 0 : start + 1).trim();
            if (candidate.contains(":")) {
                int end = candidate.indexOf(' ');
                String nodeType = end > 0 ? candidate.substring(0, end) : candidate;
                if (isValidPrefixedNodeTypeName(nodeType)) {
                    return nodeType;
                }
            }
        }
        return null;
    }

    /**
     * Validates if a string is a valid prefixed node type name (prefix:localName).
     *
     * @param name the name to validate
     * @return true if valid
     */
    public static boolean isValidPrefixedNodeTypeName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        int colonIdx = name.indexOf(':');
        if (colonIdx <= 0 || colonIdx == name.length() - 1) {
            return false;
        }
        String prefix = name.substring(0, colonIdx);
        String localName = name.substring(colonIdx + 1);
        return isValidNCName(prefix) && isValidNCName(localName);
    }

    /**
     * Validates if a string is a valid NCName (non-colonized name) per XML spec.
     *
     * @param name the name to validate
     * @return true if valid
     */
    public static boolean isValidNCName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Derives a namespace prefix from a URI.
     * <p>
     * Extracts a meaningful segment from URIs like "http://www.onehippo.org/jcr/hippofacnav/nt/1.0.1"
     *
     * @param uri the namespace URI
     * @return a suitable prefix, or "stub" + hash if none found
     */
    public static String derivePrefixFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "stub";
        }
        // Extract meaningful segment from URI
        String[] segments = uri.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];
            if (segment.isEmpty() || segment.matches("\\d+\\.\\d+.*") || "nt".equals(segment)) {
                continue;
            }
            if (isValidNCName(segment) && segment.length() <= 20) {
                return segment;
            }
        }
        return "stub" + Math.abs(uri.hashCode() % 10000);
    }
}
