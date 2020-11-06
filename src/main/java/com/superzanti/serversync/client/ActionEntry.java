package com.superzanti.serversync.client;

import com.superzanti.serversync.files.FileEntry;

public class ActionEntry {
    public FileEntry target;
    public EActionType action;
    public String reason;

    public ActionEntry(FileEntry target, EActionType action, String reason) {
        this.target = target;
        this.action = action;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return String.format("ActionEntry{path='%s',reason='%s'", target.resolvePath(), reason);
    }
}
