package com.superzanti.serversync.util.enums;

public enum ELocations {
    ROOT(""),
    CONFIG("config/serversync"),
    BANNED_IPS("banned-ips.json");

    private final String value;

    ELocations(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
