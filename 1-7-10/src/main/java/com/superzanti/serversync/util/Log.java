package com.superzanti.serversync.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Log {
	
	public String fileName;
	private static final String EXT = ".log";
	public StringBuilder logContent = new StringBuilder();
	
	public Log(String fileName) {
		this.fileName = fileName;
	}
	
	public StringBuilder getLogBuilder() {
		return this.logContent;
	}
	
	public String getReadableContent() {
		return this.logContent.toString();
	}
	
	/**
	 * Shortcut method for adding to logs string builder
	 * @param s String to append
	 * @return the log, for chaining
	 */
	public Log add(String s) {
		this.logContent.append(s);
		this.logContent.append("\r\n");
		return this;
	}
	
	public Log add(int i) {
		this.logContent.append(i);
		this.logContent.append("\r\n");
		return this;
	}
	
	public boolean saveLog() {
		
		Thread saveT = new Thread(new Runnable(){

			Path logsDir = Paths.get("../logs");
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
		saveT.run();
		// May need seperate thread?
		return true;
	}
}
