package com.superzanti.serversync.util;

import com.superzanti.serversync.files.PathBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Observable;

public class Log extends Observable {

    private final String fileName;
    private final StringBuilder logContent = new StringBuilder(1000);
    public final StringBuilder userFacingLog = new StringBuilder(1000);
    boolean shouldOutputToSystem = false;

    private static final String EXT = ".log";

    Log(String fileName) {
        this.fileName = fileName;

        Runtime.getRuntime().addShutdownHook(new Thread(this::saveLog));
    }

    /**
     * Shortcut method for adding to logs string builder
     * @param tag The identifier for this log entry
     * @param message The content of the log entry
     * @return The logging instance for further logging actions
     */
    public Log add(String tag, String message) {
        if (tag.equals(Logger.TAG_LOG) || tag.equals(Logger.TAG_ERROR)) {
            this.userFacingLog.append(message);
            this.userFacingLog.append("\r\n");
        }
        if (shouldOutputToSystem) {
            System.out.println(tag + message);
        }
        this.logContent.append(tag).append(message);
        this.logContent.append("\r\n");
        this.setChanged();
        this.notifyObservers();
        return this;
    }

    void saveLog() {
        Thread saveT = new Thread(new Runnable() {
            final Path logsDir = new PathBuilder().add("logs").toPath();
            final Path log = logsDir.resolve(fileName + EXT);

            @Override
            public void run() {
                try {
                    Files.createDirectories(logsDir);
                    Files.write(log, logContent.toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        saveT.setName("Log Saving");
        saveT.start();
        // May need seperate thread?
    }
}
