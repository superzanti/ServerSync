package com.superzanti.serversync.config;

import com.superzanti.serversync.util.enums.EValid;

public class Mod{

    private String name;
    private EValid validValue;
    private boolean ignoreValue = false;

    public Mod(String name) {
        this.name = name;
        this.validValue = EValid.INVALID;
    }

    public Mod(String name, EValid valid, Boolean b){
        this.name=name;
        this.validValue = valid;
        this.ignoreValue = b;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValidValue() {
        return validValue.toString();
    }
    public void setValidValue(EValid validValue) {
        this.validValue = validValue;
    }

    public boolean isIgnoreValue() {
        return ignoreValue;
    }

    public void setIgnoreValue(boolean ignoreValue) {
        this.ignoreValue = ignoreValue;
    }
}
