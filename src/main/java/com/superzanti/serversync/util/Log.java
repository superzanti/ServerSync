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
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				saveLog();
			}
		}));
	}
	
	public StringBuilder getLogBuilder() {
		return this.logContent;
	}
	
	public String getReadableContent() {
		return this.logContent.toString();
	}
	
	/**
	 * Shortcut method for adding to logs string builder
	 * @param s
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
	
	public Log addToConsole(String s) {
		//TODO add levels later
		this.logContent.append(s);
		this.logContent.append("\r\n");
		System.out.println(s);
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
		saveT.run();
		// May need seperate thread?
		return true;
	}
}
