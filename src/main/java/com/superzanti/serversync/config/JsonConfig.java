package com.superzanti.serversync.config;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.superzanti.serversync.SyncConfig;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;

public class JsonConfig {
    public static SyncConfig forServer(Path json) throws IOException {
        try (Reader reader = Files.newBufferedReader(json)) {
            SyncConfig config = new SyncConfig();
            JsonObject root = Json.parse(reader).asObject();
            if (root.isNull()) {
                throw new IOException("Invalid configuration file");
            }

            JsonObject general = getCategory(root, "general");
            JsonObject connection = getCategory(root, "connection");
            JsonObject rules = getCategory(root, "rules");
            JsonObject misc = getCategory(root, "misc");

            config.PUSH_CLIENT_MODS = getBoolean(general, "push_client_mods");
            config.SYNC_MODE = getInt(general, "sync_mode");
            config.SERVER_PORT = getInt(connection, "port");

            JsonArray directoryIncludeList = getArray(rules, "directory_include_list");
            config.DIRECTORY_INCLUDE_LIST = directoryIncludeList
                .values()
                .stream()
                .map(v -> {
                    if (v.isObject()) {
                        // TODO ditching mode for now as we are not using it
                        return v.asObject().get("name").asString();
                    }
                    return v.asString();
                })
                .collect(Collectors.toList());

            JsonArray fileIgnoreList = getArray(rules, "file_ignore_list");
            config.FILE_IGNORE_LIST = fileIgnoreList
                .values()
                .stream()
                .map(v -> {
                    if (v.isObject()) {
                        // Ditching description as we don't use it for anything
                        return v.asObject().get("pattern").asString();
                    }
                    return v.asString();
                })
                .collect(Collectors.toList());

            String[] localeParts = getString(misc, "locale").split("_");
            config.LOCALE = new Locale(localeParts[0], localeParts[1]);

            return config;
        }
    }

    private static JsonObject getCategory(JsonObject root, String name) throws IOException {
        JsonObject jso = root.get(name).asObject();
        if (jso.isNull()) {
            throw new IOException(String.format("No %s category present in configuration file", name));
        }
        return jso;
    }

    private static String getString(JsonObject root, String name) throws IOException {
        JsonValue jsv = root.get(name);
        if (jsv.isNull()) {
            throw new IOException(String.format("No %s value present in configuration file", name));
        }
        if (!jsv.isString()) {
            throw new IOException(String.format("Invalid value for %s, expected string", name));
        }
        return jsv.asString();
    }

    private static boolean getBoolean(JsonObject root, String name) throws IOException {
        JsonValue jsv = root.get(name);
        if (jsv.isNull()) {
            throw new IOException(String.format("No %s value present in configuration file", name));
        }
        if (!jsv.isBoolean()) {
            throw new IOException(String.format("Invalid value for %s, expected boolean", name));
        }
        return jsv.asBoolean();
    }

    private static int getInt(JsonObject root, String name) throws IOException {
        JsonValue jsv = root.get(name);
        if (jsv.isNull()) {
            throw new IOException(String.format("No %s value present in configuration file", name));
        }
        if (!jsv.isNumber()) {
            throw new IOException(String.format("Invalid value for %s, expected integer", name));
        }
        return jsv.asInt();
    }

    private static JsonArray getArray(JsonObject root, String name) throws IOException {
        JsonValue jsv = root.get(name);
        if (jsv.isNull()) {
            throw new IOException(String.format("No %s value present in configuration file", name));
        }
        if (!jsv.isArray()) {
            throw new IOException(String.format("Invalid value for %s, expected array", name));
        }
        return jsv.asArray();
    }
}
