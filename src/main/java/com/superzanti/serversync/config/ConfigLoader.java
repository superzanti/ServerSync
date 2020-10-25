package com.superzanti.serversync.config;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.util.enums.EConfigType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigLoader {
    public static final Path v2ServerConfig = Paths.get(ServerSync.rootDir.toString(),"serversync-server.json");
    public static final Path v2ClientConfig = Paths.get(ServerSync.rootDir.toString(),"serversync-client.json");

    private static final Path v1ServerConfig = Paths.get(ServerSync.rootDir.toString(),"config", "serversync", "serversync-server.cfg");
    private static final Path v1ClientConfig = Paths.get(ServerSync.rootDir.toString(),"config", "serversync", "serversync-client.cfg");

    public static void load(EConfigType type) throws IOException {
        if (EConfigType.SERVER.equals(type)) {
            if (Files.exists(v2ServerConfig)) {
                JsonConfig.forServer(v2ServerConfig);
                return;
            }
            if (Files.exists(v1ServerConfig)) {
                OldConfig.forServer(v1ServerConfig);
                return;
            }
            createConfig(type);
            JsonConfig.forServer(v2ServerConfig);
            return;
        }

        if (EConfigType.CLIENT.equals(type)) {
            if (Files.exists(v2ClientConfig)) {
                JsonConfig.forClient(v2ClientConfig);
                return;
            }
            if (Files.exists(v1ClientConfig)) {
                OldConfig.forClient(v1ClientConfig);
                return;
            }
            createConfig(type);
            JsonConfig.forClient(v2ClientConfig);
            return;
        }
        throw new IOException(String.format("Unhandled config type given: %s", type));
    }

    private static void createConfig(EConfigType type) throws IOException {
        if (EConfigType.SERVER.equals(type)) {
            Files.createDirectories(v2ServerConfig.getParent());
            Files.createFile(v2ServerConfig);
            JsonConfig.saveServer(v2ServerConfig);
        }

        if (EConfigType.CLIENT.equals(type)) {
            Files.createDirectories(v2ClientConfig.getParent());
            Files.createFile(v2ClientConfig);
            JsonConfig.saveClient(v2ClientConfig);
        }
    }
}
