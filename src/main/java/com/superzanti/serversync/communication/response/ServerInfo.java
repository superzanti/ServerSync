package com.superzanti.serversync.communication.response;

import java.io.Serializable;
import java.util.List;

public class ServerInfo implements Serializable {
    public List<String> messages;
    public int syncMode;

    public ServerInfo(List<String> messages, int syncMode) {
        this.messages = messages;
        this.syncMode = syncMode;
    }
}
