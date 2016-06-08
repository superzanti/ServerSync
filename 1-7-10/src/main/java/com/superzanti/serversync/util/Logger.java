package com.superzanti.serversync.util;

import java.lang.reflect.InvocationTargetException;

import com.superzanti.serversync.gui.Console;

/**
 * Manager for serversyncs logs
 * @author Rheimus
 *
 */
public class Logger {
	private Log fullLog;
	private Log userLog;
	public static final String FULL_LOG = "full";
	public static final String USER_LOG = "user";
	private Console console;
	
	public Logger() {
		fullLog = new Log("serversync-detailed");
		userLog = new Log("serversync-ui");
		console = new Console();
	}
	
	public boolean save() {
		fullLog.saveLog();
		return true;
	}
	
	/**
	 * Shorthand for upadteLogs(string,true), updates GUI console text as well
	 * @param s - Text to update logs with
	 * 
	 */
	public void updateLogs(String s) {
		updateLogs(s,true);
	}
	
	/**
	 * Update a specific log
	 * @param s - Text to update log with
	 * @param logToUpdate - Use Logger constants
	 */
	public void updateLogs(String s, String logToUpdate) {
		if (logToUpdate.equals(FULL_LOG))
			fullLog.add(s);
		if (logToUpdate.equals(USER_LOG))
			userLog.add(s);
	}
	
	public void updateLogs(String s, boolean update) {
		fullLog.add(s);
		userLog.add(s);

		if (update) {
			console.updateText(userLog.getReadableContent());
		}
	}
}
