package com.superzanti.serversync.communication;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.client.Server;
import com.superzanti.serversync.util.ServerSyncLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public class SyncFileOutputStream {
    private final Server server;
    private final Path outputFile;
    private final long size;

    public SyncFileOutputStream(Server server, long size, Path outputFile) {
        this.server = server;
        this.size = size;
        this.outputFile = outputFile;
    }

    public boolean write(Consumer<Double> onProgress) {
        try {
            Files.createDirectories(outputFile.getParent());
        } catch (IOException e) {
            ServerSyncLogger.error("Failed to create parent directories for: " + outputFile.toString());
            ServerSyncLogger.debug(e);
        }

        if (Files.notExists(outputFile)) {
            try {
                Files.createFile(outputFile);
            } catch (IOException e) {
                ServerSyncLogger.error("Failed to create new file for: " + outputFile.toString());
                ServerSyncLogger.debug(e);
                return false;
            }
        }

        if (size == 0) {
            ServerSyncLogger.debug(String.format("Found a 0 byte file, writing an empty file to: %s", outputFile));
            return true;
        }

        try {
            ServerSyncLogger.debug("Attempting to write file (" + outputFile.toString() + ")");
            OutputStream wr = Files.newOutputStream(outputFile, StandardOpenOption.TRUNCATE_EXISTING);

            byte[] outBuffer = new byte[server.clientSocket.getReceiveBufferSize()];

            int bytesReceived;
            long totalBytesReceived = 0L;
            while ((bytesReceived = server.input.read(outBuffer)) > 0) {
                totalBytesReceived += bytesReceived;

                wr.write(outBuffer, 0, bytesReceived);
                // Not terribly worried about conversion loss
                onProgress.accept((double) totalBytesReceived / size);

                if (totalBytesReceived == size) {
                    break;
                }
            }
            wr.flush();
            wr.close();

            ServerSyncLogger.debug("Finished writing file" + outputFile.toString());
        } catch (IOException e) {
            ServerSyncLogger.error("Failed to transfer data for: " + outputFile.toString());
            ServerSyncLogger.debug(e);
            return false;
        }

        ServerSyncLogger.log(
            String.format(
                "%s %s %s",
                RefStrings.UPDATE_TOKEN,
                ServerSync.strings.getString("update_success"),
                outputFile.toString()
            ));
        return true;
    }
}
