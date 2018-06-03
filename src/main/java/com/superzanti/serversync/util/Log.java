package com.superzanti.serversync.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;

public class Log extends Observable {
	
	public String fileName;
	public StringBuilder logContent = new StringBuilder(1000);
	public StringBuilder userFacingLog = new StringBuilder(1000);
	public boolean shouldOutputToSystem = false;
	
	private static final String EXT = ".log";
	
	public Log(String fileName) {
		this.fileName = fileName;
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				saveLog();
			}
		}));
	}
	
	public void clearUserFacingLog() {
		userFacingLog.setLength(0);
	}
	
	/**
	 * Shortcut method for adding to logs string builder
	 * @param s
	 */
	public Log add(String tag, String message) {
		if (tag.equals(Logger.TAG_LOG) || tag.equals(Logger.TAG_ERROR)) {			
			this.userFacingLog.append(message);
			this.userFacingLog.append("\r\n");
		}
		if (shouldOutputToSystem) {
			System.out.println(tag + message);
		}
		this.logContent.append(message);
		this.logContent.append("\r\n");
		this.setChanged();
		this.notifyObservers();
		return this;
	}
	
	public boolean saveLog() {
		
		Thread saveT = new Thread(new Runnable(){
			Path logsDir = Paths.get("logs");
			Path log = logsDir.resolve(fileName + EXT);
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
		saveT.run();
		// May need seperate thread?
		return true;
	}
}
