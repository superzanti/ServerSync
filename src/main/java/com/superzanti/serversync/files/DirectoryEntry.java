package com.superzanti.serversync.files;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.Serializable;

public class DirectoryEntry implements Serializable {
    public final String path;
    public final EDirectoryMode mode;

    public DirectoryEntry(String path, EDirectoryMode mode) {
        this.path = path;
        this.mode = mode;
    }

    public JsonObject toJson() {
        return Json.object().add("path", path).add("mode", mode.toString());
    }
}
