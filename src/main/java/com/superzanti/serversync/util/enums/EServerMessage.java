package com.superzanti.serversync.util.enums;

public enum EServerMessage {
    SYNC_FILES("SYNC_FILES"),
    GET_SERVER_INFO("GET_SERVER_INFO"),
    GET_MANAGED_DIRECTORIES("GET_MANAGED_DIRECTORIES"),
    GET_NUMBER_OF_MANAGED_FILES("GET_NUMBER_OF_MANAGED_FILES"),
    GET_MANIFEST("GET_MANIFEST"),
    UPDATE_FILE("UPDATE_FILE"),
    EXIT("EXIT");

    String name;

    EServerMessage(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%d.EServerMessage.%s", ordinal(), name);
    }
}
