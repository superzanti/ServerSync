package com.superzanti.serversync.files;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.File;
import java.io.Serializable;

public class DirectoryEntry implements Serializable {
    public final String path;
    public final EDirectoryMode mode;

    public DirectoryEntry(String path, EDirectoryMode mode) {
        this.path = path;
        this.mode = mode;
    }

    public String getLocalPath() {
        return path.replace("/", File.separator).replace("\\", File.separator);
    }

    public JsonObject toJson() {
        return Json.object().add("path", path).add("mode", mode.toString());
    }

    @Override
    public String toString() {
        return "DirectoryEntry{" +
            "path='" + path + '\'' +
            ", mode=" + mode +
            '}';
    }
}
