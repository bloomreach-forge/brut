package com.bloomreach.ps.brxm.jcr.repository.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        assertNull(PathUtils.normalizePath(null));
    }

}