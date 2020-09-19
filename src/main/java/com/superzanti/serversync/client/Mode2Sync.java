package com.superzanti.serversync.client;

import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.util.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class Mode2Sync implements Runnable {
    private final ManifestServer server;

    private Mode2Sync(ManifestServer server) {
        this.server = server;
    }

    public static Mode2Sync forServer(Server server) {
        return new Mode2Sync(new ManifestServer(server));
    }

    @Override
    public void run() {
        FileManifest manifest = server.fetchManifest();

        manifest.entries
            .forEach(entry -> {
                Path file = entry.resolvePath();
                Logger.debug(String.format("Starting check for file: %s", file));
                if (!entry.redirectTo.equals("")) {
                    Logger.debug(String.format(
                        "File: %s, redirected from: %s to %s",
                        file.getFileName(),
                        entry.path,
                        file
                    ));
                }

                if (Files.exists(file)) {
                    String hash = FileHash.hashFile(file);

                    if (entry.hash.equals(hash)) {
                        Logger.debug("File already exists");
                        return;
                    }
                }
                server.updateIndividualFile(entry, file);
            });
    }
}
