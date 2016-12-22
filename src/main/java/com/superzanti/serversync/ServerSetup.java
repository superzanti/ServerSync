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

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
		//TODO mash dir handling together
		SyncConfig.serverSide = true;
		ArrayList<Path> _list = null;
		ArrayList<String> directories = new ArrayList<String>();
		/* SYNC DIRECTORIES */
		for (String dir : SyncConfig.DIR_LIST) {
			// Specific config handling later
			if (dir.equals("config") || dir.equals("clientmods")) {
				continue;
			}
			directories.add(dir);
		}
		//TODO add ability to include directories in the config
		
		if (SyncConfig.PUSH_CLIENT_MODS) {
			_list = PathUtils.fileListDeep(Paths.get("clientmods"));
			ServerSync.logger.info("Getting all of: clientmods");
			if (_list != null) {
				try {
					clientMods.addAll(SyncFile.parseList(_list));
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
				Path _d = Paths.get(directory);
				if (Files.isDirectory(_d)) {
					String dirString = _d.toAbsolutePath().normalize().toString();
					_list = PathUtils.fileListDeep(Paths.get(directory));
					ServerSync.logger.info("Getting all of: " + dirString);
					
					if (_list != null) {
						allMods.addAll(SyncFile.parseList(_list));
					} else {
						ServerSync.logger.info("Failed to access: " + dirString);
					}
					ServerSync.logger.info("Finished getting: " + dirString);
				}
			}
			
			/* CONFIGS */
			if (!SyncConfig.INCLUDE_LIST.isEmpty()) {
				_list = PathUtils.fileListDeep(Paths.get("config"));
				if (_list != null) {
					for (Path path : _list) {
						if (SyncConfig.INCLUDE_LIST.contains(path.getFileName().toString())) {
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
				sc = new ServerWorker(socket, allMods, clientMods, new ArrayList<String>(SyncConfig.DIR_LIST), server);
				new Thread(sc).start();
			} catch (Exception e) {
				ServerSync.logger.info("Error occured." + e);
				e.printStackTrace();
			}
		}
	}
}
