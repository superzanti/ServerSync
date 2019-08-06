package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EConfigType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


public class ServerSetupTests {
    @BeforeEach
    void init() {
        Logger logger = new Logger("testing");
        ServerSync.CONFIG = new SyncConfig(EConfigType.COMMON);
        ServerSync.CONFIG.DIRECTORY_INCLUDE_LIST = new ArrayList<>();
        ServerSync.CONFIG.PUSH_CLIENT_MODS = false;
    }

    @Test
    @DisplayName("Construction")
    void construction() {
        ServerSetup setup = new ServerSetup();

        assertNotNull(ServerSetup.allFiles);
        assertNotNull(ServerSetup.clientOnlyFiles);
        assertNotNull(ServerSetup.configFiles);
        assertNotNull(ServerSetup.directories);
        assertNotNull(ServerSetup.standardFiles);
    }

    @Test
    @DisplayName("Questions")
    void shouldPushClientOnlyFiles() {
        ServerSync.CONFIG.PUSH_CLIENT_MODS = false;
        ServerSetup setup = new ServerSetup();

        assertFalse(setup.shouldPushClientOnlyFiles());

        ServerSync.CONFIG.PUSH_CLIENT_MODS = true;
        setup = new ServerSetup();

        assertTrue(setup.shouldPushClientOnlyFiles());
    }
}
