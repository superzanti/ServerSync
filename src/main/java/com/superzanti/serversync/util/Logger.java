package com.superzanti.serversync.util;

import java.util.Arrays;

/**
 * Manager for serversyncs logs
 * @author Rheimus
 *
 */
public class Logger {
	public static final String FULL_LOG = "full";
	public static final String USER_LOG = "user";
	public static final String TAG_DEBUG = "DEBUG:";
	public static final String TAG_ERROR = "ERROR:";
	public static final String TAG_LOG = "LOG:";
	
	private static Log LOG;

	public Logger(String context) {
		LOG = new Log("serversync-" + context);
	}
	
	public static Log getLog() {
		return LOG;
	}
	
	public static void setSystemOutput(boolean output) {
		LOG.shouldOutputToSystem = output;
	}

	public static boolean save() {
		LOG.saveLog();
		return true;
	}
	
	public static void log(String s) {
		LOG.add(TAG_LOG, s);
		LOG.saveLog();
	}
	
	public static void error(String s) {
		LOG.add(TAG_ERROR, s);
		LOG.saveLog();
	}
	
	public static void debug(Exception e) {
		debug(Arrays.toString(e.getStackTrace()));
	}
	
	public static void debug(String s) {
		LOG.add(TAG_DEBUG, s);
		LOG.saveLog();
	}
	
	public static void outputError(Object object) {
		debug("Failed to write object (" + object + ") to output stream");
	}
	
	public static void inputError(Object object) {
		debug("Failed to read object from input stream: " + object);
	}
}
