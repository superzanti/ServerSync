package com.superzanti.serversync.client;

import com.superzanti.serversync.files.FileEntry;

public class ActionEntry {
    public final FileEntry target;
    public EActionType action;
    public String reason;

    public ActionEntry(FileEntry target, EActionType action, String reason) {
        this.target = target;
        this.action = action;
        this.reason = reason;
    }

    public String getName() {
        return target.path;
    }

    public FileEntry getTarget() {
        return target;
    }

    public EActionType getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("ActionEntry{path='%s',reason='%s'", target.resolvePath(), reason);
    }
}
