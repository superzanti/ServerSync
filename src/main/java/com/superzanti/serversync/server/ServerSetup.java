package com.superzanti.serversync.server;

import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.files.*;
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

    private final SyncConfig config = SyncConfig.getConfig();
    private final Path bannedIps = Paths.get(ELocations.BANNED_IPS.getValue());

    private final Timer timeoutScheduler = new Timer();
    private final Map<String, String> serverFiles = new HashMap<>(200);
    private final List<String> managedDirectories = new ArrayList<>(config.DIRECTORY_INCLUDE_LIST);


    private final FileManifest manifest = new FileManifest();
    private final List<String> messages = Arrays.stream(EServerMessage.values())
                                                .map(EServerMessage::toString)
                                                .collect(Collectors.toList());

    private void populateManifest(Map<String, String> files) {
        try {
            manifest.directories = config.DIRECTORY_INCLUDE_LIST;
            files.forEach((key, value) -> {
                Path p = Paths.get(key);
                Optional<FileRedirect> re = config.REDIRECT_FILES_LIST
                    .stream()
                    .filter(r -> GlobPathMatcher.matches(p, r.pattern))
                    .findFirst();
                if (re.isPresent()) {
                    manifest.entries.add(new ManifestEntry(key, value, re.get().redirectTo));
                } else {
                    manifest.entries.add(new ManifestEntry(key, value, ""));
                }
            });
            Logger.debug(String.format("Manifest directories: %s", PrettyCollection.get(manifest.directories)));
            Logger.debug(String.format("Manifest entries: %s", PrettyCollection.get(manifest.entries)));
        } catch (Exception e) {
            Logger.debug(e);
        }
    }


    public ServerSetup() {
        DateFormat dateFormatter = DateFormat.getDateInstance();

        try {
            Logger.log(String.format("Starting server in mode: %s", config.SYNC_MODE));
            Logger.log("Starting scan for managed files: " + dateFormatter.format(new Date()));
            Logger.log(String.format("Ignore patterns: %s", PrettyCollection.get(config.FILE_IGNORE_LIST)));

            for (String managedDirectory : managedDirectories) {
                Files.createDirectories(Paths.get(managedDirectory));
            }

            Map<String, String> managedFiles = FileManager.getDiffableFilesFromDirectories(managedDirectories);
            populateManifest(managedFiles);

            Logger.log(String.format(
                "Found %d files in %d directories <%s>",
                managedFiles.size(),
                managedDirectories.size(),
                String.join(", ", managedDirectories)
            ));
            if (managedFiles.size() > 0) {
                serverFiles.putAll(managedFiles);
                Logger.log(String.format("Managed files: %s", PrettyCollection.get(managedFiles)));
            }

            // Only include configs if some are actually listed
            // saves wasting time scanning the config directory.
            if (config.CONFIG_INCLUDE_LIST.size() > 0) {
                Logger.log(String.format("Starting scan for managed configs: %s", dateFormatter.format(new Date())));
                Logger.log(String.format("Include patterns: %s", PrettyCollection.get(config.CONFIG_INCLUDE_LIST)));
                // Add config include files
                Map<String, String> configIncludeFiles = config.CONFIG_INCLUDE_LIST
                    .stream()
                    .parallel()
                    .map(p -> new PathBuilder().add("config").add(p).toPath())
                    .filter(path -> Files.exists(path) && !IgnoredFilesMatcher.matches(path))
                    .collect(Collectors.toConcurrentMap(Path::toString, FileHash::hashFile));

                Logger.log(String.format(
                    "Found %d included configs in <config>",
                    configIncludeFiles.size()
                ));
                if (configIncludeFiles.size() > 0) {
                    Logger.log(String.format("Config files: %s", PrettyCollection.get(configIncludeFiles)));
                    serverFiles.putAll(configIncludeFiles);
                }
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
                        serverFiles.putAll(clientOnlyFiles);
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
