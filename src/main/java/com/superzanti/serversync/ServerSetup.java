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
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;

import com.superzanti.serversync.util.GlobPathMatcher;
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
	public static ArrayList<SyncFile> allFiles = new ArrayList<SyncFile>();
	public static ArrayList<SyncFile> clientOnlyFiles = new ArrayList<SyncFile>();
	public static ArrayList<String> directories = new ArrayList<String>();

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
			_list = PathUtils.fileListDeep(Paths.get("clientmods"));
			serverLog.addToConsole("Found " + _list.size() + " files in: clientmods");
			
			if (_list != null) {
				try {
					clientOnlyFiles.addAll(SyncFile.parseList(_list));
					clientOnlyFiles.forEach((mod) -> {
						serverLog.addToConsole(mod.MODPATH.toString());
					});
				} catch (IOException e) {
					serverLog.addToConsole("Failed to access files in: " + _list);
				}
			}
		}

		try {
			GlobPathMatcher globber = new GlobPathMatcher();

			// Main directory scan for mods
			serverLog.addToConsole("Starting scan for sync files: " + dateFormatter.format(new Date()));
			for (String directory : directories) {
				serverLog.addToConsole("Scanning " + directory);
				Path _d = Paths.get(directory);
				if (Files.isDirectory(_d)) {
					_list = PathUtils.fileListDeep(Paths.get(directory));

					if (_list != null) {
						serverLog.addToConsole("Found " + _list.size() + " files in: " + directory);

						for (Path file : _list) {
							boolean matchedIgnoreGlob = false;
							for (String glob : Main.CONFIG.FILE_IGNORE_LIST) {
								globber.setPattern(glob);
								if (globber.matches(file)) {
									matchedIgnoreGlob = true;
									serverLog.addToConsole(Main.strings.getString("ignoring") + " " + file.toString());
									break;
								}
							}

							if (!matchedIgnoreGlob) {
								allFiles.add(new SyncFile(file));
							}
						}
					} else {
						serverLog.addToConsole("Failed to access: " + directory);
					}
				}
			}

			Pattern pattern = Pattern.compile("serversync-.+");
			for (int i = 0; i < allFiles.size(); i++) {
				if (pattern.matcher(allFiles.get(i).fileName).matches()) {
					allFiles.remove(i);
					serverLog.addToConsole("Found serversync in the mods folder, removing from sync list");
					break;
				}
			}

			/* CONFIGS */
			if (!Main.CONFIG.CONFIG_INCLUDE_LIST.isEmpty() && !configsInDirectoryList) {
				_list = PathUtils.fileListDeep(Paths.get("config"));
				serverLog.addToConsole("Found " + _list.size() + " files in: config");
				if (_list != null) {
					for (Path path : _list) {
						for (String glob : Main.CONFIG.CONFIG_INCLUDE_LIST) {
							globber.setPattern("config\\" + glob);

							if (globber.matches(path)) {
								serverLog.addToConsole("Including config: " + path.getFileName().toString());
								allFiles.add(new SyncFile(path, false));
								break;
							}
						}
					}
				}
			}

		} catch (IOException e) {
			// TODO Narrow this error handling
			e.printStackTrace();
		}
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
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Scanner input = new Scanner(System.in);
				while(input.hasNext()) {
					if ("exit".equals(input.next())) {
						ServerSetup.serverLog.addToConsole("Exiting serversync");
						break;
					}
				}
				input.close();
				System.exit(0);
			}
		}).start();
		
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
