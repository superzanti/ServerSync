package com.superzanti.serversync.config;

import com.superzanti.serversync.util.enums.EValid;

public class Mod{

    private String name;
    private EValid status;
    private boolean ignoreValue = false;

    public Mod(String name) {
        this.name = name;
        this.status = EValid.INVALID;
    }

    public Mod(String name, EValid valid, Boolean b){
        this.name=name;
        this.status = valid;
        this.ignoreValue = b;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status.toString();
    }
    public void setStatus(EValid status) {
        this.status = status;
    }

    public boolean isIgnoreValue() {
        return ignoreValue;
    }

    public void setIgnoreValue(boolean ignoreValue) {
        this.ignoreValue = ignoreValue;
    }
}
