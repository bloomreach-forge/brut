package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Characterization tests for ConfigServiceBootstrapStrategy.
 * <p>
 * These tests document existing behavior before extracting classes.
 * Each nested class corresponds to a responsibility to be extracted.
 */
class ConfigServiceBootstrapStrategyTest {

    private ConfigServiceBootstrapStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ConfigServiceBootstrapStrategy();
    }

    // ========================================
    // Existing tests (canHandle, etc.)
    // ========================================

    @Test
    void testCanHandleReturnsFalseWhenNoHcmModule() {
        ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            emptyClassLoader
        );

        assertFalse(strategy.canHandle(context));
    }

    @Test
    void testCanHandleReturnsTrueWhenHcmModuleExists(@TempDir Path tempDir) throws IOException {
        Path metaInf = tempDir.resolve("META-INF");
        Files.createDirectories(metaInf);
        Path hcmModule = metaInf.resolve("hcm-module.yaml");
        Files.writeString(hcmModule, "group:\n  name: test\nproject: test\nmodule:\n  name: test\n");

        ClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()});
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            classLoader
        );

        assertTrue(strategy.canHandle(context));
    }

    @Test
    void testCanHandleReturnsTrueWhenModuleDescriptorsProvided(@TempDir Path tempDir) throws IOException {
        Path moduleDescriptor = tempDir.resolve("hcm-module.yaml");
        Files.writeString(moduleDescriptor, "group:\n  name: test\nproject: test\nmodule:\n  name: test\n");

        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(moduleDescriptor),
            new URLClassLoader(new URL[0], null)
        );

        assertTrue(strategy.canHandle(context));
    }

    @Test
    void testInitializeHstStructureThrowsWhenNoHcmModule() {
        Session mockSession = mock(Session.class);

        ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            emptyClassLoader
        );

        assertThrows(RepositoryException.class, () ->
            strategy.initializeHstStructure(mockSession, "myproject", context)
        );
    }

    // ========================================
    // HcmModuleLoader characterization tests
    // ========================================

    @Nested
    class HstRootPrefixStripping {

        @Test
        void stripHstRootPrefix_removesSlashHstColonPrefix() throws Exception {
            String result = invokeStripHstRootPrefix("/hst:myproject");
            assertEquals("myproject", result);
        }

        @Test
        void stripHstRootPrefix_removesHstColonPrefix() throws Exception {
            String result = invokeStripHstRootPrefix("hst:myproject");
            assertEquals("myproject", result);
        }

        @Test
        void stripHstRootPrefix_returnsUnchangedWhenNoPrefix() throws Exception {
            String result = invokeStripHstRootPrefix("myproject");
            assertEquals("myproject", result);
        }

        @Test
        void stripHstRootPrefix_handlesNull() throws Exception {
            String result = invokeStripHstRootPrefix(null);
            assertNull(result);
        }

        @Test
        void stripHstRootPrefix_handlesBlank() throws Exception {
            String result = invokeStripHstRootPrefix("  ");
            assertEquals("  ", result);
        }

        private String invokeStripHstRootPrefix(String hstRoot) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("stripHstRootPrefix", String.class);
            method.setAccessible(true);
            return (String) method.invoke(strategy, hstRoot);
        }
    }

    // ========================================
    // RuntimeTypeStubber tests have been moved to RuntimeTypeStubberTest
    // ========================================

    // ========================================
    // ConfigServiceReflectionBridge tests have been moved to ConfigServiceReflectionBridgeTest
    // ========================================

    // ========================================
    // Config filtering characterization tests
    // ========================================

    @Nested
    class ConfigRootFiltering {

        @Test
        void isAllowedConfigRoot_allowsMatchingPrefix() throws Exception {
            assertTrue(invokeIsAllowedConfigRoot("/hst:hst/hst:configurations", List.of("/hst:")));
        }

        @Test
        void isAllowedConfigRoot_rejectsNonMatchingPath() throws Exception {
            assertFalse(invokeIsAllowedConfigRoot("/hippo:configuration", List.of("/hst:")));
        }

        @Test
        void isAllowedConfigRoot_returnsFalseForNullPath() throws Exception {
            assertFalse(invokeIsAllowedConfigRoot(null, List.of("/hst:")));
        }

        @Test
        void isAllowedConfigRoot_handlesMultiplePrefixes() throws Exception {
            var prefixes = List.of("/hst:", "/content", "/webfiles");
            assertTrue(invokeIsAllowedConfigRoot("/content/documents", prefixes));
            assertTrue(invokeIsAllowedConfigRoot("/hst:hst", prefixes));
            assertFalse(invokeIsAllowedConfigRoot("/hippo:configuration", prefixes));
        }

        private boolean invokeIsAllowedConfigRoot(String rootPath, List<String> allowedRoots) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod(
                "isAllowedConfigRoot", String.class, List.class);
            method.setAccessible(true);
            return (boolean) method.invoke(strategy, rootPath, allowedRoots);
        }
    }

    @Nested
    class UnreachableRootExtraction {

        @Test
        void extractUnreachableRoot_extractsPathFromMessage() throws Exception {
            String result = invokeExtractUnreachableRoot(
                "ConfigurationModel contains unreachable node '/hippo:configuration/hippo:frontend/cms'");
            assertEquals("/hippo:configuration/hippo:frontend/cms", result);
        }

        @Test
        void extractUnreachableRoot_returnsNullForNull() throws Exception {
            String result = invokeExtractUnreachableRoot(null);
            assertNull(result);
        }

        @Test
        void extractUnreachableRoot_returnsNullForNoMarker() throws Exception {
            String result = invokeExtractUnreachableRoot("Some other error message");
            assertNull(result);
        }

        private String invokeExtractUnreachableRoot(String message) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("extractUnreachableRoot", String.class);
            method.setAccessible(true);
            return (String) method.invoke(strategy, message);
        }
    }

    @Nested
    class MissingPrimaryTypeExtraction {

        @Test
        void extractMissingPrimaryTypePath_extractsPathFromMessage() throws Exception {
            String msg = "Node '/hst:hst/hst:configurations/hst:default/hst:sitemap/sitemap.xml' "
                    + "defined at '...' is missing the required jcr:primaryType property.";
            assertEquals(
                "/hst:hst/hst:configurations/hst:default/hst:sitemap/sitemap.xml",
                invokeExtract(msg));
        }

        @Test
        void extractMissingPrimaryTypePath_returnsNullForNull() throws Exception {
            assertNull(invokeExtract(null));
        }

        @Test
        void extractMissingPrimaryTypePath_returnsNullForNoMarker() throws Exception {
            assertNull(invokeExtract("Some other error message"));
        }

        @Test
        void extractMissingPrimaryTypePath_returnsNullForUnreachableNodeMessage() throws Exception {
            // Must not cross-match the unreachable-node message format
            assertNull(invokeExtract("contains definition rooted at unreachable node '/foo/bar'"));
        }

        private String invokeExtract(String message) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class
                    .getDeclaredMethod("extractMissingPrimaryTypePath", String.class);
            method.setAccessible(true);
            return (String) method.invoke(strategy, message);
        }
    }

    @Nested
    class NamespaceUriExtraction {

        @Test
        void extractUnregisteredNamespaceUri_extractsUriFromMessage() throws Exception {
            String msg = "http://forge.onehippo.org/relateddocs/nt/1.1: is not a registered namespace uri.";
            assertEquals(
                "http://forge.onehippo.org/relateddocs/nt/1.1",
                invokeExtract(msg));
        }

        @Test
        void extractUnregisteredNamespaceUri_returnsNullForNull() throws Exception {
            assertNull(invokeExtract(null));
        }

        @Test
        void extractUnregisteredNamespaceUri_returnsNullForNoSuffix() throws Exception {
            assertNull(invokeExtract("some other namespace error message"));
        }

        @Test
        void extractUnregisteredNamespaceUri_handlesLeadingWhitespace() throws Exception {
            assertEquals(
                "http://example.org/ns",
                invokeExtract("  http://example.org/ns: is not a registered namespace uri."));
        }

        private String invokeExtract(String message) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class
                    .getDeclaredMethod("extractUnregisteredNamespaceUri", String.class);
            method.setAccessible(true);
            return (String) method.invoke(strategy, message);
        }
    }

    @Nested
    class NamespaceConflictInfoExtraction {

        @Test
        void extractNamespaceConflictInfo_extractsPrefixAndTarget() throws Exception {
            String msg = "namespace with prefix 'autoexport' already exists in repository with different URI. "
                    + "Existing: 'http://www.onehippo.org/autoexport/1.0', "
                    + "target: 'http://www.onehippo.org/autoexport/1.1'. "
                    + "Changing existing namespaces is not supported. Aborting.";
            String[] result = invokeExtract(msg);
            assertNotNull(result);
            assertEquals("autoexport", result[0]);
            assertEquals("http://www.onehippo.org/autoexport/1.1", result[1]);
        }

        @Test
        void extractNamespaceConflictInfo_returnsNullForNull() throws Exception {
            assertNull(invokeExtract(null));
        }

        @Test
        void extractNamespaceConflictInfo_returnsNullForNonConflictMessage() throws Exception {
            assertNull(invokeExtract("some unrelated namespace error"));
        }

        @Test
        void extractNamespaceConflictInfo_returnsNullWhenNoTargetClause() throws Exception {
            assertNull(invokeExtract("namespace with prefix 'foo' already exists in repository with different URI."));
        }

        private String[] invokeExtract(String message) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class
                    .getDeclaredMethod("extractNamespaceConflictInfo", String.class);
            method.setAccessible(true);
            return (String[]) method.invoke(strategy, message);
        }
    }

    @Nested
    class NotMixinNodeTypeExtraction {

        @Test
        void extractNotMixinNodeType_extractsQualifiedName() throws Exception {
            assertEquals("relateddocs:relatabledocs",
                invokeExtract("relateddocs:relatabledocs is not a mixin node type"));
        }

        @Test
        void extractNotMixinNodeType_returnsNullForNull() throws Exception {
            assertNull(invokeExtract(null));
        }

        @Test
        void extractNotMixinNodeType_returnsNullForNoSuffix() throws Exception {
            assertNull(invokeExtract("some other error message"));
        }

        private String invokeExtract(String message) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class
                    .getDeclaredMethod("extractNotMixinNodeType", String.class);
            method.setAccessible(true);
            return (String) method.invoke(strategy, message);
        }
    }

    @Nested
    class InvalidSupertypeExtraction {

        @Test
        void extractInvalidSupertype_extractsSupertypeFromMessage() throws Exception {
            String msg = "[{http://www.onehippo.org/grandvision/nt/1.0}contentDocument] "
                    + "invalid supertype: {http://forge.onehippo.org/relateddocs/nt/1.1}relatabledocs";
            assertEquals(
                "{http://forge.onehippo.org/relateddocs/nt/1.1}relatabledocs",
                invokeExtract(msg));
        }

        @Test
        void extractInvalidSupertype_returnsNullForNull() throws Exception {
            assertNull(invokeExtract(null));
        }

        @Test
        void extractInvalidSupertype_returnsNullForNoMarker() throws Exception {
            assertNull(invokeExtract("some unrelated node type error"));
        }

        @Test
        void extractInvalidSupertype_returnsNullForBlankAfterMarker() throws Exception {
            assertNull(invokeExtract("invalid supertype: "));
        }

        private String invokeExtract(String message) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class
                    .getDeclaredMethod("extractInvalidSupertype", String.class);
            method.setAccessible(true);
            return (String) method.invoke(strategy, message);
        }
    }

    @Nested
    class MissingDependencyParsing {

        @Test
        void parseMissingDependency_parsesGroupDependency() throws Exception {
            Object result = invokeParseMissingDependency("Group 'my-group' is missing dependency 'hippo-cms'");
            assertNotNull(result);
            assertEquals("GROUP", getFieldValue(result, "type").toString());
            assertEquals("hippo-cms", getFieldValue(result, "missing"));
        }

        @Test
        void parseMissingDependency_parsesModuleDependency() throws Exception {
            Object result = invokeParseMissingDependency("Module 'my-module' is missing dependency 'core-module'");
            assertNotNull(result);
            assertEquals("MODULE", getFieldValue(result, "type").toString());
            assertEquals("my-module", getFieldValue(result, "owner"));
            assertEquals("core-module", getFieldValue(result, "missing"));
        }

        @Test
        void parseMissingDependency_returnsNullForNull() throws Exception {
            Object result = invokeParseMissingDependency(null);
            assertNull(result);
        }

        @Test
        void parseMissingDependency_returnsNullForUnrecognizedFormat() throws Exception {
            Object result = invokeParseMissingDependency("Unknown error format");
            assertNull(result);
        }

        private Object invokeParseMissingDependency(String message) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("parseMissingDependency", String.class);
            method.setAccessible(true);
            return method.invoke(strategy, message);
        }

        private Object getFieldValue(Object obj, String fieldName) throws Exception {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        }
    }

    // ========================================
    // Content source filtering characterization tests
    // ========================================

    @Nested
    class ContentSourcePathNormalization {

        @Test
        void normalizeContentSourcePath_removesLeadingSlash() throws Exception {
            String result = invokeNormalizeContentSourcePath("/content/docs");
            assertEquals("content/docs", result);
        }

        @Test
        void normalizeContentSourcePath_removesTrailingSlash() throws Exception {
            String result = invokeNormalizeContentSourcePath("content/docs/");
            assertEquals("content/docs", result);
        }

        @Test
        void normalizeContentSourcePath_normalizesBackslashes() throws Exception {
            String result = invokeNormalizeContentSourcePath("content\\docs\\test");
            assertEquals("content/docs/test", result);
        }

        @Test
        void normalizeContentSourcePath_returnsEmptyForNull() throws Exception {
            String result = invokeNormalizeContentSourcePath(null);
            assertEquals("", result);
        }

        @Test
        void normalizeContentSourcePath_trimsWhitespace() throws Exception {
            String result = invokeNormalizeContentSourcePath("  content/docs  ");
            assertEquals("content/docs", result);
        }

        private String invokeNormalizeContentSourcePath(String value) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("normalizeContentSourcePath", String.class);
            method.setAccessible(true);
            return (String) method.invoke(strategy, value);
        }
    }

    @Nested
    class ContentRootMatching {

        @Test
        void matchesContentRoot_matchesExactPath() throws Exception {
            assertTrue(invokeMatchesContentRoot("content", "content"));
        }

        @Test
        void matchesContentRoot_matchesSubPath() throws Exception {
            assertTrue(invokeMatchesContentRoot("content/documents", "content"));
        }

        @Test
        void matchesContentRoot_matchesSegmentInMiddle() throws Exception {
            assertTrue(invokeMatchesContentRoot("hcm-content/content/documents", "content"));
        }

        @Test
        void matchesContentRoot_matchesEndingSegment() throws Exception {
            assertTrue(invokeMatchesContentRoot("hcm-content/content", "content"));
        }

        @Test
        void matchesContentRoot_rejectsMismatch() throws Exception {
            assertFalse(invokeMatchesContentRoot("other/path", "content"));
        }

        @Test
        void matchesContentRoot_returnsFalseForNull() throws Exception {
            assertFalse(invokeMatchesContentRoot(null, "content"));
        }

        @Test
        void matchesContentRoot_returnsFalseForEmpty() throws Exception {
            assertFalse(invokeMatchesContentRoot("", "content"));
        }

        private boolean invokeMatchesContentRoot(String candidate, String root) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod(
                "matchesContentRoot", String.class, String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(strategy, candidate, root);
        }
    }

    @Nested
    class ModelCaching {

        @Test
        void modelCache_isStaticConcurrentHashMap() throws Exception {
            Field f = ConfigServiceBootstrapStrategy.class.getDeclaredField("MODEL_CACHE");
            f.setAccessible(true);
            assertTrue(Modifier.isStatic(f.getModifiers()), "MODEL_CACHE must be static");
            assertInstanceOf(ConcurrentHashMap.class, f.get(null), "MODEL_CACHE must be a ConcurrentHashMap");
        }

        @Test
        void clearModelCache_isCallable() throws Exception {
            Method m = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("clearModelCache");
            m.setAccessible(true);
            // Must not throw; clears the cache for test isolation
            assertDoesNotThrow(() -> m.invoke(null));
        }

        @Test
        void modelCache_populatedAfterExplicitModuleLoad(@TempDir Path tempDir) throws Exception {
            // Ensure cache is empty before this test
            Method clearCache = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("clearModelCache");
            clearCache.setAccessible(true);
            clearCache.invoke(null);

            // Create a minimal module descriptor
            Path metaInf = tempDir.resolve("META-INF");
            Files.createDirectories(metaInf);
            Path hcmModule = metaInf.resolve("hcm-module.yaml");
            Files.writeString(hcmModule, "group:\n  name: test\nproject: test\nmodule:\n  name: test\n");

            BootstrapContext context = new BootstrapContext(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                List.of(hcmModule), new URLClassLoader(new URL[]{tempDir.toUri().toURL()}));

            Method loadModules = ConfigServiceBootstrapStrategy.class.getDeclaredMethod(
                "loadModulesExplicitly", BootstrapContext.class);
            loadModules.setAccessible(true);

            // First call populates the cache
            loadModules.invoke(strategy, context);

            Field cacheField = ConfigServiceBootstrapStrategy.class.getDeclaredField("MODEL_CACHE");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> cache = (Map<String, ?>) cacheField.get(null);
            assertEquals(1, cache.size(), "Cache must contain exactly one entry after first load");

            // Second call with identical context must not grow the cache
            loadModules.invoke(strategy, context);
            assertEquals(1, cache.size(), "Cache size must not grow on second call with same context");

            clearCache.invoke(null);
        }
    }

    @Nested
    class SafeRootPrefixCheck {

        @Test
        void isSafeRootPrefix_acceptsHstPrefix_whenNoProjectNamespaceKnown() throws Exception {
            // Conservative fallback: strategy freshly created (projectNamespace=null)
            assertTrue(invokeIsSafeRootPrefix("/hst:hst"));
        }

        @Test
        void isSafeRootPrefix_acceptsContentPrefix() throws Exception {
            assertTrue(invokeIsSafeRootPrefix("/content/documents"));
        }

        @Test
        void isSafeRootPrefix_acceptsWebfilesPrefix() throws Exception {
            assertTrue(invokeIsSafeRootPrefix("/webfiles/site"));
        }

        @Test
        void isSafeRootPrefix_acceptsNamespacesPrefix_whenNoProjectNamespaceKnown() throws Exception {
            // Conservative fallback: when project namespace is unknown, protect all /hippo:namespaces paths
            assertTrue(invokeIsSafeRootPrefix("/hippo:namespaces/myns"));
        }

        @Test
        void isSafeRootPrefix_rejectsUnsafeRoot() throws Exception {
            assertFalse(invokeIsSafeRootPrefix("/hippo:configuration"));
        }

        @Test
        void isSafeRootPrefix_returnsFalseForNull() throws Exception {
            assertFalse(invokeIsSafeRootPrefix(null));
        }

        private boolean invokeIsSafeRootPrefix(String root) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("isSafeRootPrefix", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(strategy, root);
        }
    }

    @Nested
    class ProjectNamespaceScopedSafeCheck {

        @Test
        void isSafeRootPrefix_platformNamespaceExtensionNotSafeWhenProjectNamespaceKnown() throws Exception {
            // addon module extending /hippo:namespaces/hippogallery — must be prunable when project is known
            setProjectNamespace("myproject");
            assertFalse(invokeIsSafeRootPrefix(
                "/hippo:namespaces/hippogallery/image/editor:templates/_default_/display"));
        }

        @Test
        void isSafeRootPrefix_projectNamespaceIsSafeWhenKnown() throws Exception {
            setProjectNamespace("myproject");
            assertTrue(invokeIsSafeRootPrefix("/hippo:namespaces/myproject/news"));
        }

        @Test
        void isSafeRootPrefix_allNamespacesSafeWhenProjectNamespaceNull() throws Exception {
            // projectNamespace is null on a freshly-created strategy — conservative fallback
            assertNull(getProjectNamespace());
            assertTrue(invokeIsSafeRootPrefix("/hippo:namespaces/hippogallery/image"));
        }

        @Test
        void isSafeRootPrefix_allNamespacesSafeWhenProjectNamespaceBlank() throws Exception {
            setProjectNamespace("  ");
            assertTrue(invokeIsSafeRootPrefix("/hippo:namespaces/hippogallery/image"));
        }

        @Test
        void isSafeRootPrefix_hstBlueprintsNotSafeWhenProjectNamespaceKnown() throws Exception {
            setProjectNamespace("myproject");
            assertFalse(invokeIsSafeRootPrefix("/hst:hst/hst:blueprints/new-addon-website"));
        }

        @Test
        void isSafeRootPrefix_hstHstPathNotSafeWhenProjectNamespaceKnown() throws Exception {
            setProjectNamespace("myproject");
            assertFalse(invokeIsSafeRootPrefix("/hst:hst/hst:configurations/other"));
        }

        @Test
        void isSafeRootPrefix_projectHstRootIsSafeWhenKnown() throws Exception {
            setProjectNamespace("myproject");
            assertTrue(invokeIsSafeRootPrefix("/hst:myproject/hst:hosts"));
        }

        @Test
        void isSafeRootPrefix_hstSafeForAllWhenProjectNamespaceNull() throws Exception {
            // projectNamespace is null on a freshly-created strategy — conservative fallback
            assertNull(getProjectNamespace());
            assertTrue(invokeIsSafeRootPrefix("/hst:hst/hst:blueprints/anything"));
        }

        private void setProjectNamespace(String value) throws Exception {
            Field field = ConfigServiceBootstrapStrategy.class.getDeclaredField("projectNamespace");
            field.setAccessible(true);
            field.set(strategy, value);
        }

        private String getProjectNamespace() throws Exception {
            Field field = ConfigServiceBootstrapStrategy.class.getDeclaredField("projectNamespace");
            field.setAccessible(true);
            return (String) field.get(strategy);
        }

        private boolean invokeIsSafeRootPrefix(String root) throws Exception {
            Method method = ConfigServiceBootstrapStrategy.class.getDeclaredMethod("isSafeRootPrefix", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(strategy, root);
        }
    }
}
