package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.files.*;
import com.superzanti.serversync.util.BannedIPSReader;
import com.superzanti.serversync.util.Glob;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.PrettyCollection;
import com.superzanti.serversync.util.enums.ELocation;
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
public class ServerSetup extends Thread {
    private final SyncConfig config = SyncConfig.getConfig();
    private final Path bannedIps = Paths.get(ELocation.BANNED_IPS.getValue());

    private final Timer timeoutScheduler = new Timer();

    private FileManifest manifest;
    private final List<String> messages = Arrays.stream(EServerMessage.values())
                                                .map(EServerMessage::toString)
                                                .collect(Collectors.toList());

    private ServerSocket server;

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private FileManifest populateManifest() throws IOException {
        FileManifest manifest = new FileManifest();
        manifest.directories = config.DIRECTORY_INCLUDE_LIST;
        if (config.PUSH_CLIENT_MODS) {
            Logger.log("Server configured to push client only mods, clients can still refuse these mods!");

            // Create clientmods if it does not exist already
            Files.createDirectories(FileManager.clientOnlyFilesDirectory);

            config.FILE_INCLUDE_LIST.add("clientmods/**");
            config.REDIRECT_FILES_LIST.add(new FileRedirect("clientmods/**", "mods"));
        }

        // Standard file handling, ignoring directories here as they are not relevant to serversync
        List<Path> included = Files
            .walk(ServerSync.rootDir)
            .filter(f -> !Files.isDirectory(f))
            .map(f -> ServerSync.rootDir.relativize(f))
            .filter(f -> Glob.matches(f, config.FILE_INCLUDE_LIST))
            .collect(Collectors.toList());

        List<Path> filtered = included
            .stream()
            .filter(f -> !Glob.matches(f, config.FILE_IGNORE_LIST))
            .collect(Collectors.toList());

        List<String> includeMap = filtered
            .stream()
            // optional get can never be missing as we have just filtered the list by matching patterns
            .map(
                f -> String.format("%s, Pattern: %s", f.toString(), Glob.getPattern(f, config.FILE_INCLUDE_LIST).get()))
            .collect(Collectors.toList());
        Logger.debug(String.format("Included files: %s", PrettyCollection.get(includeMap)));

        List<String> excludeMap = included
            .stream()
            .filter(f -> Glob.matches(f, config.FILE_IGNORE_LIST))
            // optional get can never be missing as we have just filtered the list by matching patterns
            .map(f -> String.format("%s, Pattern: %s", f.toString(), Glob.getPattern(f, config.FILE_IGNORE_LIST).get()))
            .collect(Collectors.toList());
        Logger.debug(String.format("Ignored files: %s", PrettyCollection.get(excludeMap)));

        manifest.files = filtered
            .stream()
            .map(f -> {
                String fileHash = FileHash.hashFile(ServerSync.rootDir.resolve(f));
                Optional<FileRedirect> redirect = config.REDIRECT_FILES_LIST
                    .stream().filter(r -> Glob.matches(f, r.pattern)).findFirst();

                return redirect
                    .map(fileRedirect -> {
                        if (fileRedirect.pattern.equals("clientmods/**")) {
                            return new FileEntry(f.toString(), fileHash, fileRedirect.redirectTo, true);
                        }
                        return new FileEntry(f.toString(), fileHash, fileRedirect.redirectTo);
                    })
                    .orElseGet(() -> new FileEntry(f.toString(), fileHash));
            }).collect(Collectors.toList());

        return manifest;
    }


    public ServerSetup() {
        this.setName("ServerSync - Server");
        DateFormat dateFormatter = DateFormat.getDateInstance();

        try {
            Logger.log(String.format("Starting server in mode: %s", config.SYNC_MODE));
            Logger.log("Starting scan for managed files: " + dateFormatter.format(new Date()));
            Logger.log(String.format("Ignore patterns: %s", PrettyCollection.get(config.FILE_IGNORE_LIST)));

            manifest = populateManifest();

            Logger.log(String.format("Manifest files: %s", PrettyCollection.get(manifest.files)));

            manifest.directories.stream().map(d -> ServerSync.rootDir.resolve(Paths.get(d.path))).forEach(p -> {
                if (Files.notExists(p)) {
                    Logger.error(String.format("Managed directory does not exist: %s", p));
                    System.exit(1);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void interrupt() {
        try {
            Logger.log("Server interrupt received, shutting down");
            server.close();
            timeoutScheduler.cancel();
        } catch (IOException e) {
            // ignore
        }
        super.interrupt();
    }


    @Override
    public void run() {
        Logger.debug("Creating new server socket");
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

                ServerWorker sc = new ServerWorker(
                    socket,
                    messages,
                    timeoutScheduler,
                    manifest
                );
                Thread clientThread = new Thread(sc, "Server client Handler");
                clientThread.setName("ServerSync - Server Client: " + address);
                clientThread.start();
            } catch (IOException e) {
                Logger.error(
                    "Error while waiting for client connection, terminating server listener. You will need to restart ServerSync"
                );
                try {
                    server.close();
                    timeoutScheduler.cancel();
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
