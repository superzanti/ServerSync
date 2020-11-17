package com.superzanti.serversync.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;

/**
 * Just a wrapper for using glob pattern matching, make sure to set a pattern to match against
 *
 * @author Rheimus
 */
public class Glob {
    private static String sanitizePattern(String pattern) {
        if (File.separator.equals("\\")) {
            return pattern.replaceAll("[/\\\\]", "\\\\\\\\"); // Gross
        } else {
            return pattern.replaceAll("[/\\\\]", File.separator);
        }
    }

    private static PathMatcher globMatcher(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }

    public static Optional<String> getPattern(Path path, List<String> patterns) {
        return patterns.stream().filter(p -> globMatcher(sanitizePattern(p)).matches(path)).findFirst();
    }

    public static boolean matches(Path path, String pattern) {
        return globMatcher(sanitizePattern(pattern)).matches(path);
    }

    public static boolean matches(Path path, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> globMatcher(sanitizePattern(pattern)).matches(path));
    }
}
