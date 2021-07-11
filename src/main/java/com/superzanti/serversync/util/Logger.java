package com.superzanti.serversync.util;

import com.superzanti.serversync.ServerSync;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

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

    public static synchronized void attachUIHandler(Handler handler) {
        if (uiHandler != null) {
            uiHandler.close();
            getInstance().javaLogger.removeHandler(uiHandler);
        }
        uiHandler = handler;
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
