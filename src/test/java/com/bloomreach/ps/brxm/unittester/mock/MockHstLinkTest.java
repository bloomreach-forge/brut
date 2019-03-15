package com.bloomreach.ps.brxm.unittester.mock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MockHstLinkTest {

    public static final String BASE_URL = "https://www.bloomreach.com";

    @Test
    public void toUrlForm() throws Exception {
        verifyCaseWithoutSubpath("/", null);
        verifyCaseWithoutSubpath("/", "");
        verifyCaseWithoutSubpath("/", "/");
        verifyCaseWithoutSubpath("/path", "path");
        verifyCaseWithoutSubpath("/path", "/path");
        verifyCaseWithoutSubpath("/path", "path/");
        verifyCaseWithoutSubpath("/path", "/path/");

        MockHstLink link = new MockHstLink(BASE_URL, "path", "subpath");
        assertEquals(BASE_URL + "/path./subpath", link.toUrlForm(null, true));
        assertEquals("/path./subpath", link.toUrlForm(null, false));
    }

    private void verifyCaseWithoutSubpath(String expected, String path) {
        MockHstLink link = new MockHstLink(BASE_URL, path);
        assertEquals(BASE_URL + expected, link.toUrlForm(null, true));
        assertEquals(expected, link.toUrlForm(null, false));
    }

}