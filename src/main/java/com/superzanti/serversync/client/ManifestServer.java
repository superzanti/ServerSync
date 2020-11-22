package com.superzanti.serversync.client;

import com.superzanti.serversync.communication.Requests;
import com.superzanti.serversync.files.FileManifest;

import java.util.function.Consumer;

public class ManifestServer {
    private final Requests requests;

    public ManifestServer(Server server) {
        this.requests = Requests.forServer(server);
    }

    public FileManifest fetchManifest() {
        return requests.getManifest();
    }

    public boolean updateIndividualFile(ActionEntry entry, Consumer<ActionProgress> progressConsumer) {
        return requests.updateFile(entry, progressConsumer);
    }
}
