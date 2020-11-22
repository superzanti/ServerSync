package com.superzanti.serversync.client;

public class ActionProgress {
    public double progress;
    public String name;
    public boolean complete;
    public ActionEntry entry;

    public ActionProgress(double progress, String name, boolean complete, ActionEntry entry) {
        this.progress = progress;
        this.name = name;
        this.complete = complete;
        this.entry = entry;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getName() {
        return name;
    }

    public ActionEntry getEntry() {
        return entry;
    }
}
