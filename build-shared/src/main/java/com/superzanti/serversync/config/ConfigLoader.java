package com.superzanti.serversync.config;

import com.superzanti.serversync.ServerSyncUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ConfigLoader {
    private static final Path v2ServerConfig = Paths.get(ServerSyncUtility.rootDir.toString(), "serversync-server.json");
    private static final Path v2ClientConfig = Paths.get(ServerSyncUtility.rootDir.toString(), "serversync-client.json");

    private static final Path v1ServerConfig = Paths
            .get(ServerSyncUtility.rootDir.toString(), "config", "serversync", "serversync-server.cfg");
    private static final Path v1ClientConfig = Paths
            .get(ServerSyncUtility.rootDir.toString(), "config", "serversync", "serversync-client.cfg");

    public static void loadServer() throws IOException {
        if (Files.exists(v2ServerConfig)) {
            JsonConfig.forServer(v2ServerConfig);
            return;
        }
        if (Files.exists(v1ServerConfig)) {
            OldConfig.forServer(v1ServerConfig);
            SyncConfig config = SyncConfig.getConfig();
            config.SYNC_MODE = 2; // mode 2 is the only supported mode
            // Old configurations would include everything in the directory list
            config.FILE_INCLUDE_LIST = config.DIRECTORY_INCLUDE_LIST
                    .stream()
                    .map(di -> di.path + "/**")
                    .collect(Collectors.toList());
            createConfigServer(); // Migrate to new json based config
            return;
        }
        createConfigServer();
        JsonConfig.forServer(v2ServerConfig);

    }

    public static void loadClient() throws IOException {
        if (Files.exists(v2ClientConfig)) {
            JsonConfig.forClient(v2ClientConfig);
            return;
        }
        if (Files.exists(v1ClientConfig)) {
            OldConfig.forClient(v1ClientConfig);
            createConfigClient(); // Migrate to new json based config
            return;
        }
        createConfigClient();
        JsonConfig.forClient(v2ClientConfig);
    }

    public static void saveServer() throws IOException {
            JsonConfig.saveServer(v2ServerConfig);

    }

    public static void saveClient() throws IOException {
            JsonConfig.saveClient(v2ClientConfig);
    }

    private static void createConfigServer() throws IOException {
        Files.createDirectories(v2ServerConfig.getParent());
        Files.createFile(v2ServerConfig);
        JsonConfig.saveServer(v2ServerConfig);

    }

    private static void createConfigClient() throws IOException {
        Files.createDirectories(v2ClientConfig.getParent());
        Files.createFile(v2ClientConfig);
        JsonConfig.saveClient(v2ClientConfig);

    }
}
