package com.superzanti.serversync;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.SyncFile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Sets up listener for clients on the server
 * 
 * @author superzanti
 */
@SideOnly(Side.SERVER)
public class ServerSetup implements Runnable {

	// static ServerSocket variable
	private static ServerSocket server;

	// This is what's in our folders
	private static ArrayList<SyncFile> allMods = new ArrayList<SyncFile>();
	private static ArrayList<SyncFile> clientMods = new ArrayList<SyncFile>();
	
	protected ServerSetup() {
		SyncConfig.serverSide = true;
		ArrayList<Path> tempList = null;
		ArrayList<String> directories = new ArrayList<String>();
		/* DEFAULT DIRECTORIES */
		directories.add("mods");
		//TODO add ability to include directories in the config
		
		if (SyncConfig.PUSH_CLIENT_MODS) {
			tempList = PathUtils.fileListDeep(Paths.get("clientmods"));
			ServerSync.logger.info("Getting all of: clientmods");
			if (tempList != null) {
				try {
					clientMods.addAll(SyncFile.parseList(tempList));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				ServerSync.logger.error("Could not access: clientmods");
			}
			ServerSync.logger.info("Finished getting: clientmods");
		}
		
		if (Files.exists(Paths.get("flan"))) {
			directories.add("flan");
			ServerSync.logger.info("Found flans mod, adding content packs to modlist");
		}

		try {
			for (String directory : directories) {
				String dirString = Paths.get(directory).toAbsolutePath().normalize().toString();
				tempList = PathUtils.fileListDeep(Paths.get(directory));
				ServerSync.logger.info("Getting all of: " + dirString);
				if (tempList != null) {
					allMods.addAll(SyncFile.parseList(tempList));
				} else {
					ServerSync.logger.error("Could not access: " + dirString);
				}
				ServerSync.logger.info("Finished getting: " + dirString);
			}
			
			/* CONFIGS */
			if (!SyncConfig.INCLUDE_LIST.isEmpty()) {
				tempList = PathUtils.fileListDeep(Paths.get("config"));
				if (tempList != null) {
					for (Path path : tempList) {
						if (SyncConfig.INCLUDE_LIST.contains(path.getFileName().toString().replaceAll(" ", ""))) {
							allMods.add(new SyncFile(path,false));
						}
					}
				}
			}
			
		} catch (IOException e) {
			//TODO handle exceptions when loading server
			ServerSync.logger.error(e.getMessage());
		}
	}

	@Override
	public void run() {
		// create the socket server object
		ServerSync.logger.info("Creating new server socket");
		try {
			server = new ServerSocket(SyncConfig.SERVER_PORT);
		} catch (IOException e) {
			ServerSync.logger.info("Error occured." + e);
			e.printStackTrace();
		}
		// keep listens indefinitely until receives 'exit' call or program
		// terminates
		ServerSync.logger.info("Now accepting clients...");
		while (true) {
			try {
				Socket socket = server.accept();
				ServerWorker sc;
				sc = new ServerWorker(socket, allMods, clientMods, server);
				new Thread(sc).start();
			} catch (Exception e) {
				ServerSync.logger.info("Error occured." + e);
				e.printStackTrace();
			}
		}
	}
}
