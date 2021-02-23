package com.superzanti.serversync.util;

import com.superzanti.serversync.GUIJavaFX.PaneLogs;
import com.superzanti.serversync.ServerSync;
import javafx.application.Platform;

import java.util.Arrays;
import java.util.logging.*;

/**
 * Wrapper for serversyncs logs
 *
 * @author Rheimus, Alfuken
 */
public class Logger {
    public static LoggerInstance instance = null;
    private static final Object mutex = new Object();

    public static String getContext(){
        return ServerSync.MODE == null ? "undefined" : ServerSync.MODE.toString();
    }

    public static LoggerInstance getInstance()
    {
        LoggerInstance result = instance;
        if (result == null)
        {
            //synchronized block to remove overhead
            synchronized (mutex)
            {
                result = instance;
                if(result == null)
                {
                    // if instance is null, initialize
                    instance = result = new LoggerInstance(getContext());
                }
            }
        }
        return result;
    }

    public static synchronized void instantiate()
    {
        instantiate(getContext());
    }

    public static void instantiate(String context){
        instance = new LoggerInstance(context);
    }

    public static synchronized void setSystemOutput(boolean output) {
        // enable/disable System.out logging
//        getInstance().javaLogger.setUseParentHandlers(output);
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

    public static synchronized void attachOutputToLogsPane(PaneLogs paneLogs){
        getInstance().javaLogger.addHandler(new Handler() {
            final SimpleFormatter fmt = new SimpleFormatter();

            @Override
            public void publish(LogRecord record) {
                Platform.runLater(() -> paneLogs.updateLogsArea(fmt.format(record)));
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
    }

}
