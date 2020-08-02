package com.superzanti.serversync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerSyncTest {

    final String TEST_ADDRESS = "test-address";
    final int TEST_PORT = 1234;

    @BeforeEach
    void setUp() {
        ServerSync.main(new String[]{"-a", "test-address", "-p", String.valueOf(TEST_PORT)});
    }

    @Test
    @DisplayName("Address argument should set config value")
    void addressPropertyIsSet() {
        SyncConfig config = SyncConfig.getConfig();
        assertEquals(TEST_ADDRESS, config.SERVER_IP);
    }

    @Test
    @DisplayName("Port argument should set config value")
    void portPropertyIsSet() {
        SyncConfig config = SyncConfig.getConfig();
        assertEquals(config.SERVER_PORT, TEST_PORT);
    }
}