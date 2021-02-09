package com.superzanti.serversync.client;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.files.*;
import com.superzanti.serversync.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Mode2Sync implements Runnable {
    private final ManifestServer server;

    private Mode2Sync(ManifestServer server) {
        this.server = server;
    }

    public static Mode2Sync forServer(Server server) {
        return new Mode2Sync(new ManifestServer(server));
    }

    public FileManifest fetchManifest() {
        return server.fetchManifest();
    }

    public void executeActionList(List<ActionEntry> actions, Consumer<ActionProgress> progressConsumer) throws IOException {
        for (ActionEntry action : actions) {
            switch (action.action) {
                case Update:
                    Logger.log(String.format("%sUpdating file %s", RefStrings.UPDATE_TOKEN, action));
                    server.updateIndividualFile(action, progressConsumer);
                    break;
                case Delete:
                    Logger.log(String.format("%sDeleting file %s", RefStrings.DELETE_TOKEN, action));
                    Files.delete(action.target.resolvePath());
                    break;

            }
        }
    }

    public List<ActionEntry> generateActionList(FileManifest manifest) throws IOException {
        List<ActionEntry> actions = manifest.files.stream().map(entry -> {
            Path file = entry.resolvePath();

            if (IgnoredFilesMatcher.matches(file)) {
                return new ActionEntry(entry, EActionType.Ignore, "ui/reason_matched_user_ignore_pattern");
            }

            if (Files.exists(file)) {
                String hash = FileHash.hashFile(file);

                if (entry.hash.equals(hash)) {
                    return new ActionEntry(entry, EActionType.None, "ui/reason_up_to_date");
                }
                return new ActionEntry(entry, EActionType.Update, "ui/reason_does_not_match_server");
            }
            return new ActionEntry(entry, EActionType.Update, "ui/reason_does_not_exist");
        }).collect(Collectors.toList());

        List<Path> files = manifest.files.stream().map(FileEntry::resolvePath).collect(Collectors.toList());
        for (DirectoryEntry dir : manifest.directories) {
            if (dir.mode == EDirectoryMode.mirror) {
                Path dirPath = ServerSync.rootDir.resolve(dir.path);
                if (Files.notExists(dirPath)) {
                    // Can happen if a directory is configured to be managed but all of its files are ignored
                    continue;
                }
                List<ActionEntry> dirActions = Files
                    .walk(dirPath)
                    .filter(f -> !Files.isDirectory(f) && !files.contains(f))
                    .map(f -> new FileEntry(ServerSync.rootDir.relativize(f).toString(), null, ""))
                    .map(entry -> {
                        if (IgnoredFilesMatcher.matches(entry.resolvePath())) {
                            return new ActionEntry(
                                entry, EActionType.Ignore,
                                "Matches user ignore pattern"
                            );
                        }
                        return new ActionEntry(
                            entry, EActionType.Delete, "Folder set to mirror mode");
                    }).collect(Collectors.toList());
                actions.addAll(dirActions);
            }
        }
        return actions;
    }

    @Override
    public void run() {
        FileManifest manifest = server.fetchManifest();

// <<<<<<< master
//         Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().clear();
//         Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(0);
//         double n = manifest.entries.size();
//         double count = 0;
//         for(ManifestEntry entry : manifest.entries){
//             Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(count/n);
//             Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setPathText(entry.path);
//             Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setStatusText("Files updated : " +(int)count+"/"+(int)n);
//             Platform.runLater(() -> Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().updateGUI());

//             Path file = entry.resolvePath();
//             Logger.debug(String.format("Starting check for file: %s", file));
//             if (!entry.redirectTo.equals("")) {
//                 Logger.debug(String.format(
// =======
        manifest.files
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

// <<<<<<< master
//                 if (entry.hash.equals(hash)) {
//                     Logger.debug("File already exists");
//                 }else{
//                     server.updateIndividualFile(entry, file);
//                 }
//             }else{
//                 server.updateIndividualFile(entry, file);
//             }

//             Gui_JavaFX.getStackMainPane().getPaneSync().getObservMods().add(new Mod(entry.path, EValid.UPTODATE,false));

//             count++;
//         };
//         Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().getProgressBar().setProgress(count/n);
//         Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setStatusText("Files updated : " +(int)count+"/"+(int)n);
//         Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().setPathText("Done!");
//         Platform.runLater(() -> Gui_JavaFX.getStackMainPane().getPaneSync().getPaneProgressBar().updateGUI());
// =======
                    if (entry.hash.equals(hash)) {
                        Logger.debug("File already exists");
                    }
                }

            });

        // TODO implement delete phase
    }
}
