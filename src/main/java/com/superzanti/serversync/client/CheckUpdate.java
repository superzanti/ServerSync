package com.superzanti.serversync.client;

import com.superzanti.serversync.GUIJavaFX.Gui_JavaFX;
import com.superzanti.serversync.config.Mod;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.ManifestEntry;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EValid;
import javafx.application.Platform;
import java.nio.file.Files;
import java.nio.file.Path;

public class CheckUpdate implements  Runnable{

    private final ManifestServer server;

    private CheckUpdate(ManifestServer server) {
        this.server = server;
    }

    public static CheckUpdate forServer(Server server) {
        return new CheckUpdate(new ManifestServer(server));
    }

    @Override
    public void run() {
        FileManifest manifest = server.fetchManifest();

        Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().clear();
        Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(0);
        double n = manifest.entries.size();
        double count = 0;
        Mod mod;
        for(ManifestEntry entry : manifest.entries){
            Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(count/n);
            Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setPathText(entry.path);
            Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setStatusText("Files checked : " +(int)count+"/"+(int)n);
            Platform.runLater(() -> Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().updateGUI());

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

                    mod.setValidValue(EValid.UPTODATE);
                }else{

                    mod.setValidValue(EValid.OUTDATED);
                }
            }else{
                mod.setValidValue(EValid.INVALID);
            }

            Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().add(mod);

            count++;
        }
        Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(count/n);
        Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setStatusText("Files checked : " +(int)count+"/"+(int)n);
        Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setPathText("Done!");
        Platform.runLater(() -> Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().updateGUI());
        SyncConfig.getConfig().SYNC_MODE = 2;
    }
}
