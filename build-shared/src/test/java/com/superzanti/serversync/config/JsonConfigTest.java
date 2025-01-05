package com.superzanti.serversync.config;

import com.superzanti.serversync.ServerSyncUtility;
import com.superzanti.serversync.util.enums.EServerMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonConfigTest {
    @BeforeEach()
    void beforeEach() {

    }

    @ParameterizedTest
    @DisplayName("Should parse")
    @ValueSource(strings = {"src/test/resources/server-config.json"})
    void parseServerConfig(Path file) {
        ServerSyncUtility.MODE = EServerMode.SERVER;
        try {
            JsonConfig.forServer(file);
            SyncConfig config = SyncConfig.getConfig();
            assertNotNull(config.PUSH_CLIENT_MODS);
            assertNotNull(config.SYNC_MODE);
            assertNotNull(config.SERVER_PORT);
            assertNotNull(config.DIRECTORY_INCLUDE_LIST);
            assertNotNull(config.FILE_IGNORE_LIST);
            assertNotNull(config.LOCALE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest
    @DisplayName("Should parse single part locales")
    @ValueSource(strings = {"src/test/resources/server-single-part-locale-config.json"})
    void parseSinglePartLocale(Path file) {
        ServerSyncUtility.MODE = EServerMode.SERVER;
        try {
            JsonConfig.forServer(file);
            SyncConfig config = SyncConfig.getConfig();
            assertNotNull(config.LOCALE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}