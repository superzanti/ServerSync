package com.superzanti.serversync.config;

import com.superzanti.serversync.util.PathBuilder;

import java.io.IOException;
import java.nio.file.Path;

public class ServerConfig extends Config {
    private static final Path PATH = new PathBuilder(".").add("serversync-server.cfg").buildPath();

    @Override
    public Path getConfigPath() {
        return ServerConfig.PATH;
    }

    @Override
    public void read() throws IOException {

    }

    @Override
    public void write() throws IOException {

    }
}
