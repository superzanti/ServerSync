package com.superzanti.serversync.server;

import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.util.*;
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

    private SyncConfig config = SyncConfig.getConfig();
    private final Path bannedIps = Paths.get(ELocations.BANNED_IPS.getValue());

    private Timer timeoutScheduler = new Timer();
    private Map<String, String> serverFiles = new HashMap<>(200);
    private List<String> managedDirectories = new ArrayList<>(config.DIRECTORY_INCLUDE_LIST);

    private static EnumMap<EServerMessage, String> generateServerMessages() {
        EnumMap<EServerMessage, String> SERVER_MESSAGES = new EnumMap<>(EServerMessage.class);

        for (EServerMessage msg : EServerMessage.values()) {
            // What is this doing? who knows but its fun!
            double rng = Math.random() * 1000d;
            String hashKey = String.format("%s", msg.hashCode() + rng);

            SERVER_MESSAGES.put(msg, hashKey);
        }

        return SERVER_MESSAGES;
    }

    public ServerSetup() {
        DateFormat dateFormatter = DateFormat.getDateInstance();
        FileManager fileManager = new FileManager();

        try {
            Logger.log("Starting scan for managed files: " + dateFormatter.format(new Date()));
            Logger.debug(String.format("Ignore patterns: %s", String.join(", ", config.FILE_IGNORE_LIST)));

            for (String managedDirectory : managedDirectories) {
                Files.createDirectories(Paths.get(managedDirectory));
            }

            Map<String, String> managedFiles = fileManager.getDiffableFilesFromDirectories(managedDirectories);

            Logger.log(String.format(
                "Found %d files in %d directories <%s>",
                managedFiles.size(),
                managedDirectories.size(),
                String.join(", ", managedDirectories)
            ));
            serverFiles.putAll(managedFiles);

            // Add config include files
            Map<String, String> configIncludeFiles = config.CONFIG_INCLUDE_LIST
                .stream()
                .parallel()
                .map(p -> new PathBuilder("config").add(p).buildPath())
                .filter(path -> Files.exists(path) && !IgnoredFilesMatcher.matches(path))
                .collect(Collectors.toMap(Path::toString, FileHash::hashFile));

            Logger.log(String.format(
                "Found %d included configs in <config>",
                configIncludeFiles.size()
            ));
            Logger.debug("Config files: " + String.join(",", configIncludeFiles.keySet()));
            serverFiles.putAll(configIncludeFiles);

            if (shouldPushClientOnlyFiles()) {
                Logger.log("Server configured to push client only mods, clients can still refuse these mods!");
                if (Files.notExists(fileManager.clientOnlyFilesDirectory)) {
                    Logger.log(String.format(
                        "%s directory did not exist, creating",
                        FileManager.clientOnlyFilesDirectoryName
                    ));
                    Files.createDirectories(fileManager.clientOnlyFilesDirectory);
                } else {
                    Map<String, String> clientOnlyFiles = fileManager.getDiffableFilesFromDirectories(
                        Collections.singletonList(FileManager.clientOnlyFilesDirectoryName)
                    );
                    Logger.log(String.format(
                        "Found %d files in %s",
                        clientOnlyFiles.size(),
                        FileManager.clientOnlyFilesDirectoryName
                    ));
                    Logger.debug(clientOnlyFiles.toString());
                    serverFiles.putAll(clientOnlyFiles);
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
                    socket, generateServerMessages(), timeoutScheduler, managedDirectories, serverFiles);
                Thread clientThread = new Thread(sc, "Server client Handler");
                clientThread.setName("ClientThread - " + address);
                clientThread.start();
            } catch (IOException e) {
                Logger.error(
                    "Error while accepting client connection, breaking server listener. You will need to restart serversync");
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
