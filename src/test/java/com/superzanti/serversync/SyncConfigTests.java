package com.superzanti.serversync;

import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.ServerSyncLogger;
import com.superzanti.serversync.util.enums.EConfigType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class SyncConfigTests {
    private SyncConfig config;

    @BeforeEach
    void init() {
        ServerSyncLogger testServerSyncLogger = new ServerSyncLogger("testing");
        config = new SyncConfig(EConfigType.COMMON);
    }

    @Test
    @DisplayName("Default values")
    void fileIgnoreList() {
        assertTrue(config.FILE_IGNORE_LIST.contains("**/serversync-*.jar"), "should ignore ServerSync files");
        assertTrue(config.FILE_IGNORE_LIST.contains("**/serversync-*.cfg"), "should ignore ServerSync configuration");
    }
}
