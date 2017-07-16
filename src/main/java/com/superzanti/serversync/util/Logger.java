package com.superzanti.serversync.util;

import com.superzanti.serversync.gui.Console;

/**
 * Manager for serversyncs logs
 * @author Rheimus
 *
 */
public class Logger {
	private static Log fullLog = new Log("serversync-detailed");
	private static Log userLog = new Log("serversync-ui");
	public static final String FULL_LOG = "full";
	public static final String USER_LOG = "user";
	public static final String TAG_DEBUG = "DEBUG:";
	public static final String TAG_ERROR = "ERROR:";
	public static final String TAG_LOG = "LOG:";
	private static Console console = new Console();

	public boolean save() {
		fullLog.saveLog();
		return true;
	}
	
	public static void log(String s) {
		fullLog.add(String.format("%s %s", TAG_LOG, s));
		fullLog.saveLog();
		userLog.add(s);
		console.updateText(userLog.getReadableContent());
	}
	
	public static void error(String s) {
		fullLog.add(String.format("%s %s", TAG_ERROR, s));
		fullLog.saveLog();
		userLog.add(s);
		console.updateText(userLog.getReadableContent());
	}
	
	public static void debug(Exception e) {
		fullLog.add(e.getStackTrace().toString());
		fullLog.saveLog();
	}
	
	public static void debug(String s) {
		fullLog.add(String.format("%s %s", TAG_DEBUG, s));
		fullLog.saveLog();
	}
	
	public static void outputError(Object object) {
		debug("Failed to write object (" + object + ") to output stream");
	}
	
	public static void inputError(Object object) {
		debug("Failed to read object from input stream: " + object);
	}
}
