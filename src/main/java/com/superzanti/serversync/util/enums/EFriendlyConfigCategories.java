package com.superzanti.serversync.util.enums;

public enum EFriendlyConfigCategories {
    GENERAL("general"),
    CONNECTION("connection"),
    RULES("rules"),
    MISC("other");

    private String value;

    EFriendlyConfigCategories(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
