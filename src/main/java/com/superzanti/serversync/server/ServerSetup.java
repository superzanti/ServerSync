package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EFileMatchingMode;
import com.superzanti.serversync.util.enums.EServerMessage;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sets up various server data to be passed to the specific client socket being
 * communicated with
 *
 * @author Rheimus
 */
public class ServerSetup implements Runnable {
    private static ServerSocket server;

    // This is what's in our folders
    public static ArrayList<SyncFile> allFiles = new ArrayList<>(200);
    public static ArrayList<SyncFile> standardSyncableFiles = new ArrayList<>(200);
    public static ArrayList<SyncFile> standardFiles = new ArrayList<>(75);
    public static ArrayList<SyncFile> configFiles = new ArrayList<>(200);
    public static ArrayList<SyncFile> clientOnlyFiles = new ArrayList<>(20);
    public static ArrayList<String> directories = new ArrayList<>(20);

    private Timer timeoutScheduler = new Timer();

    public static EnumMap<EServerMessage, String> generateServerMessages() {
        EnumMap<EServerMessage, String> SERVER_MESSAGES = new EnumMap<>(EServerMessage.class);

        for (EServerMessage msg : EServerMessage.values()) {
            double rng = Math.random() * 1000d;
            String hashKey = DigestUtils.sha1Hex(msg.toString() + rng);

            SERVER_MESSAGES.put(msg, hashKey);
        }

        return SERVER_MESSAGES;
    }

    public ServerSetup() {
        DateFormat dateFormatter = DateFormat.getDateInstance();
        FileManager fileManager = new FileManager();

        boolean configsInDirectoryList = false;

        /* SYNC DIRECTORIES */
        for (String dir : ServerSync.CONFIG.DIRECTORY_INCLUDE_LIST) {
            // Specific config handling later
            if (dir.equals("config") || dir.equals("clientmods")) {
                if (dir.equals("config")) {
                    configsInDirectoryList = true;
                    directories.add(dir);
                }
                continue;
            }
            directories.add(dir);
        }

        if (shouldPushClientOnlyFiles()) {
            Logger.log("Server configured to push client only mods, clients can still refuse these mods!");
            // Create clientmods directory if it does not exist
            Path clientOnlyMods = Paths.get("clientmods/");
            if (!Files.exists(clientOnlyMods)) {
                try {
                    Files.createDirectories(clientOnlyMods);
                    Logger.log("clientmods directory did not exist, creating");
                } catch (IOException e) {
                    Logger.error("Could not create clientmods directory");
                }
            }

            clientOnlyFiles = fileManager.getClientOnlyFiles();
            Logger.log(String.format("Found %d files in clientmods", clientOnlyFiles.size()));
        }

        // Main directory scan for mods
        Logger.log("Starting scan for sync files: " + dateFormatter.format(new Date()));
        Logger.debug(String.format("Ignore patterns: %s", String.join(", ", ServerSync.CONFIG.FILE_IGNORE_LIST)));
        standardFiles = fileManager.getModFiles(directories, EFileMatchingMode.INGORE);
        Logger.log(String.format("Found %d files that match user defined patterns", standardFiles.size()));

        List<String> fileNames = standardFiles.stream().map(SyncFile::getFileName).collect(Collectors.toList());
        Logger.log(fileNames.toString());

        /* CONFIGS */
        // If the include list is empty then we have no configs to add
        // If the user has added the config directory to the directory list then they are switching to blacklist mode
        // configs in this mode will be treated as standard files
        // TODO clean up this cruft, just let the user switch their config matching list from white to blacklist in the SS config
        if (!ServerSync.CONFIG.CONFIG_INCLUDE_LIST.isEmpty() && !configsInDirectoryList) {
            configFiles = fileManager.getConfigurationFiles(ServerSync.CONFIG.CONFIG_INCLUDE_LIST, EFileMatchingMode.INCLUDE);
        }

        ServerSetup.allFiles.addAll(ServerSetup.clientOnlyFiles);
        ServerSetup.allFiles.addAll(ServerSetup.standardFiles);
        ServerSetup.allFiles.addAll(ServerSetup.configFiles);

        ServerSetup.standardSyncableFiles.addAll(ServerSetup.standardFiles);
        ServerSetup.standardSyncableFiles.addAll(ServerSetup.configFiles);
    }

    @Override
    public void run() {
        Logger.debug("Creating new server socket");
        try {
            server = new ServerSocket(ServerSync.CONFIG.SERVER_PORT);
        } catch (BindException e) {
            Logger.error("Socket alredy bound at: " + ServerSync.CONFIG.SERVER_PORT);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // keep listening indefinitely until program terminates
        Logger.log("Now accepting clients...");

        while (true) {
            try {
                // Sanity check, server should never be null here
                if (server == null) {
                    break;
                }
                Socket socket = server.accept();
                ServerWorker sc = new ServerWorker(socket, generateServerMessages(), timeoutScheduler);
                Thread clientThread = new Thread(sc, "Server client Handler");
                clientThread.setName("ClientThread - " + socket.getInetAddress());
                clientThread.start();
            } catch (IOException e) {
                Logger.error(
                    "Error while accepting client connection, breaking server listener. You will need to restart serversync");
            }
        }
    }

    public boolean shouldPushClientOnlyFiles() {
        return ServerSync.CONFIG.PUSH_CLIENT_MODS;
    }
}
