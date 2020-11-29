package com.superzanti.serversync.util;

import java.util.Arrays;

/**
 * Non static manager for serversyncs logs
 * @author Rheimus
 *
 */
public class LoggerNG {
    public static final String FULL_LOG = "full";
    public static final String USER_LOG = "user";
    private static final String TAG_DEBUG = "DEBUG:";
    private static final String TAG_ERROR = "ERROR:";
    private static final String TAG_LOG = "LOG:";

    private final Log LOG;

    public LoggerNG(String context) {
        LOG = new Log("serversync-" + context);
    }

    public Log getLog() {
        return LOG;
    }

    public void setSystemOutput(boolean output) {
        LOG.shouldOutputToSystem = output;
    }

    public boolean save() {
        LOG.saveLog();
        return true;
    }

    public void log(String s) {
        LOG.add(TAG_LOG, s);
        LOG.saveLog();
    }

    public void error(String s) {
        LOG.add(TAG_ERROR, s);
        LOG.saveLog();
    }

    public void debug(Exception e) {
        debug(e.getMessage());
        debug(Arrays.toString(e.getStackTrace()));
    }

    public void debug(String s) {
        LOG.add(TAG_DEBUG, s);
        LOG.saveLog();
    }

    public void outputError(Object object) {
        debug("Failed to write object (" + object + ") to output stream");
    }

    public void inputError(Object object) {
        debug("Failed to read object from input stream: " + object);
    }
}
