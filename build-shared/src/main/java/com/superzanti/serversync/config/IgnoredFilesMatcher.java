package com.superzanti.serversync.config;

import com.superzanti.serversync.util.Glob;

import java.nio.file.Path;

/**
 * A wrapper for: {@link Glob}.
 *
 * Matches against the user configured list of ignored files.
 *
 * @author Rheimus
 */
public class IgnoredFilesMatcher {
    private static final SyncConfig config = SyncConfig.getConfig();

    public static boolean matches(Path file) {
        return Glob.matches(file, config.FILE_IGNORE_LIST);
    }
}
