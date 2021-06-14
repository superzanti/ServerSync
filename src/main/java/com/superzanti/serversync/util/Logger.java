package com.superzanti.serversync.util;

import com.superzanti.serversync.GUIJavaFX.PaneLogs;
import com.superzanti.serversync.ServerSync;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Wrapper for serversyncs logs
 *
 * @author Rheimus, Alfuken
 */
public class Logger {
    public static LoggerInstance instance = null;
    private static final Object mutex = new Object();
    private static Handler uiHandler;

    // Probably a heinous implementation of debounce but whatever
    private static final long dbTimeMS = 2000L;
    private static final ScheduledExecutorService dbRunner = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> waitingFlush;

    public static String getContext() {
        return ServerSync.MODE == null ? "undefined" : ServerSync.MODE.toString();
    }

    public static LoggerInstance getInstance() {
        LoggerInstance result = instance;
        if (result == null) {
            //synchronized block to remove overhead
            synchronized (mutex) {
                result = instance;
                if (result == null) {
                    // if instance is null, initialize
                    instance = result = new LoggerInstance(getContext());
                }
            }
        }
        return result;
    }

    public static synchronized void instantiate() {
        instantiate(getContext());
    }

    public static void instantiate(String context) {
        instance = new LoggerInstance(context);
    }

    public static synchronized void setSystemOutput(boolean output) {
        // enable/disable System.out logging
        getInstance().javaLogger.setUseParentHandlers(output);
    }

    public static synchronized void log(String s) {
        getInstance().log(s);
    }

    public static synchronized void error(String s) {
        getInstance().error(s);
    }

    public static synchronized void debug(String s) {
        getInstance().debug(s);
    }

    public static synchronized void debug(Exception e) {
        getInstance().debug(Arrays.toString(e.getStackTrace()));
    }

    public static synchronized void outputError(Object object) {
        getInstance().debug("Failed to write object (" + object + ") to output stream");
    }

    public static synchronized void inputError(Object object) {
        getInstance().debug("Failed to read object from input stream: " + object);
    }

    public static synchronized void attachOutputToLogsPane(PaneLogs paneLogs) {
        final StringProperty records = new SimpleStringProperty();
        paneLogs.getText().textProperty().bind(records);
        uiHandler = new Handler() {
            final SimpleFormatter fmt = new SimpleFormatter();
            final StringBuilder r = new StringBuilder();


            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().equals(Level.INFO)) {
                    r.append(fmt.format(record));
                    Logger.flush();
                }
            }

            @Override
            public void flush() {
                records.set(r.toString());
            }

            @Override
            public void close() {
            }
        };
        getInstance().javaLogger.addHandler(uiHandler);
    }

    public static synchronized void flush() {
        if (uiHandler != null) {
            if (waitingFlush == null || waitingFlush.isDone()) {
                waitingFlush = dbRunner.schedule(() -> {
                    uiHandler.flush();
                }, dbTimeMS, TimeUnit.MILLISECONDS);
            }
        }
    }
}
