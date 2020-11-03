package com.superzanti.serversync.client;

import com.superzanti.serversync.GUIJavaFX.Gui_JavaFX;
import com.superzanti.serversync.config.Mod;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.ManifestEntry;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.Valid;
import javafx.application.Platform;
import java.nio.file.Files;
import java.nio.file.Path;

public class Mode3Sync implements  Runnable{

    private final ManifestServer server;

    private Mode3Sync(ManifestServer server) {
        this.server = server;
    }

    public static Mode3Sync forServer(Server server) {
        return new Mode3Sync(new ManifestServer(server));
    }

    @Override
    public void run() {
        FileManifest manifest = server.fetchManifest();

        Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().clear();
        Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(0);
        double n = manifest.entries.size();
        double count = 0;
        Mod mod = null;
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

           mod = new Mod(entry.path);

            if(Files.exists(file)){

                String hash = FileHash.hashFile(file);

                if (entry.hash.equals(hash)) {

                    mod.setValidValue(Valid.UPTODATE);
                }else{

                    mod.setValidValue(Valid.OUTDATED);
                }
            }else{
                mod.setValidValue(Valid.INVALID);
            }

            Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().add(mod);

            count++;
            Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(count/n);
            Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setText(entry.path);
            Platform.runLater(() -> Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().update());
        }

        SyncConfig.getConfig().SYNC_MODE = 2;
    }
}
