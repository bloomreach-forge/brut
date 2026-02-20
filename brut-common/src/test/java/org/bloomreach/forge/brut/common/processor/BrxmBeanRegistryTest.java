package org.bloomreach.forge.brut.common.processor;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BrxmBeanRegistryTest {

    @Test
    void loadBeanClassNames_noRegistry_returnsEmpty() {
        ClassLoader empty = new URLClassLoader(new URL[0], null);
        Set<String> result = BrxmBeanRegistry.loadBeanClassNames(empty);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadBeanClassNames_withRegistry_returnsParsedFqns() throws Exception {
        Path dir = createTempRegistry(BrxmBeanRegistry.BEANS_RESOURCE,
                "com.example.beans.NewsDocument\ncom.example.beans.HeroBanner\n# comment\n\n");
        ClassLoader cl = new URLClassLoader(new URL[]{dir.toUri().toURL()}, null);

        Set<String> result = BrxmBeanRegistry.loadBeanClassNames(cl);

        assertEquals(2, result.size());
        assertTrue(result.contains("com.example.beans.NewsDocument"));
        assertTrue(result.contains("com.example.beans.HeroBanner"));
    }

    @Test
    void loadBeanClassNames_skipsBlankLinesAndComments() throws Exception {
        Path dir = createTempRegistry(BrxmBeanRegistry.BEANS_RESOURCE,
                "\n# this is a comment\n  \ncom.example.Foo\n");
        ClassLoader cl = new URLClassLoader(new URL[]{dir.toUri().toURL()}, null);

        Set<String> result = BrxmBeanRegistry.loadBeanClassNames(cl);

        assertEquals(1, result.size());
        assertTrue(result.contains("com.example.Foo"));
    }

    private Path createTempRegistry(String resourcePath, String content) throws Exception {
        Path dir = Files.createTempDirectory("brut-registry-test");
        dir.toFile().deleteOnExit();
        Path file = dir.resolve(Path.of(resourcePath));
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return dir;
    }
}
