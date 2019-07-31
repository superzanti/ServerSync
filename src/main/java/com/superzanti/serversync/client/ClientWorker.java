package com.superzanti.serversync.client;

import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.server.Server;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EFileMatchingMode;
import com.superzanti.serversync.util.errors.InvalidSyncFileException;
import com.superzanti.serversync.util.minecraft.MinecraftModInformation;
import runme.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Deals with all of the synchronizing for the client, this works without
 * starting minecraft
 *
 * @author Rheimus
 */
public class ClientWorker implements Runnable {

    private boolean errorInUpdates = false;
    private boolean updateHappened = false;
    private boolean finished = false;

    private Server server;

    private List<SyncFile> ignoredClientSideFiles;
    private FileManager fileManager = new FileManager();

    public ClientWorker() {
        ignoredClientSideFiles = new ArrayList<>(20);
        errorInUpdates = false;
        updateHappened = false;
        finished = false;
    }

    public boolean getErrors() {
        return errorInUpdates;
    }

    public boolean getUpdates() {
        return updateHappened;
    }

    public boolean isFinished() {
        return finished;
    }

    private void closeWorker() {
        if (server == null) {
            return;
        }

        if (server.close()) {
            Logger.debug("Successfully closed all connections");
        }

        if (!updateHappened && !errorInUpdates) {
            Logger.log(Main.strings.getString("update_not_needed"));
            Main.clientGUI.updateProgress(100);
        } else {
            Logger.debug(Main.strings.getString("update_happened"));
            Main.clientGUI.updateProgress(100);
        }

        if (errorInUpdates) {
            Logger.error(Main.strings.getString("update_error"));
        }

        Main.clientGUI.enableSyncButton();
    }

    private List<SyncFile> getClientFiles(ArrayList<String> directories) {
        boolean addConfigFiles = true;

        for (String directory : directories) {
            // Currently servers can add the config directory to the included dirs list
            // this essentially switches the included configs from whitelist to blacklist
            // TODO make this system simpler
            if (directory.equals("config")) {
                addConfigFiles = false;
            }
        }

        List<SyncFile> clientFiles = fileManager.getModFiles(
            directories,
            EFileMatchingMode.INGORE
        );

        if (addConfigFiles) {
            ArrayList<SyncFile> configurationFiles = fileManager
                .getConfigurationFiles(Main.CONFIG.CONFIG_INCLUDE_LIST, EFileMatchingMode.INCLUDE);
            if (configurationFiles.size() > 0) {
                clientFiles.addAll(configurationFiles);
            } else {
                Logger.debug("Found no configuration files.");
            }
        }
        return clientFiles;
    }

    private void updateFiles(List<SyncFile> clientFiles, List<SyncFile> serverFiles) {
        Logger.log("<------> " + Main.strings.getString("update_start") + " <------>");
        Logger.debug(Main.strings.getString("ignoring") + " " + Main.CONFIG.FILE_IGNORE_LIST);

        int currentProgress = 0;
        int maxProgress = serverFiles.size();

        for (SyncFile serverFile : serverFiles) {
            SyncFile clientFile;
            if (serverFile.isClientSideOnlyFile) {
                // TODO link this to a config value
                clientFile = SyncFile.ClientOnlySyncFile(serverFile.getClientSidePath());
                ignoredClientSideFiles.add(clientFile);
                Logger.log(Main.strings.getString("mods_clientmod_added") + ": " + clientFile.getFileName());
            } else {
                clientFile = SyncFile.StandardSyncFile(serverFile.getFileAsPath());
            }

            boolean exists = Files.exists(clientFile.getFileAsPath());

            if (exists) {
                try {
                    if (!clientFile.equals(serverFile)) {
                        server.updateFile(serverFile, clientFile);
                    } else {
                        Logger.log(clientFile.getFileName() + " " + Main.strings.getString("up_to_date"));
                    }
                } catch (InvalidSyncFileException e) {
                    // TODO stub invalid file handling
                    Logger.debug(e);
                }
            } else {
                // Ignore support for client only files, users may wish to not allow some mods
                // out of personal preference
                if (serverFile.isClientSideOnlyFile && serverFile.matchesIgnoreListPattern()) {
                    Logger.log("<>" + Main.strings.getString("ignoring") + " " + serverFile.getFileName());
                } else {
                    Logger.debug(serverFile.getFileName() + " " + Main.strings.getString("does_not_exist"));
                    server.updateFile(serverFile, clientFile);
                }
            }

            Main.clientGUI.updateProgress((int) (++currentProgress / maxProgress));
        }
    }

    private void deleteFiles(List<SyncFile> clientFiles, List<SyncFile> serverFiles) {
        Logger.log("<------> " + Main.strings.getString("delete_start") + " <------>");
        Logger.log(String.format("Ignore patterns: %s", String.join(", ", Main.CONFIG.FILE_IGNORE_LIST)));
        int currentProgress = 0;
        int maxProgress = clientFiles.size();

        for (SyncFile clientFile : clientFiles) {
            if (clientFile.matchesIgnoreListPattern()) {
                // User created ignore rules
                Logger.debug(Main.strings.getString("ignoring") + " " + clientFile.getFileName());
            } else {
                Logger.debug(Main.strings.getString("client_check") + " " + clientFile.getFileName());

                if (!serverFiles.contains(clientFile)) {
                    if (clientFile.delete()) {
                        Logger.log("<>" + clientFile.getFileName() + " " + Main.strings.getString("delete_success"));
                        Path parentDirectory = clientFile.getClientSidePath().getParent();

                        if (parentDirectory != null && Files.isDirectory(parentDirectory)
                            && !parentDirectory.getFileName().toString().matches("mods|minecraft")) {
                            try {
                                Files.delete(parentDirectory);
                            } catch (IOException e) {
                                // Don't actually care if this fails, this either means there are files
                                // left in the directory so we don't want to delete it
                                // or some other failure to delete has happened, eg permissions
                            }
                        }
                    } else {
                        Logger.log("!!! failed to delete: " + clientFile.getFileName() + " !!!");
                    }
                    updateHappened = true;
                }

                Main.clientGUI.updateProgress((int) (++currentProgress / maxProgress));
            }
        }
    }

    private void duplicateCheck(List<SyncFile> clientFiles) {
        // ENHANCE: User dialog to pick a file to keep?
        ArrayList<String> modNames = new ArrayList<>(200);
        ArrayList<String> modHashes = new ArrayList<>(200);
        ArrayList<SyncFile> dupes = new ArrayList<>(10);

        for (SyncFile clientFile : clientFiles) {
            MinecraftModInformation modInfo = clientFile.getModInformation();
            if (modInfo != null) {
                if (modNames.contains(modInfo.name)) {
                    Logger.log("<!> Potential duplicate: " + clientFile.getFileName() + " - " + modInfo.name);
                    dupes.add(clientFile);
                } else {
                    modNames.add(modInfo.name);
                }
            } else {
                String hash = clientFile.getFileHash();
                if (modHashes.contains(hash)) {
                    Logger.log("<!> Potential duplicate: " + clientFile.getFileName() + " - " + hash);
                    dupes.add(clientFile);
                } else {
                    modHashes.add(hash);
                }
            }
        }
    }

    @Override
    public void run() {
        updateHappened = false;

        Main.clientGUI.disableSyncButton();
        Logger.getLog().clearUserFacingLog();

        server = new Server(this, Main.CONFIG.SERVER_IP, Main.CONFIG.SERVER_PORT);

        if (!server.connect()) {
            errorInUpdates = true;
            this.closeWorker();
            return;
        }

        ArrayList<String> syncableDirectories = server.getSyncableDirectories();
        if (syncableDirectories == null) {
            errorInUpdates = true;
            closeWorker();
            return;
        }

        if (syncableDirectories.isEmpty()) {
            Logger.log(Main.strings.getString("no_syncable_directories"));
            finished = true;
            closeWorker();
            return;
        }

        List<SyncFile> clientFiles = getClientFiles(syncableDirectories);

        Logger.debug("Checking Server.isUpdateNeeded()");
        Logger.debug(clientFiles.toString());
        boolean updateNeeded = server.isUpdateNeeded(clientFiles);
        updateNeeded = true; // TODO TEMP

        /* MAIN PROCESSING CHUNK */
        if (updateNeeded) {
            updateHappened = true;
            Logger.log(Main.strings.getString("mods_incompatable"));
            Logger.log("<------> " + "Getting files" + " <------>");

            Logger.log(Main.strings.getString("mods_get"));
            ArrayList<SyncFile> serverFiles = server.getFiles();

            if (serverFiles == null) {
                Logger.log("Failed to get files from server, check detailed log in minecraft/logs");
                errorInUpdates = true;
                closeWorker();
                return;
            }

            if (serverFiles.isEmpty()) {
                Logger.log("Server has no syncable files");
                finished = true;
                closeWorker();
                return;
            }

            /* CLIENT SPECIFIC MODS */
            // These are files that do not need to be present on the server to connect and
            // play
            // These are only added if the user wanting to connect to the server has
            // ServerSync configured to accept them
            if (!Main.CONFIG.REFUSE_CLIENT_MODS) {
                Logger.log(Main.strings.getString("mods_accepting_clientmods"));

                ArrayList<SyncFile> serverClientOnlyMods = server.getClientOnlyFiles();

                if (serverClientOnlyMods == null) {
                    // TODO add to TDB
                    Logger.log("Failed to access servers client only mods");
                    errorInUpdates = true;
                } else {
                    serverFiles.addAll(serverClientOnlyMods);
                }
            } else {
                Logger.log(Main.strings.getString("mods_refusing_clientmods"));
            }

            updateFiles(clientFiles, serverFiles);

            deleteFiles(clientFiles, serverFiles);

            // Get a new list of client files as we will have modified them during the
            // previous phases
            duplicateCheck(getClientFiles(syncableDirectories));

        }

        closeWorker();
        Logger.log(Main.strings.getString("update_complete"));
    }

}
