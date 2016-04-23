package com.superzanti.serversync;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.superzanti.serversync.util.Mod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Sets up listener for clients on the server
 * 
 * @author superzanti
 */
@SideOnly(Side.SERVER)
public class SyncServer implements Runnable {

	// static ServerSocket variable
	private static ServerSocket server;

	// This is what's in our folders
	private static ArrayList<Mod> allMods = new ArrayList<Mod>();
	// Obtained from the servers clientmods directory
	private static ArrayList<Mod> clientOnlyMods = new ArrayList<Mod>();

	protected SyncServer() {
		ArrayList<String> tempList = null;

		try {
			ServerSync.logger.info("Getting ./mods contents");
			/* STANDARD MODS */
			if ((tempList = dirContents("./mods")) != null) {
				allMods.addAll(Mod.parseList(tempList));
			} else {
				ServerSync.logger.info("Could not access ./mods, have you installed forge?");
			}
			/* FLANS MOD CONTENT PACKS */
			if ((tempList = dirContents("./flan")) != null) {
				ServerSync.logger.info("Found flans mod, adding content packs to modlist");
				allMods.addAll(Mod.parseList(tempList));
			}
			// TODO only included configs are loaded
			if (!ServerSyncConfig.INCLUDE_LIST.isEmpty()) {
				if ((tempList = dirContents("./config"))!= null) {
					for (String path : tempList) {
						Path p = Paths.get(path);
						if (ServerSyncConfig.INCLUDE_LIST.contains(p.getFileName().toString().replaceAll(" ", ""))) {
							allMods.add(new Mod(p,false));
						}
					}
				}
			}
			// ServerSync.logger.info("Getting ./config contents");
			/*CLIENT ONLY MODS*/
			if (ServerSyncConfig.PUSH_CLIENT_MODS && (tempList = dirContents("./clientmods")) != null) {
				clientOnlyMods.addAll(Mod.parseList(tempList));
				allMods.addAll(clientOnlyMods);
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
			server = new ServerSocket(ServerSyncConfig.SERVER_PORT);
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
				if (ServerSyncConfig.PUSH_CLIENT_MODS) {
					sc = new ServerWorker(socket, allMods, clientOnlyMods, server);
				} else {
					sc = new ServerWorker(socket, allMods, server);
				}
				new Thread(sc).start();
			} catch (Exception e) {
				ServerSync.logger.info("Error occured." + e);
				e.printStackTrace();
			}
		}
	}

	private static ArrayList<String> dirContents(String dir) {
		ServerSync.logger.info("Getting all of " + dir.replace('\\', '/') + "'s folder contents");
		File f = new File(dir);
		// UPDATE file safety check
		if (f.exists()) {
			File[] files = f.listFiles();
			ArrayList<String> dirList = new ArrayList<String>();
			// Loop through all the directories and only add to the list if it's
			// a file
			for (File file : files) {
				if (file.isDirectory()) {
					dirList.addAll(dirContents(file.getPath()));
				} else {
					dirList.add(file.toString());
				}
			}
			return dirList;
		}
		return null;
	}

}
