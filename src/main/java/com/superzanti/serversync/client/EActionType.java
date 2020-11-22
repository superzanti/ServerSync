package com.superzanti.serversync.client;

public enum EActionType {
    None("none"),
    Update("update"),
    Delete("delete"),
    Ignore("ignore");

    private final String type;

    EActionType(String type) {
        this.type = type;
    }

    public String toString() {
        return type;
    }
}
