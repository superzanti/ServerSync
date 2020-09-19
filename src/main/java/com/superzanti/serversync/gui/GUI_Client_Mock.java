package com.superzanti.serversync.gui;

import com.superzanti.serversync.config.SyncConfig;

public class GUI_Client_Mock extends GUI_Client {
    private static final long serialVersionUID = 1L;

    public GUI_Client_Mock() {
    }

    @Override
    public void updateText(String text) {
    }

    @Override
    public void updateProgress(int progress) {
    }

    @Override
    public void updateFileProgress(String message, int progress) {
    }

    @Override
    public void enableSyncButton() {
    }

    @Override
    public void disableSyncButton() {
    }

    @Override
    public String getIPAddress() {
        return SyncConfig.getConfig().SERVER_IP;
    }

    @Override
    public void setIPAddress(String ip) {
    }

    @Override
    public int getPort() {
        return SyncConfig.getConfig().SERVER_PORT;
    }

    @Override
    public boolean setPort(int port) {
        return true;
    }
}
