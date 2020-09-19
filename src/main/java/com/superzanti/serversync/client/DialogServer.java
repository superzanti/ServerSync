package com.superzanti.serversync.client;

import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.communication.Requests;

public class DialogServer {
    private final Server server;
    private final Requests requests;

    private final SyncConfig config = SyncConfig.getConfig();

    public DialogServer(Server server) {
        this.server = server;
        requests = Requests.forServer(server);
    }

//    public List<String> fetchManagedDirectories() {
//        return requests.getManagedDirectories();
//    }
//
//    public int fetchNumberOfServerManagedFiles() {
//        try {
//            return requests.getNumberOfManagedFiles();
//        }
//        Logger.debug("Fetching number of managed files from server");
//        String message = info.messages.get(EServerMessage.GET_NUMBER_OF_MANAGED_FILES);
//        try {
//            oos.writeObject(message);
//            oos.flush();
//
//            return ois.readInt();
//        } catch (IOException e) {
//            Logger.debug(e);
//        }
//        return -1;
//    }
//
//    /**
//     * Start mode 0 sync dialog with the server.
//     *
//     * @param afterEachFile A consumer that executes after each file is processed
//     * @return A map of files processed to status
//     */
//    public Map<String, EFileProccessingStatus> syncFiles(VoidFunction afterEachFile) {
//        // Server: Do you have this file?
//        // - String: path
//        // - String: hash
//        // Client: yes | no (bit)
//        // Server (yes) - skip to next file
//        // Server (no) - send file
//        Map<String, EFileProccessingStatus> processedFiles = new HashMap<>(100);
//        boolean didSyncFiles = false;
//
//        requestSync(0);
//
//        // While I have more files to process...
//        try {
//            while (ois.readBoolean()) {
//                // Server: Do you have this file?
//                String path = ois.readUTF();
//                String hash = ois.readUTF();
//                Logger.debug(String.format("Processing file: %s, with hash: %s", path, hash));
//
//                if (isClientOnlyFile(path)) {
//                    if (config.REFUSE_CLIENT_MODS) {
//                        // Skip this file essentially, possibly worth making a specific answer for client refused
//                        // could be interesting for analytics.
//                        Logger.log(String.format("<R> Refused client mod: %s", Paths.get(path)));
//                        respond(EBinaryAnswer.YES);
//                        processedFiles.put(Paths.get(path).toString(), EFileProccessingStatus.REFUSED);
//                        afterEachFile.f();
//                        continue;
//                    } else {
//                        // TODO make the destination server configurable
//                        path = path.replaceFirst(FileManager.clientOnlyFilesDirectoryName, "mods");
//                    }
//                }
//
//                Path clientFile = Paths.get(path);
//
//                // Has the client set this file to be ignored, clients can refuse to accept files
//                // from servers.
//                if (IgnoredFilesMatcher.matches(clientFile)) {
//                    Logger.debug(String.format("File: %s, set to ignore by the client.", clientFile));
//                    Logger.log(String.format(
//                        "%s %s %s",
//                        RefStrings.IGNORE_TOKEN,
//                        ServerSync.strings.getString("message_client_refused_file"),
//                        clientFile
//                    ));
//                    respond(EBinaryAnswer.YES);
//                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.REFUSED);
//                    afterEachFile.f();
//                    continue;
//                }
//
//                // Does the file exist on the client?
//                //   - if it does then check its hash to see if it is the same file
//                //       - if the hash check succeeds then we already have the file, skip to the next file
//                if (Files.exists(clientFile) && hash.equals(FileHash.hashFile(clientFile))) {
//                    // Client: I have that file already!
//                    respond(EBinaryAnswer.YES);
//                    Logger.log(String.format("File up to date: %s", clientFile));
//                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.NO_WORK);
//                    afterEachFile.f();
//                    continue;
//                }
//
//                // Client: I don't have that file!
//                respond(EBinaryAnswer.NO);
//                Logger.debug(String.format("Don't have file: %s", clientFile));
//
//                // Server: Here is the file.
//                long fileSize = ois.readLong();
//                if (updateFile(clientFile, fileSize)) {
//                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.SUCCESS);
//                    didSyncFiles = true;
//                } else {
//                    Logger.error(String.format("Failed to update file: %s", clientFile));
//                    processedFiles.put(clientFile.toString(), EFileProccessingStatus.FAILED);
//                }
//                afterEachFile.f();
//            }
//        } catch (IOException e) {
//            Logger.error("Critical failure during sync process!");
//            Logger.debug(e);
//            return null;
//        }
//
//        if (!didSyncFiles) {
//            Logger.log("All files match the server.");
//        }
//
//        return processedFiles;
//    }
//
//    public boolean updateFile(Path path, long size) {
//        FileProgress GUIUpdater = new FileProgress();
//
//        try {
//            Files.createDirectories(path.getParent());
//        } catch (IOException e) {
//            Logger.debug("Could not create parent directories for: " + path.toString());
//            Logger.debug(e);
//        }
//
//        if (size == 0 && Files.notExists(path)) {
//            Logger.debug(String.format("Found a 0 byte file, writing an empty file to: %s", path));
//            try {
//                Files.createDirectories(path.getParent());
//                Files.createFile(path);
//                return true;
//            } catch (IOException e) {
//                Logger.debug("Failed to write 0 size file.");
//                return false;
//            }
//        }
//
//        if (Files.exists(path)) {
//            try {
//                Files.delete(path);
//                Files.createFile(path);
//
//                // Zero size files do not need to continue
//                // The server will not send any bytes through the socket
//                if (size == 0) {
//                    return true;
//                }
//            } catch (IOException e) {
//                Logger.debug("Failed to delete file: " + path.getFileName().toString());
//                Logger.debug(e);
//                return false;
//            }
//        }
//
//        try {
//            Logger.debug("Attempting to write file (" + path.toString() + ")");
//            OutputStream wr = Files.newOutputStream(path);
//
//            byte[] outBuffer = new byte[clientSocket.getReceiveBufferSize()];
//
//            int bytesReceived;
//            long totalBytesReceived = 0L;
//            while ((bytesReceived = ois.read(outBuffer)) > 0) {
//                totalBytesReceived += bytesReceived;
//
//                wr.write(outBuffer, 0, bytesReceived);
//                GUIUpdater.updateProgress(
//                    (int) Math.ceil((float) totalBytesReceived / size * 100),
//                    path.getFileName().toString()
//                );
//
//                if (totalBytesReceived == size) {
//                    break;
//                }
//            }
//            wr.flush();
//            wr.close();
//
//            GUIUpdater.fileFinished();
//            Logger.debug("Finished writing file" + path.toString());
//        } catch (FileNotFoundException e) {
//            Logger.debug("Failed to create file (" + path.toString() + "): " + e.getMessage());
//            Logger.debug(e);
//            return false;
//        } catch (IOException e) {
//            Logger.debug(e);
//            return false;
//        }
//
//        Logger.log(
//            String.format(
//                "%s %s %s",
//                RefStrings.UPDATE_TOKEN,
//                ServerSync.strings.getString("update_success"),
//                path.toString()
//            ));
//        return true;
//    }
//
//    /**
//     * Terminates the listener thread on the server for this client
//     */
//    private void exit() {
//        if (info == null) {
//            // NO server messages set up, server must have not connected at this point
//            return;
//        }
//        String message = info.messages.get(EServerMessage.EXIT);
//        Logger.debug(ServerSync.strings.getString("debug_server_exit"));
//
//        try {
//            oos.writeObject(message);
//            oos.flush();
//        } catch (IOException e) {
//            Logger.debug("Failed to write object (" + message + ") to client output stream");
//            Logger.debug(e);
//        }
//    }
//
//    /**
//     * Releases resources related to this server instance, MUST call this when
//     * interaction is finished if a server is opened
//     *
//     * @return true if client successfully closes all connections
//     */
//    public boolean close() {
//        exit();
//        Logger.debug(ServerSync.strings.getString("debug_server_close"));
//        try {
//            if (clientSocket != null && !clientSocket.isClosed())
//                clientSocket.close();
//        } catch (IOException e) {
//            Logger.debug("Failed to close client socket: " + e.getMessage());
//            return false;
//        }
//        Logger.debug(ServerSync.strings.getString("debug_server_close_success"));
//        return true;
//    }
//
//    private void requestSync(int mode) {
//        // TODO implement a response to this request
//
//        if (mode == 0) {
//            String message = info.messages.get(EServerMessage.SYNC_FILES);
//            try {
//                oos.writeObject(message);
//                oos.flush();
//            } catch (IOException e) {
//                Logger.debug("Failed to send coms message to server: SYNC_FILES");
//                Logger.debug(e);
//            }
//        }
//
//        if (mode == 1) {
//            // TODO sync via a single transmission of a manifest file
//            Logger.error("Mode 1 not implemented yet!");
//        }
//    }
//
//    private void respond(EBinaryAnswer answer) throws IOException {
//        oos.writeInt(answer.getValue());
//        oos.flush();
//    }
//
//    private boolean isClientOnlyFile(String path) {
//        return path.startsWith(FileManager.clientOnlyFilesDirectoryName);
//    }
}
