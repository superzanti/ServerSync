package com.superzanti.serversync.config;

import com.superzanti.serversync.util.PathBuilder;
import com.superzanti.serversync.util.enums.EFriendlyConfigEntries;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfig;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfigReader;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfigWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientConfig extends Config {
    private static final Path PATH = new PathBuilder(".").add("serversync-client.cfg").buildPath();

    private final FriendlyConfig config;

    ClientConfig() throws IOException {
        if (Files.exists(getConfigPath())) {
            config = new FriendlyConfig();
            read();
        } else {
            config = new ClientConfigDefault().getConfig();
            write();
        }
    }

    @Override
    public Path getConfigPath() {
        return ClientConfig.PATH;
    }

    public String getServerIp() {
        return config.getEntryByName(EFriendlyConfigEntries.SERVER_IP.getValue()).getString();
    }

    public int getServerPort() {
        return config.getEntryByName(EFriendlyConfigEntries.SERVER_PORT.getValue()).getInt();
    }

    public boolean isRefusingClientOnlyFilesFromServer() {
        return config.getEntryByName(EFriendlyConfigEntries.REFUSE_CLIENT_MODS.getValue()).getBoolean();
    }

    @Override
    public void read() throws IOException {
        try (FriendlyConfigReader read = new FriendlyConfigReader(Files.newBufferedReader(getConfigPath()))) {
            config.readConfig(read);
        }
    }

    @Override
    public void write() throws IOException {
        if (Files.notExists(getConfigPath())) {
            Files.createDirectories(getConfigPath());
            Files.createFile(getConfigPath());
        }

        try (FriendlyConfigWriter write = new FriendlyConfigWriter(Files.newBufferedWriter(getConfigPath()))) {
            config.writeConfig(write);
        }
    }
}
