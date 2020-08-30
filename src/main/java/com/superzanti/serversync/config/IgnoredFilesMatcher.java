package com.superzanti.serversync.config;

import com.superzanti.serversync.util.GlobPathMatcher;

import java.nio.file.Path;

/**
 * A wrapper for: {@link com.superzanti.serversync.util.GlobPathMatcher}.
 *
 * Matches against the user configured list of ignored files.
 *
 * @author Rheimus
 */
public class IgnoredFilesMatcher {
    private static final SyncConfig config = SyncConfig.getConfig();

    public static boolean matches(Path file) {
        return GlobPathMatcher.matches(file, config.FILE_IGNORE_LIST);
    }
}
