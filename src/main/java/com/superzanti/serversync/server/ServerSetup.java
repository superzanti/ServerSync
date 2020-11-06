package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.files.FileManager;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.FileRedirect;
import com.superzanti.serversync.files.FileEntry;
import com.superzanti.serversync.util.BannedIPSReader;
import com.superzanti.serversync.util.GlobPathMatcher;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.PrettyCollection;
import com.superzanti.serversync.util.enums.ELocations;
import com.superzanti.serversync.util.enums.EServerMessage;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
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
    private static final int SEND_BUFFER_SIZE = 1024 * 8;

    private final SyncConfig config = SyncConfig.getConfig();
    private final Path bannedIps = Paths.get(ELocations.BANNED_IPS.getValue());

    private final Timer timeoutScheduler = new Timer();

    private FileManifest manifest;
    private final List<String> messages = Arrays.stream(EServerMessage.values())
                                                .map(EServerMessage::toString)
                                                .collect(Collectors.toList());

    private FileManifest populateManifest() throws IOException {
        FileManifest manifest = new FileManifest();
        manifest.directories = config.DIRECTORY_INCLUDE_LIST;
        List<String> dirs = manifest.directories.stream().map(d -> d.path).collect(Collectors.toList());
        Map<String, String> files = FileManager.getDiffableFilesFromDirectories(dirs);
        files.forEach((key, value) -> {
            Path p = Paths.get(key);
            Optional<FileRedirect> re = config.REDIRECT_FILES_LIST
                .stream()
                .filter(r -> GlobPathMatcher.matches(p, r.pattern))
                .findFirst();
            if (re.isPresent()) {
                manifest.files.add(new FileEntry(key, value, re.get().redirectTo));
            } else {
                manifest.files.add(new FileEntry(key, value, ""));
            }
        });
        return manifest;
    }


    public ServerSetup() {
        DateFormat dateFormatter = DateFormat.getDateInstance();

        try {
            Logger.log(String.format("Starting server in mode: %s", config.SYNC_MODE));
            Logger.log("Starting scan for managed files: " + dateFormatter.format(new Date()));
            Logger.log(String.format("Ignore patterns: %s", PrettyCollection.get(config.FILE_IGNORE_LIST)));

            manifest = populateManifest();

            manifest.directories.stream().map(d -> ServerSync.rootDir.resolve(Paths.get(d.path))).forEach(p -> {
                if (Files.notExists(p)) {
                    Logger.error(String.format("Managed directory does not exist: %s", p));
                    System.exit(1);
                }
            });

            Logger.log(String.format(
                "Found %d files in %d directories",
                manifest.files.size(),
                manifest.directories.size()
            ));

            if (manifest.directories.size() > 0) {
                Logger.log(String.format("Managed files: %s", PrettyCollection.get(manifest.files)));
            }

            if (shouldPushClientOnlyFiles()) {
                Logger.log("Server configured to push client only mods, clients can still refuse these mods!");
                if (Files.notExists(FileManager.clientOnlyFilesDirectory)) {
                    Logger.log(String.format(
                        "%s directory did not exist, creating",
                        FileManager.clientOnlyFilesDirectoryName
                    ));
                    Files.createDirectories(FileManager.clientOnlyFilesDirectory);
                } else {
                    Map<String, String> clientOnlyFiles = FileManager.getDiffableFilesFromDirectories(
                        Collections.singletonList(FileManager.clientOnlyFilesDirectoryName)
                    );
                    Logger.log(String.format(
                        "Found %d files in %s",
                        clientOnlyFiles.size(),
                        FileManager.clientOnlyFilesDirectoryName
                    ));
                    if (clientOnlyFiles.size() > 0) {
                        Logger.log(String.format("Client only files: %s", PrettyCollection.get(clientOnlyFiles)));
                        for (Map.Entry<String, String> clientFile : clientOnlyFiles.entrySet()) {
                            manifest.files.add(
                                new FileEntry(clientFile.getValue(), clientFile.getKey(), "mods")
                            );
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Logger.debug("Creating new server socket");
        ServerSocket server;
        try {
            server = new ServerSocket(config.SERVER_PORT);
        } catch (BindException e) {
            Logger.error("Socket already bound at: " + config.SERVER_PORT);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // keep listening indefinitely until program terminates
        Logger.log("Now accepting clients...");

        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                InetAddress address = socket.getInetAddress();
                Logger.debug(String.format("Accepted connection from: %s", address));

                if (isIpBanned(address.getHostAddress())) {
                    socket.close();
                    Logger.log(String.format("Connection closed from banned IP address: %s", address.getHostAddress()));
                    continue;
                }

                socket.setSendBufferSize(ServerSetup.SEND_BUFFER_SIZE);
                ServerWorker sc = new ServerWorker(
                    socket,
                    messages,
                    timeoutScheduler,
                    manifest
                );
                Thread clientThread = new Thread(sc, "Server client Handler");
                clientThread.setName("ClientThread - " + address);
                clientThread.start();
            } catch (IOException e) {
                Logger.error(
                    "Error while accepting client connection, breaking server listener. You will need to restart ServerSync");
                try {
                    server.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private boolean isIpBanned(String ip) {
        if (Files.exists(bannedIps)) {
            try {
                return BannedIPSReader.read(bannedIps).contains(ip);
            } catch (IOException e) {
                Logger.debug("Failed to read banned-ips.json");
            }
        }
        Logger.debug("No banned-ips.json file exists, skipping ban check");
        return false;
    }

    public boolean shouldPushClientOnlyFiles() {
        return config.PUSH_CLIENT_MODS;
    }
}
