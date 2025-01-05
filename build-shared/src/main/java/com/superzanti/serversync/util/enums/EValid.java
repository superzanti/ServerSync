package com.superzanti.serversync.util.enums;

public enum EValid {


    UPTODATE("Up to date"),
    OUTDATED("Outdated"),
    INVALID("Missing");

    private final String state;

    EValid(String state) {
        this.state = state;
    }

    public String toString() {
        return state;
    }
}
