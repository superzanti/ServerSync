package com.superzanti.serversync.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BannedIPSReader {
    public static List<String> read(String data) {
        try {
            return fromJson(Json.parse(data).asArray());
        } catch (ParseException e) {
            Logger.debug("Found invalid JSON in string");
        }
        return Collections.emptyList();
    }

    public static List<String> read(Path file) throws IOException {
        try {
            return fromJson(Json.parse(Files.newBufferedReader(file)).asArray());
        } catch (ParseException e) {
            Logger.debug("Found invalid JSON in banned-ips.json");
        }
        return Collections.emptyList();
    }

    private static List<String> fromJson(JsonArray a) {
        if (a.size() == 0) {
            return Collections.emptyList();
        }
        return a.values().parallelStream().map(j -> j.asObject().get("ip").asString()).collect(Collectors.toList());
    }
}
