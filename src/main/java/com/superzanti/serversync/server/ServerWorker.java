package com.superzanti.serversync.server;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.util.FileHash;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.ServerTimeout;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EErrorType;
import com.superzanti.serversync.util.enums.EServerMessage;
import com.superzanti.serversync.util.errors.InvalidSyncFileException;
import com.superzanti.serversync.util.errors.MessageError;
import com.superzanti.serversync.util.errors.UnknownMessageError;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.*;

/**
 * This worker handles requests from the client continuously until told to exit
 * using SECURE_EXIT These workers are assigned per socket connection i.e. one
 * per client
 *
 * @author superzanti
 */
public class ServerWorker implements Runnable {
    public static final int DEFAULT_CLIENT_TIMEOUT_MS = 10000; // 10 seconds
    public static final int FILE_SYNC_CLIENT_TIMEOUT_MS = 300000; // 5 minutes

    private Socket clientsocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private EnumMap<EServerMessage, String> messages;

    private Date clientConnectionStarted;
    private DateFormat dateFormatter;
    private Timer timeout;
    private TimerTask timeoutTask;

    protected ServerWorker(
        Socket socket, EnumMap<EServerMessage, String> comsMessages,
        Timer timeoutScheduler
    ) {
        clientsocket = socket;
        messages = comsMessages;
        clientConnectionStarted = new Date();
        dateFormatter = DateFormat.getDateTimeInstance();
        timeout = timeoutScheduler;

        Logger.log("Connection established with " + clientsocket + dateFormatter.format(clientConnectionStarted));
        Logger.log(ServerSetup.directories.toString());
    }

    @Override
    public void run() {
        try {
            ois = new ObjectInputStream(clientsocket.getInputStream());
            oos = new ObjectOutputStream(clientsocket.getOutputStream());
            oos.flush();
        } catch (IOException e) {
            Logger.log("Failed to create client streams");
            e.printStackTrace();
        }

        while (!clientsocket.isClosed()) {
            String message = null;
            try {
                setTimeout(ServerWorker.DEFAULT_CLIENT_TIMEOUT_MS);
                message = (String) ois.readObject();
                Logger.log(
                    String.format("Received message: %s, from client: %s", message, clientsocket.getInetAddress()));
            } catch (SocketException e) {
                // Client timed out
                break;
            } catch (ClassNotFoundException | IOException e) {
                Logger.debug(e);
            }

            if (message == null) {
                continue;
            }

            try {
                if (message.equals(ServerSync.HANDSHAKE)) {
                    Logger.log("Sending coms messages");
                    oos.writeObject(messages);
                    oos.flush();
                    continue;
                }

                if (!messages.containsValue(message)) {
                    try {
                        Logger.log("Unknown message received from: " + clientsocket.getInetAddress());
                        oos.writeObject(new UnknownMessageError(message));
                        oos.flush();
                    } catch (IOException e) {
                        Logger.log("Failed to write error to client " + clientsocket);
                        Logger.debug(e);
                    }

                    // There should not be unknown messages being sent to com.superzanti.serversync.ServerSync, disconnect from the client.
                    break;
                }

                if (message.equals(messages.get(EServerMessage.FILES_NEEDING_UPDATES))) {
                    // Accept a map of <Path,FileHash> representing the clients files
                    // Path - Localized path to the file
                    // Check said list for different files or missing files
                    // Push back a list of files needing updates/sync
                    // Considerations: client only mods will need to map their path to the servers 'clientmods' dir
                    Logger.log("Checking clients files for required updates");
                    try {
                        @SuppressWarnings("unchecked cast")
                        Map<String, String> clientFiles = (Map<String, String>) ois.readObject();
                        Map<String, String> serverFiles = new HashMap<>(0);
                    } catch (ClassNotFoundException e) {
                        // Should never happen as our client and server are built from the same code
                        Logger.debug(e);
                    }
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.INFO_LAST_UPDATE))) {
                    Logger.log("Sending last updated timestamp");
                    oos.writeObject(ServerSync.CONFIG.LAST_UPDATE);
                    oos.flush();
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.UPDATE_NEEDED))) {
                    int checkLevel = ois.readInt();
                    ArrayList<String> serverFileNames = new ArrayList<>(200);
                    if (checkLevel == 3) {
                        Logger.log("Client Requested a list of all files");
                        serverFileNames.addAll(SyncFile.listModNames(ServerSetup.allFiles));
                    } else {
                        Logger.log("Client is refusing client only files, sending standard file list");
                        serverFileNames.addAll(SyncFile.listModNames(ServerSetup.standardSyncableFiles));
                    }
                    Logger.log("Sending list of syncable mods");

                    serverFileNames.removeAll(new ArrayList<>(ServerSync.CONFIG.FILE_IGNORE_LIST));

                    Logger.log("Syncable mods are: " + serverFileNames.toString());
                    oos.writeObject(serverFileNames);
                    oos.flush();
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.FILE_GET_LIST))) {
                    Logger.log("Sending servers file list to " + clientsocket);

                    oos.writeObject(ServerSetup.standardSyncableFiles);
                    oos.flush();
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.UPDATE_GET_SYNCABLE_DIRECTORIES))) {
                    Logger.log("Sending list of syncable directories: " + ServerSetup.directories);
                    oos.writeObject(ServerSetup.directories);
                    oos.flush();
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.FILE_COMPARE))) {
                    Logger.log("Comparing clients file against server " + clientsocket);
                    File theFile;
                    try {
                        theFile = (File) ois.readObject();
                        String serverChecksum = FileHash.hashString(theFile);
                        oos.writeObject(serverChecksum);
                        oos.flush();
                    } catch (ClassNotFoundException e) {
                        Logger.log("Failed to read object from client " + clientsocket);
                        e.printStackTrace();
                        oos.writeObject(new MessageError("Failed to read file", EErrorType.STREAM_ACCESS));
                        oos.flush();
                    }
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.UPDATE_GET_CLIENT_ONLY_FILES))) {
                    Logger.log("Sending client only file list");
                    oos.writeObject(ServerSetup.clientOnlyFiles);
                    oos.flush();
                    continue;
                }

                // Main file update message
                if (message.equals(messages.get(EServerMessage.UPDATE))) {
                    setTimeout(ServerWorker.FILE_SYNC_CLIENT_TIMEOUT_MS);
                    SyncFile file;
                    try {
                        // TODO update this to NIO
                        file = (SyncFile) ois.readObject();
                        File f = file.getFile();
                        Logger.log("Writing " + f + " to client " + clientsocket + "...");
                        byte[] buff = new byte[clientsocket.getSendBufferSize()];
                        int bytesRead = 0;
                        InputStream in = new FileInputStream(f);
                        if ((bytesRead = in.read(buff)) == -1) {
                            // End of file
                            oos.writeBoolean(false);
                        } else {
                            oos.writeBoolean(true);
                            oos.write(buff, 0, bytesRead);

                            while ((bytesRead = in.read(buff)) > 0) {
                                // oos.writeObject("BLOB");
                                oos.write(buff, 0, bytesRead);
                            }
                        }
                        in.close();
                        oos.flush();
                        // oos.writeObject("EOF");
                        Logger.log("Finished writing file to client " + clientsocket);

                    } catch (ClassNotFoundException e) {
                        Logger.log("Failed to read object from client " + clientsocket);
                        e.printStackTrace();
                        oos.flush();
                        oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
                        oos.flush();
                    }
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.FILE_GET_CONFIG))) {
                    Logger.log("Sending config info to client...");
                    HashMap<String, List<String>> rules = new HashMap<>();
                    rules.put("ignore", ServerSync.CONFIG.FILE_IGNORE_LIST);
                    rules.put("include", ServerSync.CONFIG.CONFIG_INCLUDE_LIST);
                    // TODO add security info in transfer
                    oos.writeObject(rules);
                    oos.flush();
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.INFO_GET_FILESIZE))) {
                    Logger.log("Writing filesize to client " + clientsocket + "...");

                    SyncFile theFile;
                    try {
                        theFile = (SyncFile) ois.readObject();
                        oos.writeLong(Files.size(theFile.getFileAsPath()));
                        oos.flush();
                    } catch (ClassNotFoundException e) {
                        Logger.log("Failed to read object from client " + clientsocket);
                        e.printStackTrace();
                        oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
                        oos.flush();
                    }
                    continue;
                }

                if (message.equals(messages.get(EServerMessage.FILE_EXISTS))) {
                    try {
                        int checkLevel = ois.readInt();
                        SyncFile clientFile = (SyncFile) ois.readObject();
                        boolean exists = false;

                        if (checkLevel == 3) {
                            for (SyncFile serverFile : ServerSetup.allFiles) {
                                try {
                                    if (serverFile.equals(clientFile)) {
                                        exists = true;
                                    }
                                } catch (InvalidSyncFileException e) {
                                    // TODO stub invalid file handling
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            for (SyncFile serverFile : ServerSetup.standardSyncableFiles) {
                                try {
                                    if (serverFile.equals(clientFile)) {
                                        exists = true;
                                    }
                                } catch (InvalidSyncFileException e) {
                                    // TODO stub invalid file handling
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (exists) {
                            System.out.println(clientFile.getFileName() + " exists");
                            oos.writeBoolean(true);
                            oos.flush();
                        } else {
                            System.out.println(clientFile.getFileName() + " does not exist");
                            oos.writeBoolean(false);
                            oos.flush();
                        }
                    } catch (ClassNotFoundException e) {
                        Logger.log("Failed to read object from client " + clientsocket);
                        e.printStackTrace();
                        oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
                        oos.flush();
                    }
                    continue;
                }
            } catch (SocketException e) {
                Logger.log("Client " + clientsocket + " closed by timeout");
                break;
            } catch (IOException e) {
                Logger.log("Failed to write to " + clientsocket + " client stream");
                e.printStackTrace();
                break;
            }

            if (message.equals(messages.get(EServerMessage.EXIT))) {
                Logger.log(String.format("Client requested exit, sync process complete for: %s",
                                         clientsocket.getInetAddress()
                ));
                break;
            }
        }

        Logger.log("Closing connection with: " + clientsocket);
        teardown();
        return; // End thread, probably not needed here as it is the terminal point of the
        // thread anyway
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
        Logger.debug(String.format(
            "Reset timeout for client: %s, with a timeout of: %s",
            clientsocket.getInetAddress(),
            durationMs
        ));
    }

    private void teardown() {
        try {
            clearTimeout();
            timeout = null;

            if (!clientsocket.isClosed()) {
                clientsocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void timeoutShutdown() {
        try {
            Logger.log("Client connection timed out, closing " + clientsocket);

            if (!clientsocket.isClosed()) {
                clientsocket.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
