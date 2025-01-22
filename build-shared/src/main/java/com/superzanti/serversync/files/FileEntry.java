package com.superzanti.serversync.files;

import java.io.File;
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
        String localPath=path.replace("/", File.separator).replace("\\", File.separator);
        String localRedirect=redirectTo.replace("/", File.separator).replace("\\", File.separator);
        if ("".equals(localRedirect)) {
            return new PathBuilder().add(localPath).toPath();
        }
        return new PathBuilder().add(localRedirect).add(Paths.get(localPath).getFileName()).toPath();
    }

    @Override
    public String toString() {
        return String.format("FileEntry{path=%s,hash=%s,redirectTo=%s,isOptional=%s}", path, hash, redirectTo, isOptional);
    }
}

