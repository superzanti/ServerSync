package com.superzanti.serversync;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.util.Logger;

class LineFeedTest {

    public LineFeedTest() {
        Logger.instantiate("test");
    }

    @Test
    @DisplayName("Should hash text files ignoring line feeds")
    void textHash() {
        String hashLF = FileHash.hashFile(Paths.get(this.getClass().getResource("/hash_lf.txt").getPath()));
        String hashCRLF = FileHash.hashFile(Paths.get(this.getClass().getResource("/hash_crlf.txt").getPath()));
        assertEquals(hashLF, hashCRLF);
    }
}