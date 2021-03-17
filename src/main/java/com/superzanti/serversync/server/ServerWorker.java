package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.files.FileManifest;
import com.superzanti.serversync.files.FileEntry;
import com.superzanti.serversync.communication.response.ServerInfo;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.files.PathBuilder;
import com.superzanti.serversync.util.LoggerInstance;
import com.superzanti.serversync.util.PrettyCollection;
import com.superzanti.serversync.util.enums.EBinaryAnswer;
import com.superzanti.serversync.util.enums.EServerMessage;
import com.superzanti.serversync.util.errors.UnknownMessageError;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This worker handles requests from the client continuously until told to exit
 * using SECURE_EXIT These workers are assigned per socket connection i.e. one
 * per client
 *
 * @author superzanti
 */
public class ServerWorker implements Runnable {
    private static final int DEFAULT_CLIENT_TIMEOUT_MS = 60000 * 2; // 2 minutes
    private static final int FILE_SYNC_CLIENT_TIMEOUT_MS = 60000 * 20; // 20 minutes

    private final Socket clientSocket;
    private InputStream is;
    private OutputStream os;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private final List<String> messages;
    private final FileManifest manifest;

    private Timer timeout;
    private TimerTask timeoutTask;

    private final LoggerInstance clientLogger;

    ServerWorker(
        Socket socket,
        List<String> messages,
        Timer timeoutScheduler,
        FileManifest manifest
    ) {
        clientLogger = new LoggerInstance(String.format(
            "server-connection-from-%s",
            socket.getInetAddress().toString().replaceAll("[^A-Za-z0-9]", "-")
        ));
        this.manifest = manifest;
        this.messages = messages;
        clientSocket = socket;
        timeout = timeoutScheduler;
        Date clientConnectionStarted = new Date();
        DateFormat dateFormatter = DateFormat.getDateTimeInstance();

        clientLogger.log("Connection established with " + clientSocket + dateFormatter.format(clientConnectionStarted));
    }

    @Override
    public void run() {
        try {
            is = clientSocket.getInputStream();
            os = clientSocket.getOutputStream();
            ois = new ObjectInputStream(is);
            oos = new ObjectOutputStream(os);
        } catch (IOException e) {
            clientLogger.log("Failed to create client streams");
            Logger.error(String.format("Error in client setup: %s", clientSocket.getInetAddress()));
            Logger.debug(e);
        }

        while (!clientSocket.isClosed()) {
            String message = null;
            try {
                setTimeout(ServerWorker.DEFAULT_CLIENT_TIMEOUT_MS);
                message = ois.readUTF();
                clientLogger.log(String.format(
                    "Received message: %s, from client: %s",
                    message,
                    clientSocket.getInetAddress()
                ));
                Logger.debug(String.format("Received message: %s", message));
            } catch (SocketException e) {
                // Client timed out
                Logger.error(String.format("Client: %s, timed out", clientSocket.getInetAddress()));
                break;
            } catch (IOException e) {
                clientLogger.debug(e);
            }

            if (message == null) {
                clientLogger.debug("Received null message, this should not happen.");
                break;
            }

            try {
                // <---->
                // always called first
                if (message.equals(ServerSync.GET_SERVER_INFO)) {
                    clientLogger.log("Sending server information");
                    oos.writeObject(new ServerInfo(messages, SyncConfig.getConfig().SYNC_MODE));
                    oos.flush();
                    continue;
                }

                // <---->
                // fallback if I don't know what this message is
                if (!messages.contains(message)) {
                    try {
                        clientLogger.log("Unknown message received from: " + clientSocket.getInetAddress());
                        oos.writeObject(new UnknownMessageError(message));
                        oos.flush();
                    } catch (IOException e) {
                        clientLogger.log("Failed to write error to client " + clientSocket);
                        clientLogger.debug(e);
                    }

                    // There should not be unknown messages being sent to ServerSync, disconnect from the client.
                    break;
                }

                if (matchMessage(message, EServerMessage.GET_MANIFEST)) {
                    oos.writeObject(manifest);
                    oos.flush();
                    continue;
                }

                // READ FROM CLIENT <entry>: The file I want
                // SEND TO CLIENT <boolean>: If the file exists on the server
                // STREAM TO CLIENT <the file>
                /*
                 * Individual updating of singular files, this is less efficient than using a manifest of files
                 * that can be packaged and sent all at once.
                 */
                if (matchMessage(message, EServerMessage.UPDATE_FILE)) {
                    try {
                        FileEntry entry = (FileEntry) ois.readObject();
                        Path theFile = new PathBuilder().add(entry.path).toPath();
                        if (Files.exists(theFile)) {
                            oos.writeBoolean(true);
                            oos.flush();
                            setTimeout(ServerWorker.FILE_SYNC_CLIENT_TIMEOUT_MS);
                            transferFile(theFile);
                        } else {
                            oos.writeBoolean(false);
                        }
                        oos.flush();
                    } catch (ClassNotFoundException e) {
                        clientLogger.error("Failed to parse entry from client");
                        clientLogger.debug(e);
                        oos.writeBoolean(false);
                        oos.flush();
                    }
                    continue;
                }

                // <---->
                // the actual file sync
                // @Deprecated - This sync mode is no longer used or supported
                if (matchMessage(message, EServerMessage.SYNC_FILES)) {
                    // Server: Do you have this file?
                    // - String: path
                    // - String: hash
                    // Client: yes | no
                    // -- (yes) - skip to next file
                    // -- (no) - send filesize -> send file
                    if (manifest.files.size() > 0) {
                        for (FileEntry entry : manifest.files) {
                            try {
                                Path relative = Paths.get(entry.path);
                                Path serverPath = ServerSync.rootDir.resolve(relative);

                                clientLogger.debug(String.format("Asking client if the have file: %s", entry.path));
                                oos.writeBoolean(true); // There are files left
                                oos.writeUTF(relative.toString()); // The path
                                oos.writeUTF(entry.hash); // The hash
                                oos.flush();


                                // Client: Nope, don't have it joe!
                                if (EBinaryAnswer.NO.getValue() == ois.readInt()) {
                                    clientLogger.debug("Client said they don't have the file");
                                    setTimeout(ServerWorker.FILE_SYNC_CLIENT_TIMEOUT_MS);
                                    transferFile(serverPath);
                                } else {
                                    clientLogger.debug("Client said they have the file already");
                                    setTimeout(ServerWorker.DEFAULT_CLIENT_TIMEOUT_MS);
                                }
                            } catch (IOException ex) {
                                clientLogger.debug(ex);
                                clientLogger
                                    .log(String.format(
                                        "Encountered error during sync with %s, killing sync process",
                                        clientSocket.getInetAddress()
                                    ));
                                break;
                            }
                        }

                        clientLogger.debug("Finished sync");
                        oos.writeBoolean(false); // No files left
                    } else {
                        clientLogger.debug("No files on the server?");
                        oos.writeBoolean(false); // No files at all?
                    }
                    oos.flush();
                    continue;
                }

                // <---->
                // the directories that I am managing / sync'ing
                // needed by the client to know what it should delete
                if (matchMessage(message, EServerMessage.GET_MANAGED_DIRECTORIES)) {
                    clientLogger.debug(PrettyCollection.get(manifest.directories));
                    oos.writeObject(manifest.directories);
                    oos.flush();
                    continue;
                }

                // <---->
                // how many files are managed by the server?
                if (matchMessage(message, EServerMessage.GET_NUMBER_OF_MANAGED_FILES)) {
                    oos.writeInt(manifest.files.size());
                    oos.flush();
                    continue;
                }
            } catch (SocketException e) {
                clientLogger.log("Client " + clientSocket + " closed by timeout");
                break;
            } catch (IOException e) {
                clientLogger.log("Failed to write to " + clientSocket + " client stream");
                e.printStackTrace();
                break;
            }

            // <---->
            if (matchMessage(message, EServerMessage.EXIT)) {
                clientLogger.log(String.format(
                    "Client requested exit, sync process complete for: %s",
                    clientSocket.getInetAddress()
                ));
                break;
            }

            String error = String.format("Unhandled message type: %s", message);
            clientLogger.log(error);
            Logger.error(String.format("%s from client: %s", error, clientSocket.getInetAddress()));
            break;
        }

        clientLogger.log("Closing connection with: " + clientSocket);
        teardown();
    }

    private void transferFile(Path file) throws IOException {
        // Not checking if the file exists as this is coming from a list of
        // files that we already know exist.

        clientLogger.log("Writing " + file.toString() + " to client " + clientSocket.getInetAddress() + "...");

        // -- Size, for client GUI progress tracking
        long size = 0L;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            clientLogger.debug(e);
            String error = String.format(ServerSync.strings.getString("server_message_file_missing"), file);
            clientLogger.error(error);
            Logger.error(error);
        } catch (SecurityException se) {
            clientLogger.debug(se);
            clientLogger
                .error(String.format(ServerSync.strings.getString("server_message_file_permission_denied"), file));
        }
        clientLogger.debug(String.format("File size is: %d", size));
        oos.writeLong(size);
        oos.flush();
        // --

        if (size > 0) {
            int bytesRead;
            byte[] buffer = new byte[SyncConfig.getConfig().BUFFER_SIZE];

            try (BufferedInputStream fis = new BufferedInputStream(Files.newInputStream(file), SyncConfig.getConfig().BUFFER_SIZE)) {
                while ((bytesRead = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                clientLogger.debug(String.format("Failed to write file: %s", file));
                clientLogger.debug(e);
            } finally {
                os.flush();
            }
        }

        clientLogger.log(String.format(
            "Finished writing: %s, to client: %s",
            file.toString(),
            clientSocket.getInetAddress()
        ));
    }

    private boolean matchMessage(String incomingMessage, EServerMessage message) {
        return incomingMessage.equals(message.toString());
    }

    private void clearTimeout() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeout.purge();
        }
    }

    private void setTimeout(int durationMs) {
        clearTimeout();
        timeoutTask = new ServerTimeout(this);
        timeout.schedule(timeoutTask, durationMs);
        clientLogger.debug(String.format(
            "Reset timeout for client: %s, with a timeout of: %s",
            clientSocket.getInetAddress(),
            durationMs
        ));
    }

    private void teardown() {
        try {
            clearTimeout();
            timeout = null;

            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void timeoutShutdown() {
        try {
            clientLogger.log("Client connection timed out, closing " + clientSocket);

            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
