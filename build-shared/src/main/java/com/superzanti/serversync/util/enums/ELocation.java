package com.superzanti.serversync.util.enums;

public enum ELocation {
    ROOT(""),
    CONFIG("config/serversync"),
    BANNED_IPS("banned-ips.json");

    private final String value;

    ELocation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
