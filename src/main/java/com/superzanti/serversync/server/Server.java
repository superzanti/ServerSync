package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.gui.FileProgress;
import com.superzanti.serversync.util.AutoClose;
import com.superzanti.serversync.util.FileHash;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EBinaryAnswer;
import com.superzanti.serversync.util.enums.EServerMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Interacts with a server running serversync
 *
 * @author Rheimus
 */
public class Server {
    private final String IP_ADDRESS;
    private final int PORT;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private Socket clientSocket = null;
    private EnumMap<EServerMessage, String> SCOMS;
    private SyncConfig config = SyncConfig.getConfig();

    public Server(String ip, int port) {
        IP_ADDRESS = ip;
        PORT = port;
    }

    @SuppressWarnings("unchecked")
    public boolean connect() {
        InetAddress host;
        try {
            host = InetAddress.getByName(IP_ADDRESS);
        } catch (UnknownHostException e) {
            Logger.error(ServerSync.strings.getString("connection_failed_host") + ": " + IP_ADDRESS);
            return false;
        }

        Logger.debug(ServerSync.strings.getString("connection_attempt_server"));
        clientSocket = new Socket();

        Logger.log("< " + ServerSync.strings.getString("connection_message") + " >");
        try {
            clientSocket.connect(new InetSocketAddress(host.getHostName(), PORT), 5000);
        } catch (IOException e) {
            Logger.error(ServerSync.strings.getString("connection_failed_server") + ": " + IP_ADDRESS + ":" + PORT);
            AutoClose.closeResource(clientSocket);
            return false;
        }

        Logger.debug(ServerSync.strings.getString("debug_IO_streams"));
        try {
            clientSocket.setPerformancePreferences(0, 1, 2);
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ois = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            Logger.debug(ServerSync.strings.getString("debug_IO_streams_failed"));
            AutoClose.closeResource(clientSocket);
            return false;
        }

        try {
            oos.writeObject(ServerSync.HANDSHAKE);
        } catch (IOException e) {
            Logger.outputError(ServerSync.HANDSHAKE);
        }

        try {
            SCOMS = (EnumMap<EServerMessage, String>) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Logger.inputError(e.getMessage());
        }

        System.out.println(SCOMS);

        return true;
    }

    public List<String> fetchManagedDirectories() {
        Logger.debug("Fetching managed directories from server");
        String message = SCOMS.get(EServerMessage.GET_MANAGED_DIRECTORIES);

        try {
            oos.writeObject(message);
            oos.flush();

            @SuppressWarnings("unchecked")
            List<String> directories = (List<String>) ois.readObject();
            return directories;
        } catch (IOException e) {
            Logger.debug("Failed to write object (" + message + ") to client output stream");
        } catch (ClassCastException | ClassNotFoundException e) {
            Logger.debug("Unexpected object read from input stream");
            Logger.debug(e);
        }
        return null;
    }

    public int fetchNumberOfServerManagedFiles() {
        Logger.debug("Fetching number of managed files from server");
        String message = SCOMS.get(EServerMessage.GET_NUMBER_OF_MANAGED_FILES);
        try {
            oos.writeObject(message);
            oos.flush();

            return ois.readInt();
        } catch (IOException e) {
            Logger.debug(e);
        }
        return -1;
    }

    public Map<String, String> syncFiles(List<Path> clientFiles, VoidFunction guiUpdate) {
        // Server: Do you have this file?
        // - String: path
        // - String: hash
        // Client: yes | no (bit)
        // Server (yes) - skip to next file
        // Server (no) - send file
        Map<String, String> leftoverFiles = new HashMap<>();
        List<String> clientPaths = clientFiles.stream().map(Path::toString).collect(Collectors.toList());
        String message = SCOMS.get(EServerMessage.SYNC_FILES);
        boolean didSyncFiles = false;

        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            Logger.debug("Failed to send coms message to server: SYNC_FILES");
            Logger.debug(e);
        }

        // While I have more files to process...
        try {
            while (ois.readBoolean()) {
                // Server: Do you have this file?
                String path = ois.readUTF();
                String hash = ois.readUTF();

                if (isClientOnlyFile(path)) {
                    if (config.REFUSE_CLIENT_MODS) {
                        // Skip this file essentially, possibly worth making a specific answer for client refused
                        // could be interesting for analytics.
                        clientPaths.remove(path);
                        Logger.log(String.format("<R> Refused client mod: %s", path));
                        respond(EBinaryAnswer.YES);
                        guiUpdate.f();
                        continue;
                    } else {
                        // TODO make the destination server configurable
                        path = path.replaceFirst(FileManager.clientOnlyFilesDirectoryName, "mods");
                    }
                }

                Path clientFile = Paths.get(path);

                if (IgnoredFilesMatcher.matches(clientFile)) {
                    Logger.debug(String.format("File: %s, set to ignore by the client.", path));
                    clientPaths.remove(path);
                    respond(EBinaryAnswer.YES);
                    guiUpdate.f();
                    continue;
                }

                // Does the file exist on the client && does the hash match
                if (Files.exists(clientFile) && hash.equals(FileHash.hashFile(clientFile))) {
                    // Client: Yes I do!
                    clientPaths.remove(path);
                    respond(EBinaryAnswer.YES);
                    Logger.log(String.format("File up to date: %s", path));
                } else {
                    didSyncFiles = true;
                    // Client: No I don't!
                    respond(EBinaryAnswer.NO);
                    Logger.debug(String.format("Don't have file: %s", path));

                    // Server: Here is the file.
                    long fileSize = ois.readLong();
                    if (updateFile(path, fileSize)) {
                        clientPaths.remove(path);
                    } else {
                        Logger.error(String.format("Failed to update file: %s", path));
                        leftoverFiles.put(path, "retry");
                    }
                }
                guiUpdate.f();
            }
        } catch (IOException e) {
            Logger.error("Critical failure during sync process!");
            Logger.debug(e);
            return null;
        }

        if (!didSyncFiles) {
            Logger.log("All files match the server.");
        }

        // Return list of remaining files to deal with
        clientPaths.forEach(remainingPath -> {
            leftoverFiles.put(remainingPath, "delete");
        });

        return leftoverFiles;
    }

    private boolean updateFile(String path, long size) {
        FileProgress GUIUpdater = new FileProgress();

        Path clientFile = Paths.get(path);
        try {
            Files.createDirectories(clientFile.getParent());
        } catch (IOException e) {
            Logger.debug("Could not create parent directories for: " + clientFile.toString());
            Logger.debug(e);
        }

        if (size == 0 && Files.notExists(clientFile)) {
            Logger.debug(String.format("Found a 0 byte file, writing an empty file to: %s", path));
            try {
                Files.createDirectories(clientFile.getParent());
                Files.createFile(clientFile);
                return true;
            } catch (IOException e) {
                Logger.debug("Failed to write 0 size file.");
                return false;
            }
        }

        if (Files.exists(clientFile)) {
            try {
                Files.delete(clientFile);
                Files.createFile(clientFile);

                // Zero size files do not need to continue
                // The server will not send any bytes through the socket
                if (size == 0) {
                    return true;
                }
            } catch (IOException e) {
                Logger.debug("Failed to delete file: " + clientFile.getFileName().toString());
                Logger.debug(e);
                return false;
            }
        }

        try {
            Logger.debug("Attempting to write file (" + clientFile.toString() + ")");
            OutputStream wr = Files.newOutputStream(clientFile);

            byte[] outBuffer = new byte[clientSocket.getReceiveBufferSize()];

            int bytesReceived;
            long totalBytesReceived = 0L;
            while ((bytesReceived = ois.read(outBuffer)) > 0) {
                totalBytesReceived += bytesReceived;

                wr.write(outBuffer, 0, bytesReceived);
                GUIUpdater.updateProgress(
                    (int) Math.ceil((float) totalBytesReceived / size * 100),
                    clientFile.getFileName().toString()
                );

                if (totalBytesReceived == size) {
                    break;
                }
            }
            wr.flush();
            wr.close();

            GUIUpdater.fileFinished();
            Logger.debug("Finished writing file" + clientFile.toString());
        } catch (FileNotFoundException e) {
            Logger.debug("Failed to create file (" + clientFile.toString() + "): " + e.getMessage());
            Logger.debug(e);
            return false;
        } catch (IOException e) {
            Logger.debug(e);
            return false;
        }

        Logger.log(ServerSync.strings.getString("update_success") + ": " + clientFile.toString());
        return true;
    }

    /**
     * Terminates the listener thread on the server for this client
     */
    private void exit() {
        if (SCOMS == null) {
            // NO server messages set up, server must have not connected at this point
            return;
        }
        String message = SCOMS.get(EServerMessage.EXIT);
        Logger.debug(ServerSync.strings.getString("debug_server_exit"));

        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            Logger.debug("Failed to write object (" + message + ") to client output stream");
        }
    }

    /**
     * Releases resources related to this server instance, MUST call this when
     * interaction is finished if a server is opened
     *
     * @return true if client successfully closes all connections
     */
    public boolean close() {
        exit();
        Logger.debug(ServerSync.strings.getString("debug_server_close"));
        try {
            if (clientSocket != null && !clientSocket.isClosed())
                clientSocket.close();
        } catch (IOException e) {
            Logger.debug("Failed to close client socket: " + e.getMessage());
            return false;
        }
        Logger.debug(ServerSync.strings.getString("debug_server_close_success"));
        return true;
    }

    private void respond(EBinaryAnswer answer) throws IOException {
        oos.writeInt(answer.getValue());
        oos.flush();
    }

    private boolean isClientOnlyFile(String path) {
        return path.startsWith(FileManager.clientOnlyFilesDirectoryName);
    }
}
