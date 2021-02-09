package com.superzanti.serversync.util;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.files.PathBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class LoggerInstance {
    public java.util.logging.Logger javaLogger;
    final Path logsDir = new PathBuilder().add("logs").toPath();
    String ctx = "";
    final String onelinerLogFormat = "[%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS][ServerSync] %4$s: %5$s%6$s%n";

    public LoggerInstance(String context) {
        setupOnelinerLogFormat();
        this.ctx = context;
        javaLogger = java.util.logging.Logger.getLogger(ServerSync.APPLICATION_TITLE + "-" + context);

        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileHandler fh = getNewFileHander(context);
            if (fh != null) javaLogger.addHandler(fh);
        } catch (SecurityException ex) {
            error(ex.getMessage());
        }
    }

    private void setupOnelinerLogFormat(){
        String existingFormat = System.getProperty("java.util.logging.SimpleFormatter.format");
        if (existingFormat == null){
            System.setProperty("java.util.logging.SimpleFormatter.format", onelinerLogFormat);
        }
    }

    private FileHandler getNewFileHander(String context){
        String logFilePath = logsDir.resolve("serversync-" + context + ".log").toAbsolutePath().toString();
        FileHandler fh = null;
        try {
            fh = new FileHandler(logFilePath);
            fh.setFormatter(new SimpleFormatter());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fh;
    }

    public void log(String s) {
        javaLogger.info(s);
    }

    public void error(String s) {
        javaLogger.severe(s);
    }

    public void debug(String s) {
        javaLogger.info(s);
    }

    public void debug(Exception e) {
        debug(Arrays.toString(e.getStackTrace()));
    }

    public void outputError(Object object) {
        debug("Failed to write object (" + object + ") to output stream");
    }

    public void inputError(Object object) {
        debug("Failed to read object from input stream: " + object);
    }
}
