package org.bloomreach.forge.brut.common.repository.utils;

public class PathUtils {

    private PathUtils() {
        // utility class
    }

    public static String normalizePath(String path) {
        String result = path;
        if (path != null) {
            result = path.substring(path.startsWith("/") ? 1 : 0,
                    path.endsWith("/") && path.length() > 1 ? path.length() - 1 : path.length());
        }
        return result;
    }
}
