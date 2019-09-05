package com.superzanti.serversync.config;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Path;

public abstract class Config {
    protected static final Gson GSON = new Gson();

    public abstract Path getConfigPath();

    public abstract void read() throws IOException;

    public abstract void write() throws IOException;
}
