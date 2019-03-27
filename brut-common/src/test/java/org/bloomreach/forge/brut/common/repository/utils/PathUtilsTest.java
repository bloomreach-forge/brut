package org.bloomreach.forge.brut.common.repository.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathUtilsTest {
    @Test
    public void normalizePath() throws Exception {
        assertEquals("some/path", PathUtils.normalizePath("/some/path/"));
        assertEquals("some/path", PathUtils.normalizePath("/some/path"));
        assertEquals("some/path", PathUtils.normalizePath("some/path"));
        assertEquals("some/path", PathUtils.normalizePath("some/path/"));
        assertEquals("", PathUtils.normalizePath("//"));
        assertEquals("", PathUtils.normalizePath("/"));
        assertEquals("", PathUtils.normalizePath(""));
        Assertions.assertNull(PathUtils.normalizePath(null));
    }

}