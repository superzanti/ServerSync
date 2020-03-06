package com.superzanti.serversync.util.enums;

public enum EFriendlyConfigEntries {
    SERVER_IP("SERVER_IP"),
    SERVER_PORT("SERVER_PORT"),
    PUSH_CLIENT_MODS("PUSH_CLIENT_MODS"),
    REFUSE_CLIENT_MODS("REFUSE_CLIENT_MODS"),
    FILE_INCLUDE_LIST("FILE_INCLUDE_LIST"),
    FILE_IGNORE_LIST("FILE_IGNORE_LIST"),
    LOCALE("LOCALE");


    private String value;

    EFriendlyConfigEntries(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
