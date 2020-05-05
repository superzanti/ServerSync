package com.superzanti.serversync.config;

import com.superzanti.serversync.SyncConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonConfigTest {
    @ParameterizedTest
    @DisplayName("Parsing")
    @ValueSource(strings = {"src/test/resources/server-config.json"})
    void parseServerConfig(Path file) {
        try {
            SyncConfig config = JsonConfig.forServer(file);
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
}