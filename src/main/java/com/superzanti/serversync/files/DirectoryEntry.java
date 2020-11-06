package com.superzanti.serversync.files;

import java.io.Serializable;

public class DirectoryEntry implements Serializable {
    public final String path;
    public final EDirectoryMode mode;

    public DirectoryEntry(String path, EDirectoryMode mode) {
        this.path = path;
        this.mode = mode;
    }

    public String toJson() {
        return String.format("{\"path\":\"%s\",\"mode\":\"%s\"}", path, mode);
    }
}
