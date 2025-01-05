package com.superzanti.serversync.util.enums;

public enum EBinaryAnswer {
    NO(0),
    YES(1);

    private final int value;
    EBinaryAnswer(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
