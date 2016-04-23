package com.superzanti.serversync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.Mod;
import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.Server;

import runme.Main;

/**
 * Deals with all of the synchronizing for the client, this works without
 * starting minecraft
 * 
 * @author Rheimus
 *
 */
public class OfflineClientWorker implements Runnable {

	private boolean errorInUpdates;
	private boolean updateHappened;
	private boolean finished;
	private Logger logs;

	private List<Mod> clientMods;

	public OfflineClientWorker() {
		logs = new Logger();
		clientMods = new ArrayList<Mod>();
		errorInUpdates = false;
		updateHappened = false;
		finished = false;

		final class Shutdown extends Thread {

			@Override
			public void run() {
				logs.save();
			}

		}
		// Save full log on shutdown
		Runtime.getRuntime().addShutdownHook(new Shutdown());

	}

	public Logger getLogger() {
		return logs;
	}

	public boolean getErrors() {
		return errorInUpdates;
	}

	public boolean getUpdates() {
		return updateHappened;
	}

	public boolean isFinished() {
		return finished;
	}

	@Override
	public void run() {
		Server server = new Server(this, ServerSyncConfig.SERVER_IP, ServerSyncConfig.SERVER_PORT);
		boolean updateNeeded = false;
		updateHappened = false;
		//TODO fix client mod pushing

		try {
			// Get clients mod list
			List<Path> cMods = PathUtils.fileListDeep(Paths.get("../mods/"));
			List<Path> cConfigs = PathUtils.fileListDeep(Paths.get("../config/"));
			for (Path path : cMods) {
				Mod m = new Mod(path);
				clientMods.add(m);
			}

			server.connect();
			logs.updateLogs("Updating serversync config...");
			server.getConfig();
			for (Path path : cConfigs) {
				String fileName = path.getFileName().toString();
				if (ServerSyncConfig.INCLUDE_LIST.contains(fileName)) {
					clientMods.add(new Mod(path,false));
				}
			}
			updateNeeded = server.isUpdateNeeded(clientMods);

			if (updateNeeded) {
				updateHappened = true;
				logs.updateLogs("The mods between server and client are incompatable... Updating...");

				// get all files on server
				logs.updateLogs("Getting mods...");
				ArrayList<Mod> serverMods = server.getFiles();

				logs.updateLogs("Ignoring: " + ServerSyncConfig.IGNORE_LIST, Logger.FULL_LOG);
				// run calculations to figure out how big the bar is
				float numberOfFiles = clientMods.size() + serverMods.size();
				float percentScale = numberOfFiles / 100;
				float currentPercent = 0;

				/* UPDATING */
				// Client only mods are added on the server side and treated as normal mods by the client
				for (Mod mod : serverMods) {
					// Update status
					currentPercent++;

					Path clientFile = mod.CLIENT_MODPATH;
					System.out.println(mod.CLIENT_MODPATH.toString());
					// Get file at rPath location
					boolean exists = Files.exists(clientFile);

					// Exists
					if (exists) {
						Mod clientMod = new Mod(clientFile);
						if (!clientMod.compare(mod)) {
							server.updateFile(mod.MODPATH.toString(), clientFile.toFile());
						} else {
							logs.updateLogs(mod.fileName + " is up to date");
						}
					} else {
						// only need to check for ignore here as we are working
						// on the servers file tree
						if (mod.isSetToIgnore() && !mod.clientOnlyMod) {
							logs.updateLogs("<>" + mod.fileName + " set to ignore");
						} else {
							logs.updateLogs(mod.fileName + " Does not exist...", Logger.FULL_LOG);
							server.updateFile(mod.MODPATH.toString(), clientFile.toFile());
						}
					}
					Main.updateProgress((int) (currentPercent / percentScale));
				}

				/* DELETION */
				// Parse clients file tree
				for (Mod mod : clientMods) {
					currentPercent++;

					// check for files that need to be deleted
					if (mod.isSetToIgnore()) {
						logs.updateLogs(mod.fileName + " set to ignore", Logger.FULL_LOG);
					} else {
						// Not present in server list
						logs.updateLogs("Checking client's " + mod.fileName + " against server", Logger.FULL_LOG);
						boolean exists = server.modExists(mod);
						if (!exists) {
							logs.updateLogs(mod.fileName + " Does not match... attempting phase 1 delete",
									Logger.FULL_LOG);

							// File fails to delete
							if (!mod.delete()) {
								logs.updateLogs(mod.fileName + "Failed to delete flagging for deleteOnExit",
										Logger.FULL_LOG);
								mod.deleteOnExit();
							} else {
								logs.updateLogs("<>" + mod.fileName + " deleted");
							}
							updateHappened = true;
						}
						Main.updateProgress((int) (currentPercent / percentScale));
					}
				}
			} else {
				logs.updateLogs("No Updates Needed");
				Thread.sleep(1000);
			}

			server.exit();

			logs.updateLogs("Update Complete! Have a nice day!");
		} catch (Exception e) {
			logs.updateLogs("Exception caught! - " + e, Logger.FULL_LOG);
			e.printStackTrace();
			errorInUpdates = true;
		} finally {
			server.close();
			if (errorInUpdates) {
				logs.updateLogs(
						"Errors occured, please check ip/port details are correct. For a detailed log check the logs folder in your minecraft directory");
			}
		}

		try

		{
			if (!updateHappened) {
				Main.updateText("No update needed, for a full log check the logs folder in the minecraft directory");
				Main.updateProgress(100);
			} else {
				logs.updateLogs("Files updated", Logger.FULL_LOG);
				Main.updateProgress(100);
			}
			Thread.sleep(100);
			Main.toggleButton();
		} catch (InterruptedException e) {
			logs.updateLogs("Exception caught! - " + e,Logger.FULL_LOG);
		}
		finished = true;
	}

}
