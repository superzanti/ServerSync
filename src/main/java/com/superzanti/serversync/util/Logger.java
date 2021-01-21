package com.superzanti.serversync.util;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.files.PathBuilder;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.*;

/**
 * Wrapper for serversyncs logs
 *
 * @author Rheimus, Alfuken
 */
public class Logger {
    public static java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ServerSync.APPLICATION_TITLE);
    public static SimpleFormatter formatter = new SimpleFormatter();
    FileHandler logFileHandler;

    final Path logsDir = new PathBuilder().add("logs").toPath();

    public Logger(String side) {
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String logFilePath = logsDir.resolve("serversync-" + side + ".log").toAbsolutePath().toString();
        try {
            logFileHandler = new FileHandler(logFilePath);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        LOG.addHandler(logFileHandler);
        logFileHandler.setFormatter(formatter);

    }

    public static java.util.logging.Logger getLog() {
        return LOG;
    }

    public static void setSystemOutput(boolean output) {
        // enable/disable System.out logging
        LOG.setUseParentHandlers(output);
    }

    public static void log(String s) {
        LOG.info(s);
    }

    public static void error(String s) {
        LOG.severe(s);
    }

    public static void debug(Exception e) {
        debug(Arrays.toString(e.getStackTrace()));
    }

    public static void debug(String s) {
        LOG.info(s);
    }

    public static void outputError(Object object) {
        debug("Failed to write object (" + object + ") to output stream");
    }

    public static void inputError(Object object) {
        debug("Failed to read object from input stream: " + object);
    }
}
