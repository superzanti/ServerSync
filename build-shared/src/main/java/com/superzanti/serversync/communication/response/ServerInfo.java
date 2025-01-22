package com.superzanti.serversync.communication.response;

import java.io.Serializable;
import java.util.List;

public class ServerInfo implements Serializable {
    public final List<String> messages;
    public final int syncMode;

    public ServerInfo(List<String> messages, int syncMode) {
        this.messages = messages;
        this.syncMode = syncMode;
    }
}
