package com.superzanti.serversync.client;

import com.superzanti.serversync.communication.Requests;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.FileEntry;

import java.nio.file.Path;

public class ManifestServer {
    private final Requests requests;

    public ManifestServer(Server server) {
        this.requests = Requests.forServer(server);
    }

    public FileManifest fetchManifest() {
        return requests.getManifest();
    }

    public boolean updateIndividualFile(FileEntry entry, Path theLocalFile) {
        return requests.updateFile(entry, theLocalFile);
    }
}
