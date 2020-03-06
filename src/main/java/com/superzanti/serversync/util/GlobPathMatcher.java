package com.superzanti.serversync.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/**
 * Just a wrapper for using glob pattern matching, make sure to set a pattern to match against
 *
 * @author Rheimus
 */
public class GlobPathMatcher {
    private static String sanitizePattern(String pattern) {
        if (File.separator.equals("\\")) {
            return pattern.replaceAll("[/\\\\]", "\\\\\\\\"); // Gross
        } else {
            return pattern.replaceAll("[/\\\\]", File.separator);
        }
    }

    public static boolean matches(Path path, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> {
            String sanitizedPattern = sanitizePattern(pattern);
            PathMatcher globMatcher = FileSystems.getDefault().getPathMatcher("glob:" + sanitizedPattern);
            return globMatcher.matches(path);
        });
    }
}
