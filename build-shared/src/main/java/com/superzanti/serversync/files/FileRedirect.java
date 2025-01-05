package com.superzanti.serversync.files;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class FileRedirect {
    public final String pattern;
    public final String redirectTo;

    public FileRedirect() {
        this.pattern = "";
        this.redirectTo = "";
    }

    public FileRedirect(String pattern, String redirectTo) {
        this.pattern = pattern;
        this.redirectTo = redirectTo;
    }

    public static FileRedirect from(JsonObject o) {
        return new FileRedirect(o.get("pattern").asString(), o.get("redirectTo").asString());
    }

    public JsonObject toJson() {
        return Json.object().add("pattern", pattern).add("redirectTo", redirectTo);
    }
}
