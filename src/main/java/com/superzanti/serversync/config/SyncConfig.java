package com.superzanti.serversync.config;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.files.DirectoryEntry;
import com.superzanti.serversync.files.EDirectoryMode;
import com.superzanti.serversync.files.FileRedirect;
import com.superzanti.serversync.util.enums.EConfigType;
import com.superzanti.serversync.util.enums.EServerMode;
import com.superzanti.serversync.util.enums.ETheme;

import java.io.IOException;
import java.util.*;

/**
 * Handles all functionality to do with serversyncs config file and
 * other configuration properties
 *
 * @author Rheimus
 */
public class SyncConfig {
    public final EConfigType configType;

    // COMMON //////////////////////////////
    public String SERVER_IP = "127.0.0.1";
    public List<String> FILE_IGNORE_LIST = Arrays.asList("**/serversync-*.jar", "**/serversync-*.cfg");
    public List<String> CONFIG_INCLUDE_LIST = new ArrayList<>();
    public Locale LOCALE = Locale.getDefault();
    public ETheme THEME = ETheme.BLUE_YELLOW;
    public int BUFFER_SIZE = 1024 * 64;
    ////////////////////////////////////////

    // SERVER //////////////////////////////
    public int SERVER_PORT = 38067;
    public Boolean PUSH_CLIENT_MODS = false;
    public List<String> FILE_INCLUDE_LIST = Collections.singletonList("mods/**");
    public List<DirectoryEntry> DIRECTORY_INCLUDE_LIST = Collections.singletonList(new DirectoryEntry(
        "mods",
        EDirectoryMode.mirror
    ));
    public List<FileRedirect> REDIRECT_FILES_LIST = new ArrayList<>();
    public int SYNC_MODE = 2;
    ////////////////////////////////////////

    // CLIENT //////////////////////////////
    public Boolean REFUSE_CLIENT_MODS = false;
    ////////////////////////////////////////

    private static SyncConfig singleton;

    public SyncConfig() {
        this.configType = EConfigType.COMMON;
    }

    public SyncConfig(EConfigType type) {
        configType = type;
    }

    public static SyncConfig getConfig() {
        if (SyncConfig.singleton == null) {
            if (ServerSync.MODE == EServerMode.SERVER) {
                SyncConfig.singleton = new SyncConfig(EConfigType.SERVER);
            }
            if (ServerSync.MODE == EServerMode.CLIENT) {
                SyncConfig.singleton = new SyncConfig(EConfigType.CLIENT);
            }
        }
        return SyncConfig.singleton;
    }

    public void save() throws IOException {
        ConfigLoader.save(configType);
    }
}
