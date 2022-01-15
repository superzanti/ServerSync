package com.superzanti.serversync;

import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.util.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class LineFeedTest {

    public LineFeedTest() {
        Logger.instantiate("test");
    }


    @Test
    @DisplayName("Should hash text files ignoring line feeds")
    void textHash() {
        // We know these resource files will not be null
        Path crlf = Path.of(getClass().getResource("/hash_crlf.txt").getPath().substring(1));
        Path lf = Path.of(getClass().getResource("/hash_lf.txt").getPath().substring(1));

        String hashLF = FileHash.hashFile(lf);
        String hashCRLF = FileHash.hashFile(crlf);

        Assertions.assertEquals(hashLF, hashCRLF);
    }
}