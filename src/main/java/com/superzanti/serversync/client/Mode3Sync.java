package com.superzanti.serversync.client;

import com.superzanti.serversync.GUIJavaFX.Gui_JavaFX;
import com.superzanti.serversync.config.Mod;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.Valid;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
       // System.out.println(manifest.entries.toString());
        //System.out.println("zeub");

        ObservableList<Mod> observMods = FXCollections.observableArrayList();
        //Mod e = new Mod("BLA");
        //observMods.add(e);

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

                    Mod mod = new Mod(entry.path);

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

                    observMods.add(mod);

                });

        System.out.println(observMods.toString());

        Gui_JavaFX.getStackMainPane().getPaneSync().setObservMods(observMods);

        SyncConfig.getConfig().SYNC_MODE = 2;
    }
}
