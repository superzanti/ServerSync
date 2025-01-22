package com.superzanti.serversync.files;

public enum EDirectoryMode {
    // A bit magical to be using string values that match the enum name
    mirror("mirror"),
    push("push");

    private final String value;
    EDirectoryMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
