package com.superzanti.serversync.client;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.files.DirectoryEntry;
import com.superzanti.serversync.files.EDirectoryMode;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.files.FileHash;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.FileEntry;
import com.superzanti.serversync.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    public void executeActionList(List<ActionEntry> actions) throws IOException {
        for (ActionEntry action : actions) {
            switch (action.action) {
                case Update:
                    Logger.log(String.format("%sUpdating file %s", RefStrings.UPDATE_TOKEN, action));
                    server.updateIndividualFile(action.target, action.target.resolvePath());
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
                return new ActionEntry(entry, EActionType.Ignore, "Matched user ignore pattern");
            }

            if (Files.exists(file)) {
                String hash = FileHash.hashFile(file);

                if (entry.hash.equals(hash)) {
                    return new ActionEntry(entry, EActionType.None, "File up to date");
                }
                return new ActionEntry(entry, EActionType.Update, "File does not match server");
            }
            return new ActionEntry(entry, EActionType.Update, "File does not exist");
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

                    if (entry.hash.equals(hash)) {
                        Logger.debug("File already exists");
                    }
                }

            });

        // TODO implement delete phase
    }
}
