package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.jcr.ItemExistsException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigServiceReflectionBridge.
 */
class ConfigServiceReflectionBridgeTest {

    @Nested
    class ItemExistsPathExtraction {

        @Test
        void extractsPathFromException() {
            ItemExistsException ex = new ItemExistsException("Item already exists:/hst:hst/node");
            String result = ConfigServiceReflectionBridge.extractItemExistsPath(ex);
            assertEquals("/hst:hst/node", result);
        }

        @Test
        void returnsNullForNull() {
            assertNull(ConfigServiceReflectionBridge.extractItemExistsPath(null));
        }

        @Test
        void returnsNullForNonItemExistsException() {
            Exception ex = new RuntimeException("Some other error");
            assertNull(ConfigServiceReflectionBridge.extractItemExistsPath(ex));
        }

        @Test
        void findsNestedItemExistsException() {
            ItemExistsException innerEx = new ItemExistsException("exists:/nested/path");
            Exception wrapperEx = new RuntimeException("Wrapper", innerEx);
            String result = ConfigServiceReflectionBridge.extractItemExistsPath(wrapperEx);
            assertEquals("/nested/path", result);
        }
    }

    @Nested
    class ItemExistsExceptionFinding {

        @Test
        void findsDirectException() {
            ItemExistsException ex = new ItemExistsException("test");
            ItemExistsException found = ConfigServiceReflectionBridge.findItemExistsException(ex);
            assertSame(ex, found);
        }

        @Test
        void findsWrappedException() {
            ItemExistsException innerEx = new ItemExistsException("inner");
            RuntimeException wrapperEx = new RuntimeException("wrapper", innerEx);
            ItemExistsException found = ConfigServiceReflectionBridge.findItemExistsException(wrapperEx);
            assertSame(innerEx, found);
        }

        @Test
        void returnsNullWhenNotFound() {
            Exception ex = new RuntimeException("no ItemExistsException in chain");
            assertNull(ConfigServiceReflectionBridge.findItemExistsException(ex));
        }

        @Test
        void returnsNullForNull() {
            assertNull(ConfigServiceReflectionBridge.findItemExistsException(null));
        }
    }
}
