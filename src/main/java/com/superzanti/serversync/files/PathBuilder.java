package com.superzanti.serversync.files;

import com.superzanti.serversync.ServerSync;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathBuilder {
    private final StringBuilder builder = new StringBuilder();

    public PathBuilder() {
        add(ServerSync.rootDir.toString());
    }

    /**
     * Adds (appends) a segment to path
     *
     * @param segment segment to add
     * @return The builder for further actions
     */
    public PathBuilder add(String segment) {
        if (builder.length() > 0) {
            builder.append(FileSystems.getDefault().getSeparator());
        }
        builder.append(segment);
        return this;
    }

    public PathBuilder add(Path segment) {
        return add(segment.toString());
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public Path toPath() {
        return Paths.get(builder.toString());
    }
}
