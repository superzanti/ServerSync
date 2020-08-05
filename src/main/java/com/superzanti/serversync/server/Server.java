package com.superzanti.serversync.server;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.config.IgnoredFilesMatcher;
import com.superzanti.serversync.filemanager.FileManager;
import com.superzanti.serversync.gui.FileProgress;
import com.superzanti.serversync.util.AutoClose;
import com.superzanti.serversync.util.FileHash;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EBinaryAnswer;
import com.superzanti.serversync.util.enums.EFileProccessingStatus;
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

    /**
     * Start mode 0 sync dialog with the server.
     *
     * @param afterEachFile A consumer that executes after each file is processed
     * @return A map of files processed to status
     */
    public Map<String, EFileProccessingStatus> syncFiles(VoidFunction afterEachFile) {
        // Server: Do you have this file?
        // - String: path
        // - String: hash
        // Client: yes | no (bit)
        // Server (yes) - skip to next file
        // Server (no) - send file
        Map<String, EFileProccessingStatus> processedFiles = new HashMap<>(100);
        boolean didSyncFiles = false;

        requestSync(0);

        // While I have more files to process...
        try {
            while (ois.readBoolean()) {
                // Server: Do you have this file?
                String path = ois.readUTF();
                String hash = ois.readUTF();
                Logger.debug(String.format("Processing file: %s, with hash: %s", path, hash));

                if (isClientOnlyFile(path)) {
                    if (config.REFUSE_CLIENT_MODS) {
                        // Skip this file essentially, possibly worth making a specific answer for client refused
                        // could be interesting for analytics.
                        Logger.log(String.format("<R> Refused client mod: %s", Paths.get(path)));
                        respond(EBinaryAnswer.YES);
                        processedFiles.put(Paths.get(path).toString(), EFileProccessingStatus.REFUSED);
                        afterEachFile.f();
                        continue;
                    } else {
                        // TODO make the destination server configurable
                        path = path.replaceFirst(FileManager.clientOnlyFilesDirectoryName, "mods");
                    }
                }

                Path clientFile = Paths.get(path);

                // Has the client set this file to be ignored, clients can refuse to accept files
                // from servers.
                if (IgnoredFilesMatcher.matches(clientFile)) {
                    Logger.debug(String.format("File: %s, set to ignore by the client.", clientFile));
                    Logger.log(String.format(
                        "%s %s %s",
                        RefStrings.IGNORE_TOKEN,
                        ServerSync.strings.getString("message_client_refused_file"),
                        clientFile
                    ));
                    respond(EBinaryAnswer.YES);
                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.REFUSED);
                    afterEachFile.f();
                    continue;
                }

                // Does the file exist on the client?
                //   - if it does then check its hash to see if it is the same file
                //       - if the hash check succeeds then we already have the file, skip to the next file
                if (Files.exists(clientFile) && hash.equals(FileHash.hashFile(clientFile))) {
                    // Client: I have that file already!
                    respond(EBinaryAnswer.YES);
                    Logger.log(String.format("File up to date: %s", clientFile));
                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.NO_WORK);
                    afterEachFile.f();
                    continue;
                }

                // Client: I don't have that file!
                respond(EBinaryAnswer.NO);
                Logger.debug(String.format("Don't have file: %s", clientFile));

                // Server: Here is the file.
                long fileSize = ois.readLong();
                if (updateFile(clientFile, fileSize)) {
                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.SUCCESS);
                    didSyncFiles = true;
                } else {
                    Logger.error(String.format("Failed to update file: %s", clientFile));
                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.FAILED);
                }
                afterEachFile.f();
            }
        } catch (IOException e) {
            Logger.error("Critical failure during sync process!");
            Logger.debug(e);
            return null;
        }

        if (!didSyncFiles) {
            Logger.log("All files match the server.");
        }

        return processedFiles;
    }

    private boolean updateFile(Path path, long size) {
        FileProgress GUIUpdater = new FileProgress();

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            Logger.debug("Could not create parent directories for: " + path.toString());
            Logger.debug(e);
        }

        if (size == 0 && Files.notExists(path)) {
            Logger.debug(String.format("Found a 0 byte file, writing an empty file to: %s", path));
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                return true;
            } catch (IOException e) {
                Logger.debug("Failed to write 0 size file.");
                return false;
            }
        }

        if (Files.exists(path)) {
            try {
                Files.delete(path);
                Files.createFile(path);

                // Zero size files do not need to continue
                // The server will not send any bytes through the socket
                if (size == 0) {
                    return true;
                }
            } catch (IOException e) {
                Logger.debug("Failed to delete file: " + path.getFileName().toString());
                Logger.debug(e);
                return false;
            }
        }

        try {
            Logger.debug("Attempting to write file (" + path.toString() + ")");
            OutputStream wr = Files.newOutputStream(path);

            byte[] outBuffer = new byte[clientSocket.getReceiveBufferSize()];

            int bytesReceived;
            long totalBytesReceived = 0L;
            while ((bytesReceived = ois.read(outBuffer)) > 0) {
                totalBytesReceived += bytesReceived;

                wr.write(outBuffer, 0, bytesReceived);
                GUIUpdater.updateProgress(
                    (int) Math.ceil((float) totalBytesReceived / size * 100),
                    path.getFileName().toString()
                );

                if (totalBytesReceived == size) {
                    break;
                }
            }
            wr.flush();
            wr.close();

            GUIUpdater.fileFinished();
            Logger.debug("Finished writing file" + path.toString());
        } catch (FileNotFoundException e) {
            Logger.debug("Failed to create file (" + path.toString() + "): " + e.getMessage());
            Logger.debug(e);
            return false;
        } catch (IOException e) {
            Logger.debug(e);
            return false;
        }

        Logger.log(
            String.format(
                "%s %s %s",
                RefStrings.UPDATE_TOKEN,
                ServerSync.strings.getString("update_success"),
                path.toString()
            ));
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

    private void requestSync(int mode) {
        // TODO implement a response to this request

        if (mode == 0) {
            String message = SCOMS.get(EServerMessage.SYNC_FILES);
            try {
                oos.writeObject(message);
                oos.flush();
            } catch (IOException e) {
                Logger.debug("Failed to send coms message to server: SYNC_FILES");
                Logger.debug(e);
            }
        }

        if (mode == 1) {
            // TODO sync via a single transmission of a manifest file
            Logger.error("Mode 1 not implemented yet!");
        }
    }

    private void respond(EBinaryAnswer answer) throws IOException {
        oos.writeInt(answer.getValue());
        oos.flush();
    }

    private boolean isClientOnlyFile(String path) {
        return path.startsWith(FileManager.clientOnlyFilesDirectoryName);
    }
}
