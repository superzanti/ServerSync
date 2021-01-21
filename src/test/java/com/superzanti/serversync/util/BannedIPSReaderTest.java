package com.superzanti.serversync.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BannedIPSReaderTest {

    public BannedIPSReaderTest() {
        new Logger("test");
    }


    @Test
    @DisplayName("Should get ip entries from JSON array")
    void readValid() {
        List<String> entries = BannedIPSReader.read("[{\"ip\":\"0\"},{\"ip\":\"1\"}]");
        assertEquals(2, entries.size());
        assertEquals("0", entries.get(0));
        assertEquals("1", entries.get(1));
    }

    @Test
    @DisplayName("Should gracefully handle invalid JSON")
    void readInvalid() {
        List<String> entries = BannedIPSReader.read("");
        assertEquals(0, entries.size());
    }
}