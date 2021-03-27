package com.superzanti.serversync.files;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileEntry implements Serializable {
    /**
     * Where the file is on the server.
     */
    public final String path;

    /**
     * The comparable description of this file.
     */
    public final String hash;

    /**
     * Where the file should go on the client.
     * <p>
     * Defaults to the same location as the server if not specified.
     */
    public final String redirectTo;

    public final boolean isOptional;

    public FileEntry(String path, String hash) {
        this.path = path;
        this.hash = hash;
        this.redirectTo = "";
        this.isOptional = false;
    }

    public FileEntry(String path, String hash, String mapping) {
        this.path = path;
        this.hash = hash;
        this.redirectTo = mapping;
        this.isOptional = false;
    }

    public FileEntry(String path, String hash, String mapping, boolean isOptional) {
        this.path = path;
        this.hash = hash;
        this.redirectTo = mapping;
        this.isOptional = isOptional;
    }

    public Path resolvePath() {
        if ("".equals(redirectTo)) {
            return new PathBuilder().add(path).toPath();
        }
        return new PathBuilder().add(redirectTo).add(Paths.get(path).getFileName()).toPath();
    }

    @Override
    public String toString() {
        return "FileEntry{" +
            "path='" + path + '\'' +
            ", hash='" + hash + '\'' +
            ", redirectTo='" + redirectTo + '\'' +
            ", isOptional='" + isOptional + '\'' +
            '}';
    }
}

