package com.superzanti.serversync.server;

import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.util.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ServerTests {
    @BeforeEach
    void init() {
        Logger logger = new Logger("testing");
    }

    @Test
    @DisplayName("Questions")
    void isClientOnlyMod() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Server testServer = new Server(null, 0);
        Method m = Server.class.getDeclaredMethod("isClientOnlyFile", String.class);
        m.setAccessible(true);

        String testPathNormal = "mods/foobar";
        assertFalse((Boolean) m.invoke(testServer, testPathNormal));

        String testPathClientOnly = FileManager.clientOnlyFilesDirectoryName + "/foobar";
        assertTrue((Boolean) m.invoke(testServer, testPathClientOnly));
    }
}
