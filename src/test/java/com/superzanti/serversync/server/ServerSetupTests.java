package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.ServerSyncLogger;
import com.superzanti.serversync.util.enums.EServerMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ServerSetupTests {
    private SyncConfig config;

    @BeforeEach
    void init() {
        ServerSync.MODE = EServerMode.SERVER;
        ServerSyncLogger serverSyncLogger = new ServerSyncLogger("testing");
        config = SyncConfig.getConfig();
        config.DIRECTORY_INCLUDE_LIST = new ArrayList<>();
        config.PUSH_CLIENT_MODS = false;
    }

//    @Test
//    @DisplayName("Construction")
//    void construction() {
////        ServerSetup setup = new ServerSetup();
//    }

    @Test
    @DisplayName("Questions")
    void shouldPushClientOnlyFiles() {
        config.PUSH_CLIENT_MODS = false;
        ServerSetup setup = new ServerSetup();

        assertFalse(setup.shouldPushClientOnlyFiles());

        config.PUSH_CLIENT_MODS = true;
        setup = new ServerSetup();

        assertTrue(setup.shouldPushClientOnlyFiles());
    }
}
