package com.superzanti.serversync.util;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.enums.EServerMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GlobPathMatcherTests {
    SyncConfig config;

    GlobPathMatcherTests() {
        ServerSync.MODE = EServerMode.CLIENT;
        new Logger("testing");
        config = SyncConfig.getConfig();
    }

    @BeforeEach
    void init() {
    }

    @Test
    @DisplayName("Standard match behavior")
    void standardMatches() {
        final Path ignoredFile = Paths.get("ignored-file.test");
        final Path ignoredFile2 = Paths.get("nested/ignored-file.test");
        final Path ignoredDirectory = Paths.get("ignored-directory");
        final Path includedFile = Paths.get("included-file.test");

        config.FILE_IGNORE_LIST = Arrays.asList("ignored-file.test", "nested/ignored-file.test", "ignored-directory");

        assertTrue(GlobPathMatcher.matches(ignoredFile, config.FILE_IGNORE_LIST));
        assertTrue(GlobPathMatcher.matches(ignoredFile2, config.FILE_IGNORE_LIST));
        assertTrue(GlobPathMatcher.matches(ignoredDirectory, config.FILE_IGNORE_LIST));
        assertFalse(GlobPathMatcher.matches(includedFile, config.FILE_IGNORE_LIST));
    }

    @Test
    @DisplayName("Glob match behavior")
    void globMatches() {
        final Path ignoredFileWild = Paths.get("wild-ignored-file.test");
        final Path ignoredFileAnywhere = Paths.get("nested/deep-ignore.test");
        final Path ignoredFileAnywhere2 = Paths.get("nested2/sub/deep-ignore.test");
        final Path ignoredFileAnything = Paths.get("anything-shallow/ignored-file.test");
        final Path ignoredFileAnything2 = Paths.get("anything-deep/ignored-file.test");
        final Path ignoredFileAnything3 = Paths.get("anything-deep/nested/ignored-file.test");

        final Path includedFile = Paths.get("anything-shallow/nested/included-file.test");
        final Path includedFile2 = Paths.get("nested/wild-included.test");

        config.FILE_IGNORE_LIST = Arrays.asList("wild-ignored*", "**/deep-ignore.test", "anything-shallow/*", "anything-deep/**");

        assertTrue(GlobPathMatcher.matches(ignoredFileWild, config.FILE_IGNORE_LIST));
        assertTrue(GlobPathMatcher.matches(ignoredFileAnywhere, config.FILE_IGNORE_LIST));
        assertTrue(GlobPathMatcher.matches(ignoredFileAnywhere2, config.FILE_IGNORE_LIST));
        assertTrue(GlobPathMatcher.matches(ignoredFileAnything, config.FILE_IGNORE_LIST));
        assertTrue(GlobPathMatcher.matches(ignoredFileAnything2, config.FILE_IGNORE_LIST));
        assertTrue(GlobPathMatcher.matches(ignoredFileAnything3, config.FILE_IGNORE_LIST));

        assertFalse(GlobPathMatcher.matches(includedFile, config.FILE_IGNORE_LIST));
        assertFalse(GlobPathMatcher.matches(includedFile2, config.FILE_IGNORE_LIST));
    }
}
