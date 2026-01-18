package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RuntimeTypeStubber.
 */
class RuntimeTypeStubberTest {

    @Nested
    class NamespacePrefixExtraction {

        @Test
        void extractsFromColonMessage() {
            String result = RuntimeTypeStubber.extractNamespacePrefix("hippofacnav: No namespace");
            assertEquals("hippofacnav", result);
        }

        @Test
        void returnsNullForNoColon() {
            assertNull(RuntimeTypeStubber.extractNamespacePrefix("No colon in message"));
        }

        @Test
        void returnsNullForNull() {
            assertNull(RuntimeTypeStubber.extractNamespacePrefix(null));
        }

        @Test
        void ignoresColonAfterPosition50() {
            String longMessage = "a".repeat(60) + ":suffix";
            assertNull(RuntimeTypeStubber.extractNamespacePrefix(longMessage));
        }
    }

    @Nested
    class NodeTypeExtraction {

        @Test
        void extractsPrefixedNameFromSimpleMessage() {
            String result = RuntimeTypeStubber.extractNodeType("hippofacnav:facetnavigation");
            assertEquals("hippofacnav:facetnavigation", result);
        }

        @Test
        void extractsFromMessageWithPrefix() {
            String result = RuntimeTypeStubber.extractNodeType("Unknown type hippofacnav:facetnavigation not found");
            assertEquals("hippofacnav:facetnavigation", result);
        }

        @Test
        void extractsExpandedUri() {
            String result = RuntimeTypeStubber.extractNodeType("{http://www.onehippo.org/jcr/hippofacnav/nt/1.0.1}facetnavigation");
            assertEquals("{http://www.onehippo.org/jcr/hippofacnav/nt/1.0.1}facetnavigation", result);
        }

        @Test
        void returnsNullForNull() {
            assertNull(RuntimeTypeStubber.extractNodeType(null));
        }

        @Test
        void returnsNullForNoColon() {
            assertNull(RuntimeTypeStubber.extractNodeType("No colon anywhere"));
        }

        @Test
        void returnsNullWhenColonPrefixInterferes() {
            assertNull(RuntimeTypeStubber.extractNodeType("No such node type: hippofacnav:facetnavigation"));
        }
    }

    @Nested
    class NCNameValidation {

        @Test
        void acceptsValidName() {
            assertTrue(RuntimeTypeStubber.isValidNCName("validName"));
        }

        @Test
        void acceptsUnderscore() {
            assertTrue(RuntimeTypeStubber.isValidNCName("_name"));
        }

        @Test
        void acceptsNameWithDash() {
            assertTrue(RuntimeTypeStubber.isValidNCName("valid-name"));
        }

        @Test
        void acceptsNameWithDot() {
            assertTrue(RuntimeTypeStubber.isValidNCName("valid.name"));
        }

        @Test
        void acceptsNameWithNumbers() {
            assertTrue(RuntimeTypeStubber.isValidNCName("name123"));
        }

        @Test
        void rejectsNull() {
            assertFalse(RuntimeTypeStubber.isValidNCName(null));
        }

        @Test
        void rejectsEmpty() {
            assertFalse(RuntimeTypeStubber.isValidNCName(""));
        }

        @Test
        void rejectsStartingWithNumber() {
            assertFalse(RuntimeTypeStubber.isValidNCName("123name"));
        }

        @Test
        void rejectsStartingWithDash() {
            assertFalse(RuntimeTypeStubber.isValidNCName("-name"));
        }
    }

    @Nested
    class PrefixedNodeTypeNameValidation {

        @Test
        void acceptsValidPrefixedName() {
            assertTrue(RuntimeTypeStubber.isValidPrefixedNodeTypeName("hippo:document"));
        }

        @Test
        void rejectsNull() {
            assertFalse(RuntimeTypeStubber.isValidPrefixedNodeTypeName(null));
        }

        @Test
        void rejectsEmpty() {
            assertFalse(RuntimeTypeStubber.isValidPrefixedNodeTypeName(""));
        }

        @Test
        void rejectsNoColon() {
            assertFalse(RuntimeTypeStubber.isValidPrefixedNodeTypeName("document"));
        }

        @Test
        void rejectsColonAtStart() {
            assertFalse(RuntimeTypeStubber.isValidPrefixedNodeTypeName(":document"));
        }

        @Test
        void rejectsColonAtEnd() {
            assertFalse(RuntimeTypeStubber.isValidPrefixedNodeTypeName("hippo:"));
        }
    }

    @Nested
    class UriPrefixDerivation {

        @Test
        void extractsFromStandardUri() {
            String result = RuntimeTypeStubber.derivePrefixFromUri("http://www.onehippo.org/jcr/hippofacnav/nt/1.0.1");
            assertEquals("hippofacnav", result);
        }

        @Test
        void skipsVersionSegments() {
            String result = RuntimeTypeStubber.derivePrefixFromUri("http://example.org/ns/myns/2.0.0");
            assertEquals("myns", result);
        }

        @Test
        void returnsStubForNull() {
            assertEquals("stub", RuntimeTypeStubber.derivePrefixFromUri(null));
        }

        @Test
        void returnsStubForEmpty() {
            assertEquals("stub", RuntimeTypeStubber.derivePrefixFromUri(""));
        }
    }
}
