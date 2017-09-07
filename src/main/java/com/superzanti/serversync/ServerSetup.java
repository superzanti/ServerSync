package com.superzanti.serversync;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;

import org.apache.commons.codec.digest.DigestUtils;

import com.superzanti.serversync.util.FileIgnoreMatcher;
import com.superzanti.serversync.util.FileIncludeMatcher;
import com.superzanti.serversync.util.Log;
import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EServerMessage;

import runme.Main;

/**
 * Sets up various server data to be passed to the specific client socket being
 * communicated with
 * 
 * @author Rheimus
 */
public class ServerSetup implements Runnable {

	public static Log serverLog = new Log("serversync-server");

	// static ServerSocket variable
	private static ServerSocket server;

	// This is what's in our folders
	public static ArrayList<SyncFile> allFiles = new ArrayList<SyncFile>(700);
	public static ArrayList<SyncFile> standardSyncableFiles = new ArrayList<SyncFile>(700);
	public static ArrayList<SyncFile> standardFiles = new ArrayList<SyncFile>(200);
	public static ArrayList<SyncFile> configFiles = new ArrayList<SyncFile>(500);
	public static ArrayList<SyncFile> clientOnlyFiles = new ArrayList<SyncFile>(20);
	public static ArrayList<String> directories = new ArrayList<String>(20);
	
	private FileIgnoreMatcher ignoredFiles = new FileIgnoreMatcher();
	private FileIncludeMatcher includedFiles = new FileIncludeMatcher();

	public static EnumMap<EServerMessage, String> generateServerMessages() {
		EnumMap<EServerMessage, String> SERVER_MESSAGES = new EnumMap<EServerMessage, String>(EServerMessage.class);

		for (EServerMessage msg : EServerMessage.values()) {
			double rng = Math.random() * 1000d;
			String hashKey = DigestUtils.sha1Hex(msg.toString() + rng);

			SERVER_MESSAGES.put(msg, hashKey);
		}

		return SERVER_MESSAGES;
	}

	public ServerSetup() {
		DateFormat dateFormatter = DateFormat.getDateInstance();
		ArrayList<Path> _list = null;
		boolean configsInDirectoryList = false;

		/* SYNC DIRECTORIES */
		for (String dir : Main.CONFIG.DIRECTORY_INCLUDE_LIST) {
			// Specific config handling later
			if (dir.equals("config") || dir.equals("clientmods")) {
				if (dir.equals("config")) {
					configsInDirectoryList = true;
					directories.add(dir);
				}
				continue;
			}
			directories.add(dir);
		}

		if (Main.CONFIG.PUSH_CLIENT_MODS) {
			// Create clientmods directory if it does not exist
			Path clientOnlyMods = Paths.get("clientmods/");
			if (!Files.exists(clientOnlyMods)) {				
				try {
					Files.createDirectories(clientOnlyMods);
					serverLog.addToConsole("clientmods directory did not exist, creating");
				} catch (IOException e) {
					serverLog.addToConsole("Could not create clientmods directory");
				}
			}
			
			_list = PathUtils.fileListDeep(Paths.get("clientmods"));
			serverLog.addToConsole("Found " + _list.size() + " files in: clientmods");

			if (_list != null) {
				_list.forEach((path) -> {
					clientOnlyFiles.add(SyncFile.ClientOnlySyncFile(path));
					serverLog.addToConsole(path.getFileName().toString());					
				});
			}
		}

		// Main directory scan for mods
		serverLog.addToConsole("Starting scan for sync files: " + dateFormatter.format(new Date()));
		for (String directory : directories) {
			serverLog.addToConsole("Scanning " + directory);
			Path _d = Paths.get(directory);
			if (Files.isDirectory(_d)) {
				_list = PathUtils.fileListDeep(Paths.get(directory));

				if (_list != null) {
					serverLog.addToConsole("Found " + _list.size() + " files in: " + directory);

					_list.forEach((path) -> {
						if (!ignoredFiles.matches(path)) {
							standardFiles.add(SyncFile.StandardSyncFile(path));
						} else {								
							serverLog.addToConsole(Main.strings.getString("ignoring") + " " + path.toString());
						}
					});
				} else {
					serverLog.addToConsole("Failed to access: " + directory);
				}
			}
		}

		/* CONFIGS */
		if (!Main.CONFIG.CONFIG_INCLUDE_LIST.isEmpty() && !configsInDirectoryList) {
			//TODO double up? dont we alredy have configs from earlier
			Path configDir = Paths.get("config");
			_list = PathUtils.fileListDeep(configDir);
			serverLog.addToConsole("Found " + _list.size() + " files in: config");

			if (_list != null) {
				_list.forEach((configPath) -> {
					if (includedFiles.matches(configPath)) {							
						serverLog.addToConsole("Including config: " + configPath.getFileName().toString());
						configFiles.add(SyncFile.ConfigSyncFile(configPath));
					}
				});
			}
		}
		
		ServerSetup.allFiles.addAll(ServerSetup.clientOnlyFiles);
		ServerSetup.allFiles.addAll(ServerSetup.standardFiles);
		ServerSetup.allFiles.addAll(ServerSetup.configFiles);
		
		ServerSetup.standardSyncableFiles.addAll(ServerSetup.standardFiles);
		ServerSetup.standardSyncableFiles.addAll(ServerSetup.configFiles);
	}

	@Override
	public void run() {
		serverLog.addToConsole("Creating new server socket");
		try {
			server = new ServerSocket(Main.CONFIG.SERVER_PORT);
		} catch (BindException e) {
			serverLog.addToConsole("socket alredy bound at: " + Main.CONFIG.SERVER_PORT);
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// keep listening indefinitely until program terminates
		serverLog.addToConsole("Now accepting clients...");

		while (true) {
			try {
				// Sanity check, server should never be null here
				if (server == null) {
					break;
				}
				Socket socket = server.accept();
				ServerWorker sc = new ServerWorker(socket, server, generateServerMessages());
				Thread clientThread = new Thread(sc);
				clientThread.setName("ClientThread - " + socket.getInetAddress());
				clientThread.start();
			} catch (IOException e) {
				serverLog.addToConsole("Error while accepting client connection, breaking server listener. You will need to restart serversync");
			}
		}
	}
}
