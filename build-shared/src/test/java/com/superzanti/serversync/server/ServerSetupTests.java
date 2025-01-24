package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSyncUtility;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EServerMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ServerSetupTests {
    private SyncConfig config;

    @BeforeEach
    void init() {
        ServerSyncUtility.MODE = EServerMode.SERVER;
        Logger.instantiate("testing");
    }

    @Test
    @DisplayName("Questions")
    void shouldPushClientOnlyFilesWhenPushClientModsIsTrue() {
        SyncConfig.getConfig().DIRECTORY_INCLUDE_LIST = List.of();
        SyncConfig.getConfig().PUSH_CLIENT_MODS = true;
        ServerSetup setup = new ServerSetup();

        assertTrue(setup.shouldPushClientOnlyFiles());
    }

    @Test
    @DisplayName("Questions")
    void shouldNotPushClientOnlyFilesWhenPushClientModsIsFalse() {
        SyncConfig.getConfig().DIRECTORY_INCLUDE_LIST = List.of();
        SyncConfig.getConfig().PUSH_CLIENT_MODS = false;
        ServerSetup setupTrue = new ServerSetup();

        assertFalse(setupTrue.shouldPushClientOnlyFiles());
    }
}
