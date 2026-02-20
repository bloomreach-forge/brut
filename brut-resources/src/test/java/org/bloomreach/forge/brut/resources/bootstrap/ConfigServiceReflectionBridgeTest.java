package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.jcr.ItemExistsException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
    class CachedMethodFields {

        @Test
        void applyNamespacesMethod_isCachedStaticFinal() throws Exception {
            Field f = ConfigServiceReflectionBridge.class.getDeclaredField("APPLY_NAMESPACES_METHOD");
            f.setAccessible(true);
            assertTrue(Modifier.isStatic(f.getModifiers()), "APPLY_NAMESPACES_METHOD must be static");
            assertTrue(Modifier.isFinal(f.getModifiers()), "APPLY_NAMESPACES_METHOD must be final");
            assertNotNull(f.get(null), "APPLY_NAMESPACES_METHOD must not be null at class load");
        }

        @Test
        void computeWriteDeltaMethod_isCachedStaticFinal() throws Exception {
            Field f = ConfigServiceReflectionBridge.class.getDeclaredField("COMPUTE_WRITE_DELTA_METHOD");
            f.setAccessible(true);
            assertTrue(Modifier.isStatic(f.getModifiers()), "COMPUTE_WRITE_DELTA_METHOD must be static");
            assertTrue(Modifier.isFinal(f.getModifiers()), "COMPUTE_WRITE_DELTA_METHOD must be final");
            assertNotNull(f.get(null), "COMPUTE_WRITE_DELTA_METHOD must not be null at class load");
        }

        @Test
        @SuppressWarnings("deprecation")
        void applyNamespacesMethod_isAccessible() throws Exception {
            Field f = ConfigServiceReflectionBridge.class.getDeclaredField("APPLY_NAMESPACES_METHOD");
            f.setAccessible(true);
            Method m = (Method) f.get(null);
            // isAccessible() is deprecated since Java 9 but is the only way to query the flag
            // without providing an instance (canAccess(null) throws for non-static methods).
            assertTrue(m.isAccessible(), "Cached method must have setAccessible(true) applied");
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
