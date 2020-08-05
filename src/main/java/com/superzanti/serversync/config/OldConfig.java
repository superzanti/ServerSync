package com.superzanti.serversync.config;

import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EConfigType;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfig;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfigReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class OldConfig {
    public static void forServer(Path file) throws IOException {
        SyncConfig config = SyncConfig.getConfig();
        FriendlyConfig _config = new FriendlyConfig();
        _config.readConfig(new FriendlyConfigReader(Files.newBufferedReader(file)));

        String couldNotFindString = "Could not find %s config entry";
        try {
            String localeString = _config.getEntryByName("LOCALE").getString();
            String[] localeParts = localeString.split("_");
            if (localeParts.length != 2) {
                Logger.error("Malformed locale string!");
                localeParts = new String[]{"en", "US"};
            }
            config.LOCALE = new Locale(localeParts[0], localeParts[1]);
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "LOCALE"));
        }
        try {
            config.FILE_IGNORE_LIST = _config.getEntryByName("FILE_IGNORE_LIST").getList();
        } catch (NullPointerException e) {
            // Specific conversion from old config files
            Logger.debug("Could not find FILE_IGNORE_LIST, looking for old MOD_IGNORE_LIST");
            try {
                config.FILE_IGNORE_LIST = _config.getEntryByName("MOD_IGNORE_LIST").getList();
            } catch (NullPointerException e2) {
                Logger.debug(String.format(couldNotFindString, "MOD_IGNORE_LIST"));
            }
        }
        try {
            config.SERVER_PORT = _config.getEntryByName("SERVER_PORT").getInt();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "SERVER_PORT"));
        }

        try {
            config.CONFIG_INCLUDE_LIST = _config.getEntryByName("CONFIG_INCLUDE_LIST").getList();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "CONFIG_INCLUDE_LIST"));
        }
        try {
            config.SYNC_MODE = _config.getEntryByName("SYNC_MODE").getInt();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "SYNC_MODE"));
        }
        try {
            config.PUSH_CLIENT_MODS = _config.getEntryByName("PUSH_CLIENT_MODS").getBoolean();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "PUSH_CLIENT_MODS"));
        }
        try {
            config.DIRECTORY_INCLUDE_LIST = _config.getEntryByName("DIRECTORY_INCLUDE_LIST").getList();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "DIRECTORY_INCLUDE_LIST"));
        }

        Logger.debug("finished loading old server config");
    }

    public static void forClient(Path file) throws IOException {
        SyncConfig config = SyncConfig.getConfig();
        FriendlyConfig _config = new FriendlyConfig();
        _config.readConfig(new FriendlyConfigReader(Files.newBufferedReader(file)));

        String couldNotFindString = "Could not find %s config entry";
        try {
            String localeString = _config.getEntryByName("LOCALE").getString();
            String[] localeParts = localeString.split("_");
            if (localeParts.length != 2) {
                Logger.error("Malformed locale string!");
                localeParts = new String[]{"en", "US"};
            }
            config.LOCALE = new Locale(localeParts[0], localeParts[1]);
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "LOCALE"));
        }
        try {
            config.FILE_IGNORE_LIST = _config.getEntryByName("FILE_IGNORE_LIST").getList();
        } catch (NullPointerException e) {
            // Specific conversion from old config files
            Logger.debug("Could not find FILE_IGNORE_LIST, looking for old MOD_IGNORE_LIST");
            try {
                config.FILE_IGNORE_LIST = _config.getEntryByName("MOD_IGNORE_LIST").getList();
            } catch (NullPointerException e2) {
                Logger.debug(String.format(couldNotFindString, "MOD_IGNORE_LIST"));
            }
        }
        try {
            config.SERVER_PORT = _config.getEntryByName("SERVER_PORT").getInt();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "SERVER_PORT"));
        }
        try {
            config.SERVER_IP = _config.getEntryByName("SERVER_IP").getString();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "SERVER_IP"));
        }
        try {
            config.REFUSE_CLIENT_MODS = _config.getEntryByName("REFUSE_CLIENT_MODS").getBoolean();
        } catch (NullPointerException e) {
            Logger.debug(String.format(couldNotFindString, "REFUSE_CLIENT_MODS"));
        }

        Logger.debug("finished loading old client config");
        new SyncConfig(EConfigType.CLIENT);
    }
}
