package com.superzanti.serversync.client;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.server.Server;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EFileProccessingStatus;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The sync process for clients.
 * - Get my state
 * - Stream server state and pop files from my state that are dealt with
 * - Delete files that are not present on the server (remaining)
 * <p>
 * Caveats:
 * - Client can configure to ignore files from deletion (e.g. Optifine, NEET and other such client side mods)
 * <p>
 *
 * @author Rheimus
 */
public class ClientWorker implements Runnable {

    private boolean errorInUpdates = false;
    private boolean updateHappened = false;

    private Server server;
    private List<String> managedDirectories = new ArrayList<>(0);

    private SyncConfig config = SyncConfig.getConfig();

    @Override
    public void run() {
        updateHappened = false;

        ServerSync.clientGUI.disableSyncButton();
        Logger.getLog().clearUserFacingLog();

        server = new Server(config.SERVER_IP, config.SERVER_PORT);

        if (!server.connect()) {
            errorInUpdates = true;
            closeWorker();
            return;
        }

        managedDirectories = getServerManagedDirectories();

        Logger.log(String.format("Building file list for directories: %s", managedDirectories));
        // Create dirs on the client that don't exist yet
        managedDirectories.forEach(path -> {
            try {
                Files.createDirectories(Paths.get(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Attempt to sync files with max number of retries, note that some files may fail here
        // the user should be notified that they may have to manually deal with them
        // TODO make retries user configurable?
        int maxUpdateRetries = 2;
        boolean updateSuccess = false;
        Map<String, EFileProccessingStatus> updatedFiles = new HashMap<>();
        for (int i = 0; i < maxUpdateRetries; i++) {
            updatedFiles = updateFiles();

            if (updatedFiles.containsValue(EFileProccessingStatus.FAILED)) {
                Logger.log(String.format(
                    "%s %s",
                    RefStrings.ERROR_TOKEN,
                    ServerSync.strings.getString("message_file_failed_to_sync")
                ));

                if (i < maxUpdateRetries - 1) {
                    Logger.log(ServerSync.strings.getString("message_attempting_sync_retry"));
                }
                continue;
            }

            updateSuccess = true;
            break;
        }

        // Move on to delete phase, note that some files may fail here
        // the user should be notified that they may have to manually deal with them
        // TODO make retries user configurable?
        int maxDeleteRetries = 2;
        boolean deleteSuccess = false;
        Map<String, EFileProccessingStatus> deletedFiles = new HashMap<>();

        for (int i = 0; i < maxDeleteRetries; i++) {
            deletedFiles = deleteFiles(managedDirectories, updatedFiles);

            if (deletedFiles.containsValue(EFileProccessingStatus.FAILED)) {
                Logger.log(String.format(
                    "%s %s",
                    RefStrings.ERROR_TOKEN,
                    ServerSync.strings.getString("message_file_failed_to_delete")
                ));

                if (i < maxDeleteRetries - 1) {
                    Logger.log(ServerSync.strings.getString("message_attempting_delete_retry"));
                }
                continue;
            }

            deleteSuccess = true;
            break;
        }

        // Cleanup phase, things like empty directories or duplicate files should be handled here.
        FileManager.removeEmptyDirectories(
            managedDirectories.stream().map(Paths::get).collect(Collectors.toList()),
            (dir) -> Logger.log(String.format(
                "%s Removed empty directory: %s",
                RefStrings.CLEANUP_TOKEN,
                dir.toString()
            ))
        );

        // Catch update or delete errors and notify the user that they may have to manually intervene.
        if (!updateSuccess) {
            Logger.debug("Update failure, max retries exceeded");
            List<String> fileNames = updatedFiles.entrySet()
                                                 .parallelStream()
                                                 .filter(e -> e.getValue().equals(EFileProccessingStatus.FAILED))
                                                 .map(Map.Entry::getKey)
                                                 .collect(Collectors.toList());
            Logger.log(String.format(
                "%s %s",
                RefStrings.ERROR_TOKEN,
                ServerSync.strings.getString("message_file_failed_to_sync")
            ));
            Logger.log(String.format(
                "%s %s",
                RefStrings.ERROR_TOKEN,
                ServerSync.strings.getString("message_manual_action_required")
            ));
            Logger.log(fileNames.toString());
        }

        if (!deleteSuccess) {
            Logger.debug("Delete failure, max retries exceeded");
            List<String> fileNames = deletedFiles.entrySet()
                                                 .parallelStream()
                                                 .filter(e -> e.getValue().equals(EFileProccessingStatus.FAILED))
                                                 .map(Map.Entry::getKey)
                                                 .collect(Collectors.toList());
            Logger.log(String.format(
                "%s %s",
                RefStrings.ERROR_TOKEN,
                ServerSync.strings.getString("message_file_failed_to_delete")
            ));
            Logger.log(String.format(
                "%s %s",
                RefStrings.ERROR_TOKEN,
                ServerSync.strings.getString("message_manual_action_required")
            ));
            Logger.log(fileNames.toString());
        }

        updateHappened = true;
        closeWorker();

        // Update configured server to the latest used address
        // consideration to be had here for client silent sync mode
        config.updateServerDetails(ServerSync.clientGUI.getIPAddress(), ServerSync.clientGUI.getPort());

        Logger.log(ServerSync.strings.getString("update_complete"));
    }

    private void closeWorker() {
        if (server == null) {
            return;
        }

        if (server.close()) {
            Logger.debug("Successfully closed all connections");
        }

        if (!updateHappened && !errorInUpdates) {
            Logger.log(ServerSync.strings.getString("update_not_needed"));
            ServerSync.clientGUI.updateProgress(100);
        } else {
            Logger.debug(ServerSync.strings.getString("update_happened"));
            ServerSync.clientGUI.updateProgress(100);
        }

        if (errorInUpdates) {
            Logger.error(ServerSync.strings.getString("update_error"));
        }

        ServerSync.clientGUI.enableSyncButton();
    }

    private List<String> getServerManagedDirectories() {
        return server.fetchManagedDirectories();
    }

    private Map<String, EFileProccessingStatus> updateFiles() {
        Logger.log("<------> " + ServerSync.strings.getString("update_start") + " <------>");
        Logger.debug(ServerSync.strings.getString("ignoring") + " " + config.FILE_IGNORE_LIST);

        // Progress tracking setup
        AtomicInteger currentProgress = new AtomicInteger();
        double maxProgress = server.fetchNumberOfServerManagedFiles();
        Logger.debug(String.format("Number of server files: %s", maxProgress));
        if (maxProgress == 0) {
            Logger.log("Server has no files to sync?");
            return new HashMap<>(0);
        }
        if (maxProgress == -1) {
            Logger.debug("Failed to get the number of files managed by the server");
        }
        //----

        // Update files if needed, return files that remain after testing against the servers state
        // these will be the files the the client contains but the server does not.
        return server.syncFiles(() -> forEachFile((currentProgress.incrementAndGet() / maxProgress * 100)));
    }

    private synchronized void forEachFile(double progress) {
        ServerSync.clientGUI.updateProgress((int) progress);
    }

    private Map<String, EFileProccessingStatus> deleteFiles(
        List<String> managedDirectories, Map<String, EFileProccessingStatus> updatedFiles
    ) {
        Logger.log("<------> " + ServerSync.strings.getString("delete_start") + " <------>");
        Logger.log(String.format("Ignore patterns: %s", String.join(", ", config.FILE_IGNORE_LIST)));

        return managedDirectories.parallelStream()
                                 .map(Paths::get)
                                 .filter(this::filterShouldCheckDirectory)
                                 .flatMap(dir -> deleteDirectoryFiles(dir, updatedFiles).entrySet().stream())
                                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean filterShouldCheckDirectory(Path theDirectory) {
        if (IgnoredFilesMatcher.matches(theDirectory)) {
            Logger.log(String.format(
                "%s %s %s",
                RefStrings.IGNORE_TOKEN,
                ServerSync.strings.getString("ignoring"),
                theDirectory
            ));
            return false;
        }
        return true;
    }

    private Map<String, EFileProccessingStatus> deleteDirectoryFiles(
        Path theDirectory, Map<String, EFileProccessingStatus> updatedFiles
    ) {
        Map<String, EFileProccessingStatus> deletedFiles = new HashMap<>();
        try {
            Files.walkFileTree(theDirectory, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (IgnoredFilesMatcher.matches(dir)) {
                        Logger.log(String.format(
                            "%s %s %s",
                            RefStrings.IGNORE_TOKEN,
                            ServerSync.strings.getString("ignoring"),
                            dir
                        ));
                        // Pointless to continue as the client has set the whole directory to ignore.
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Logger.debug(String.format(
                        "Running delete comparison with (Server): %s, (Client files): %s",
                        file.toString(),
                        updatedFiles.keySet().toString()
                    ));
                    if (updatedFiles.containsKey(file.toString())) {
                        deletedFiles.put(file.toString(), EFileProccessingStatus.NO_WORK);
                        return FileVisitResult.CONTINUE;
                    }

                    if (IgnoredFilesMatcher.matches(file)) {
                        deletedFiles.put(file.toString(), EFileProccessingStatus.REFUSED);
                        Logger.log(String.format(
                            "%s %s %s",
                            RefStrings.IGNORE_TOKEN,
                            ServerSync.strings.getString("ignoring"),
                            file
                        ));
                        return FileVisitResult.CONTINUE;
                    }

                    try {
                        Files.deleteIfExists(file);
                        deletedFiles.put(file.toString(), EFileProccessingStatus.SUCCESS);
                        Logger.log(String.format(
                            "%s %s %s",
                            RefStrings.DELETE_TOKEN,
                            ServerSync.strings.getString("delete_success"),
                            file
                        ));
                    } catch (IOException e) {
                        Logger.debug(e);
                        Logger.log(String.format(
                            "%s %s %s",
                            RefStrings.ERROR_TOKEN,
                            ServerSync.strings.getString("message_file_failed_to_access"),
                            file
                        ));
                        deletedFiles.put(file.toString(), EFileProccessingStatus.FAILED);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    Logger.log(String.format(
                        "%s %s %s",
                        RefStrings.ERROR_TOKEN,
                        ServerSync.strings.getString("message_file_failed_to_access"),
                        file
                    ));
                    deletedFiles.put(file.toString(), EFileProccessingStatus.FAILED);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Logger.debug(e);
            Logger.log(String.format(
                "%s %s %s",
                RefStrings.ERROR_TOKEN,
                ServerSync.strings.getString("message_file_failed_to_access"),
                theDirectory
            ));
        }
        return deletedFiles;
    }

    private boolean filterShouldFileBeDeleted(Path theFile) {
        if (IgnoredFilesMatcher.matches(theFile)) {
            Logger.log(String.format(
                "%s %s %s",
                RefStrings.IGNORE_TOKEN,
                ServerSync.strings.getString("ignoring"),
                theFile
            ));
            return false;
        }
        return true;
    }

    private void deleteFile(Path theFile) {

        try {
            if (Files.deleteIfExists(theFile)) {
                Logger.log(String.format("<D> %s %s", theFile, ServerSync.strings.getString("delete_success")));
            } else {
                Logger.log("!!! failed to delete: " + theFile + " !!!");
            }
        } catch (IOException e) {
            Logger.debug(e);
        }
    }
}
