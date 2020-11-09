package com.superzanti.serversync.client;

import com.superzanti.serversync.GUIJavaFX.Gui_JavaFX;
import com.superzanti.serversync.config.Mod;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.files.ManifestEntry;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EValid;
import javafx.application.Platform;

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

        Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().clear();
        Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(0);
        double n = manifest.entries.size();
        double count = 0;
        for(ManifestEntry entry : manifest.entries){
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

            Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().add(new Mod(entry.path, EValid.UPTODATE,false));

            count++;
            Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(count/n);
            Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setText(entry.path);
            Platform.runLater(() -> Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().update());
        };
    }
}
