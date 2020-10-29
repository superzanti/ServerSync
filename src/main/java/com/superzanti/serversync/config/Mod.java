package com.superzanti.serversync.config;

import com.superzanti.serversync.util.enums.Valid;

public class Mod{

    private String name;
    private Valid validValue;
    private boolean ignoreValue = false;

    public Mod(String name) {
        this.name = name;
        this.validValue = Valid.DEFAULT;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Valid getValidValue() {
        return validValue;
    }

    public void setValidValue(Valid validValue) {
        this.validValue = validValue;
    }

    public boolean isIgnoreValue() {
        return ignoreValue;
    }

    public void setIgnoreValue(boolean ignoreValue) {
        this.ignoreValue = ignoreValue;
    }
}
