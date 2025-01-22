package com.superzanti.serversync.communication;

import com.superzanti.serversync.RefStrings;
import com.superzanti.serversync.ServerSyncUtility;
import com.superzanti.serversync.client.Server;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.util.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
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
            Logger.error("Failed to create parent directories for: " + outputFile.toString());
            Logger.debug(e);
        }

        if (Files.notExists(outputFile)) {
            try {
                Files.createFile(outputFile);
            } catch (IOException e) {
                Logger.error("Failed to create new file for: " + outputFile.toString());
                Logger.debug(e);
                return false;
            }
        }

        if (size == 0) {
            Logger.debug(String.format("Found a 0 byte file, writing an empty file to: %s", outputFile));
            return true;
        }

        try {
            Logger.debug(String.format(
                "Attempting to write file '%s' with total size of %s bytes...",
                outputFile.toString(), size
            ));
            BufferedOutputStream wr = new BufferedOutputStream(
                Files.newOutputStream(outputFile, StandardOpenOption.TRUNCATE_EXISTING),
                SyncConfig.getConfig().BUFFER_SIZE
            );

            byte[] outBuffer = new byte[SyncConfig.getConfig().BUFFER_SIZE];

            int bytesReceived;
            float mebibyte = 1024F * 1024F;
            float sizeMiB = Math.round(size / mebibyte * 10) / 10F;
            long totalBytesReceived = 0L;
            while ((bytesReceived = server.is.read(outBuffer)) > 0) {
                totalBytesReceived += bytesReceived;

                wr.write(outBuffer, 0, bytesReceived);
                // Not terribly worried about conversion loss
                onProgress.accept((double) totalBytesReceived / size);

                if (size > mebibyte && totalBytesReceived % mebibyte == 0) {
                    Logger.debug(
                        String.format("Progress: %s / %s MiB", Math.round(totalBytesReceived / mebibyte), sizeMiB));
                }

                if (totalBytesReceived == size) {
                    break;
                }
            }
            wr.flush();
            wr.close();

            Logger.debug("Finished writing file" + outputFile.toString());
        } catch (IOException e) {
            Logger.error("Failed to transfer data for: " + outputFile.toString());
            Logger.debug(e);
            return false;
        }

        Logger.log(
            String.format(
                    "%s %s %s",
                    RefStrings.UPDATE_TOKEN,
                    ServerSyncUtility.strings.getString("update_success"),
                    outputFile.toString()
            ));
        return true;
    }
}
