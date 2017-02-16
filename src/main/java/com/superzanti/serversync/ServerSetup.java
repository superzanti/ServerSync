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

import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EServerMessage;

import runme.Main;

/**
 * Sets up various server data to be passed to the specific client socket being communicated with
 * 
 * @author Rheimus
 */
public class ServerSetup implements Runnable {

	// static ServerSocket variable
	private static ServerSocket server;

	// This is what's in our folders
	public static ArrayList<SyncFile> allMods = new ArrayList<SyncFile>();
	public static ArrayList<SyncFile> clientMods = new ArrayList<SyncFile>();
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
	
	protected ServerSetup() {
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
			_list = PathUtils.fileListDeep(Paths.get("clientmods"));
			System.out.println("Found " + _list.size() + " files in: clientmods");
			
			_list.forEach((path) -> {
				System.out.println(path);
			});
			
			if (_list != null) {
				try {
					clientMods.addAll(SyncFile.parseList(_list));
					clientMods.forEach((mod) -> {
						System.out.println(mod.MODPATH);
					});
				} catch (IOException e) {
					System.out.println("Failed to access files in: " + _list);
				}
			}
		}
		
		//Specific mod compatability
		if (Files.exists(Paths.get("flan"))) {
			directories.add("flan");
			System.out.println("Found flans mod, adding content packs to sync");
		}
		//

		try {
			// Main directory scan for mods
			System.out.println("Starting scan for sync files: " + dateFormatter.format(new Date()));
			for (String directory : directories) {
				System.out.println("Scanning " + directory);
				Path _d = Paths.get(directory);
				if (Files.isDirectory(_d)) {
					_list = PathUtils.fileListDeep(Paths.get(directory));
					
					if (_list != null) {
						allMods.addAll(SyncFile.parseList(_list));
						System.out.println("Found " + _list.size() + " files in: " + directory);
					} else {
						System.out.println("Failed to access: " + directory);
					}
				}
			}
			
			/* CONFIGS */
			if (!Main.CONFIG.CONFIG_INCLUDE_LIST.isEmpty() && !configsInDirectoryList) {
				_list = PathUtils.fileListDeep(Paths.get("config"));
				System.out.println("Found " + _list.size() + " files in: config");
				if (_list != null) {
					for (Path path : _list) {
						if (Main.CONFIG.CONFIG_INCLUDE_LIST.contains(path.getFileName().toString())) {
							System.out.println("Including config: " + path.getFileName().toString());
							allMods.add(new SyncFile(path,false));
						}
					}
				}
			}
			
		} catch (IOException e) {
			//TODO Narrow this error handling
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		System.out.println("Creating new server socket");
		try {
			server = new ServerSocket(Main.CONFIG.SERVER_PORT);
		} catch (BindException e) { 
			System.out.println("socket alredy bound at: " + Main.CONFIG.SERVER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// keep listening indefinitely until program terminates
		System.out.println("Now accepting clients...");
		while (true) {
			try {
				Socket socket = server.accept();
				ServerWorker sc = new ServerWorker(socket, server, generateServerMessages());;
				Thread clientThread = new Thread(sc);
				clientThread.setName("ClientThread - " + socket.getInetAddress());
				clientThread.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
