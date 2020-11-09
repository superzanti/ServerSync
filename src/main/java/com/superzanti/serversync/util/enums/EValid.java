package com.superzanti.serversync.util.enums;

public enum EValid {


    UPTODATE("Uptodate"),
    OUTDATED("Outdated"),
    INVALID("Invalid");

    private final String state;

    EValid(String state) {
        this.state = state;
    }

    public String toString() {
        return state;
    }
}
