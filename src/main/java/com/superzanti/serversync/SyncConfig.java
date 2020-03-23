package com.superzanti.serversync;

import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EConfigType;
import com.superzanti.serversync.util.enums.ELocations;
import com.superzanti.serversync.util.enums.EServerMode;
import com.superzanti.serversync.util.minecraft.config.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Handles all functionality to do with serversyncs config file and
 * other configuration properties
 *
 * @author Rheimus
 */
public class SyncConfig {
    private static final String CONFIG_LOCATION = ELocations.CONFIG.getValue();
    private static final String DEFAULT_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_PORT = 38067;
    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_RULES = "rules";
    private static final String CATEGORY_CONNECTION = "serverconnection";
    private static final String CATEGORY_OTHER = "misc";

    private FriendlyConfig config;

    private Path configPath;
    public final EConfigType configType;
    // COMMON //////////////////////////////
    public String SERVER_IP;
    public String LAST_UPDATE;
    public List<String> FILE_IGNORE_LIST = new ArrayList<>();
    public List<String> CONFIG_INCLUDE_LIST;
    public Locale LOCALE;
    ////////////////////////////////////////

    // SERVER //////////////////////////////
    public int SERVER_PORT;
    public Boolean PUSH_CLIENT_MODS;
    public List<String> DIRECTORY_INCLUDE_LIST;
    ////////////////////////////////////////

    // CLIENT //////////////////////////////
    public Boolean REFUSE_CLIENT_MODS = false;
    ////////////////////////////////////////

    public static boolean pullServerConfig = true;

    private static SyncConfig singleton;

    public SyncConfig(EConfigType type) {
        // Adding ServerSyncs internal files to the ignored list by default
        // This stops SS from being deleted / synced when loaded via forge
        // and stops the client from deleting the config files.
        FILE_IGNORE_LIST.add("**/serversync-*.jar");
        FILE_IGNORE_LIST.add("**/serversync-*.cfg");

        configType = type;
        config = new FriendlyConfig();
        if (configType == EConfigType.SERVER) {
            configPath = Paths.get(CONFIG_LOCATION + File.separator + "serversync-server.cfg");
        } else {
            configPath = Paths.get(CONFIG_LOCATION + File.separator + "serversync-client.cfg");
        }

        if (!Files.exists(configPath.getParent())) {
            try {
                Files.createDirectories(configPath.getParent());
            } catch (IOException e) {
                Logger.debug("Failed to create directories for: " + configPath.toString());
            }
        }

        if (!Files.exists(configPath)) {
            createConfiguraton();
        } else {
            readExistingConfiguration();
        }
        init();
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

    private void readExistingConfiguration() {
        try {
            config.readConfig(new FriendlyConfigReader(Files.newBufferedReader(configPath)));
        } catch (IOException e) {
            Logger.debug("Failed to read config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean createConfiguraton() {
        try {
            Files.createFile(configPath);
        } catch (IOException e) {
            Logger.debug("Failed to create config file: " + e.getMessage());
            return false;
        }

        if (configType == EConfigType.SERVER) {
            createServerConfiguration();
        } else {
            createClientConfiguration();
        }
        return true;
    }

    private void createClientConfiguration() {
        FriendlyConfigCategory general = new FriendlyConfigCategory(SyncConfig.CATEGORY_GENERAL);
        general.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_GENERAL,
            "REFUSE_CLIENT_MODS",
            false,
            new String[]{"# Set this to true to refuse client mods pushed by the server, [default: false]"}
        ));

        FriendlyConfigCategory rules = new FriendlyConfigCategory(SyncConfig.CATEGORY_RULES);
        rules.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_RULES,
            "S",
            "CONFIG_INCLUDE_LIST",
            Collections.emptyList(),
            new String[]{"# These configs are included, by default configs are not synced."}
        ));

        rules.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_RULES,
            "S",
            "FILE_IGNORE_LIST",
            Collections.emptyList(),
            new String[]{"# These files are ignored by serversync, add your client mods here to stop serversync deleting them."}
        ));

        FriendlyConfigCategory connection = new FriendlyConfigCategory(SyncConfig.CATEGORY_CONNECTION);
        connection.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_CONNECTION,
            "SERVER_IP",
            DEFAULT_ADDRESS,
            new String[]{"# The IP address of the server [default: 127.0.0.1]"}
        ));

        connection.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_CONNECTION,
            "SERVER_PORT",
            DEFAULT_PORT,
            new String[]{"# The port that your server will be serving on [range: 1 ~ 49151, default: 38067]"}
        ));

        FriendlyConfigCategory other = new FriendlyConfigCategory(SyncConfig.CATEGORY_OTHER);
        other.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_OTHER,
            "LOCALE",
            Locale.getDefault().toString(),
            new String[]{"# Your locale string"}
        ));

        config.put(SyncConfig.CATEGORY_GENERAL, general);
        config.put(SyncConfig.CATEGORY_RULES, rules);
        config.put(SyncConfig.CATEGORY_CONNECTION, connection);
        config.put(SyncConfig.CATEGORY_OTHER, other);

        try {
            config.writeConfig(new FriendlyConfigWriter(Files.newBufferedWriter(configPath)));
        } catch (IOException e) {
            Logger.debug("Failed to write client config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createServerConfiguration() {
        SERVER_PORT = DEFAULT_PORT;
        PUSH_CLIENT_MODS = false;
        LAST_UPDATE = "";

        FriendlyConfigCategory general = new FriendlyConfigCategory(SyncConfig.CATEGORY_GENERAL);
        general.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_GENERAL,
            "PUSH_CLIENT_MODS",
            false,
            new String[]{"# set true to push client side mods from clientmods directory, set on server [default: false]"}
        ));

        FriendlyConfigCategory rules = new FriendlyConfigCategory(SyncConfig.CATEGORY_RULES);
        rules.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_RULES,
            "S",
            "CONFIG_INCLUDE_LIST",
            Collections.emptyList(),
            new String[]{"# These configs are included, by default configs are not synced"}
        ));
        rules.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_RULES,
            "S",
            "DIRECTORY_INCLUDE_LIST",
            Collections.singletonList("mods"),
            new String[]{"# These directories are included, by default mods and configs are included"}
        ));
        rules.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_RULES,
            "S",
            "FILE_IGNORE_LIST",
            Collections.emptyList(),
            new String[]{"# These files are ignored by serversync, list auto updates with mods added to the clientmods directory"}
        ));

        FriendlyConfigCategory serverConnection = new FriendlyConfigCategory(SyncConfig.CATEGORY_CONNECTION);
        serverConnection.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_CONNECTION,
            "SERVER_PORT",
            DEFAULT_PORT,
            new String[]{"# The port that your server will be serving on [range: 1 ~ 49151, default: 38067]"}
        ));

        FriendlyConfigCategory other = new FriendlyConfigCategory(SyncConfig.CATEGORY_OTHER);
        other.add(new FriendlyConfigElement(
            SyncConfig.CATEGORY_OTHER,
            "LOCALE",
            Locale.getDefault().toString(),
            new String[]{"# Your locale string"}
        ));

        config.put(SyncConfig.CATEGORY_GENERAL, general);
        config.put(SyncConfig.CATEGORY_RULES, rules);
        config.put(SyncConfig.CATEGORY_CONNECTION, serverConnection);
        config.put(SyncConfig.CATEGORY_OTHER, other);

        try {
            config.writeConfig(new FriendlyConfigWriter(Files.newBufferedWriter(configPath)));
        } catch (IOException e) {
            Logger.debug("Failed to write server config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void init() {
        try {
            LOCALE = new Locale(config.getEntryByName("LOCALE").getString());

            try {
                FILE_IGNORE_LIST.addAll(config.getEntryByName("FILE_IGNORE_LIST").getList());
            } catch (NullPointerException e) {
                // Specific conversion from old config files
                FILE_IGNORE_LIST.addAll(config.getEntryByName("MOD_IGNORE_LIST").getList());
            }

            CONFIG_INCLUDE_LIST = config.getEntryByName("CONFIG_INCLUDE_LIST").getList();

            if (configType == EConfigType.SERVER) {
                PUSH_CLIENT_MODS = config.getEntryByName("PUSH_CLIENT_MODS").getBoolean();
                DIRECTORY_INCLUDE_LIST = config.getEntryByName("DIRECTORY_INCLUDE_LIST").getList();
                SERVER_PORT = config.getEntryByName("SERVER_PORT").getInt();
            } else if (configType == EConfigType.CLIENT) {
                SERVER_IP = config.getEntryByName("SERVER_IP").getString();
                SERVER_PORT = config.getEntryByName("SERVER_PORT").getInt();
                REFUSE_CLIENT_MODS = config.getEntryByName("REFUSE_CLIENT_MODS").getBoolean();
            }
        } catch (NullPointerException e) {
            Logger.debug("could not retrieve an entry from the config file, have you altered the entry names?");
        }

        Logger.debug("finished loading config");
    }
}
