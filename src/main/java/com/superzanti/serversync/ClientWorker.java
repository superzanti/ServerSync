package com.superzanti.serversync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.Server;
import com.superzanti.serversync.util.SyncFile;

import runme.Main;

/**
 * Deals with all of the synchronizing for the client, this works without
 * starting minecraft
 * 
 * @author Rheimus
 *
 */
public class ClientWorker implements Runnable {

	private boolean errorInUpdates;
	private boolean updateHappened;
	private boolean finished;
	private Logger logs;

	private List<SyncFile> clientFiles;

	public ClientWorker() {
		logs = new Logger();
		clientFiles = new ArrayList<SyncFile>();
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

	private void closeWorker(Server server) {
		server.close();
		if (!finished) {
			try {
				if (!updateHappened && !errorInUpdates) {
					Main.updateText(
							"No update needed, for a full log check the logs folder in the minecraft directory");
					Main.updateProgress(100);
				} else {
					logs.updateLogs("Files updated", Logger.FULL_LOG);
					Main.updateProgress(100);
				}
				if (errorInUpdates) {
					logs.updateLogs(
							"Errors occured, please check ip/port details are correct. For a detailed log check the logs folder in your minecraft directory");
				}
				Thread.sleep(100);
				Main.toggleButton();
			} catch (InterruptedException e) {
				logs.updateLogs("Exception caught! - " + e, Logger.FULL_LOG);
				Main.toggleButton();
			}
			finished = true;
		} else {
			Main.toggleButton();
		}
	}

	@Override
	public void run() {
		Server server = new Server(this, SyncConfig.SERVER_IP, SyncConfig.SERVER_PORT);
		boolean updateNeeded = false;
		updateHappened = false;

		try {
			// Get clients mod list
			List<Path> cMods = PathUtils.fileListDeep(Paths.get("../mods/"));
			List<Path> cConfigs = PathUtils.fileListDeep(Paths.get("../config/"));
			if (cMods != null) {
				for (Path path : cMods) {
					SyncFile f = new SyncFile(path);
					clientFiles.add(f);
				}
			}

			if (!server.connect()) {
				errorInUpdates = true;
				finished = true;
				closeWorker(server);
				return;
			}

			logs.updateLogs("Updating serversync config...");
			server.getConfig();
			if (cConfigs != null) {
				for (Path path : cConfigs) {
					String fileName = path.getFileName().toString();
					if (SyncConfig.INCLUDE_LIST.contains(fileName)) {
						clientFiles.add(new SyncFile(path, false));
					}
				}
			}
			updateNeeded = server.isUpdateNeeded(clientFiles);

			if (updateNeeded) {
				updateHappened = true;
				logs.updateLogs("The mods between server and client are incompatable... Updating...");

				// get all files on server
				logs.updateLogs("Getting mods...");
				ArrayList<SyncFile> serverFiles = server.getFiles();

				logs.updateLogs("Ignoring: " + SyncConfig.IGNORE_LIST, Logger.FULL_LOG);
				// run calculations to figure out how big the bar is
				float numberOfFiles = clientFiles.size() + serverFiles.size();
				float percentScale = numberOfFiles / 100;
				float currentPercent = 0;

				/* UPDATING */
				logs.updateLogs("<------> Starting Update Process <------>");
				// Client only mods are added on the server side and treated as
				// normal mods by the client
				for (SyncFile file : serverFiles) {
					// Update status
					currentPercent++;

					Path clientPath = file.CLIENT_MODPATH;
					// Get file at rPath location
					boolean exists = Files.exists(clientPath);

					// Exists
					if (exists) {
						SyncFile clientFile = new SyncFile(clientPath);
						if (!clientFile.compare(file)) {
							server.updateFile(file.MODPATH.toString(), clientPath.toFile());
						} else {
							logs.updateLogs(file.fileName + " is up to date");
						}
					} else {
						// only need to check for ignore here as we are working
						// on the servers file tree
						if (file.isSetToIgnore() && !file.clientOnlyMod) {
							logs.updateLogs("<>" + file.fileName + " set to ignore");
						} else {
							logs.updateLogs(file.fileName + " Does not exist...", Logger.FULL_LOG);
							server.updateFile(file.MODPATH.toString(), clientPath.toFile());
						}
					}
					Main.updateProgress((int) (currentPercent / percentScale));
				}

				/* DELETION */
				logs.updateLogs("<------> Starting Deletion Process <------>");
				// Parse clients file tree
				for (SyncFile file : clientFiles) {
					currentPercent++;

					// check for files that need to be deleted
					if (file.isSetToIgnore()) {
						logs.updateLogs(file.fileName + " set to ignore", Logger.FULL_LOG);
					} else {
						// Not present in server list
						logs.updateLogs("Checking client's " + file.fileName + " against server", Logger.FULL_LOG);
						boolean exists = server.modExists(file);
						if (!exists) {
							logs.updateLogs(file.fileName + " Does not match... attempting phase 1 delete",
									Logger.FULL_LOG);

							// File fails to delete
							try {
								file.delete();
								logs.updateLogs("<>" + file.fileName + " deleted");
							} catch(IOException e) {
								logs.updateLogs(file.fileName + "Failed to delete flagging for deleteOnExit",
										Logger.FULL_LOG);
								file.deleteOnExit();
							}
						
							updateHappened = true;
						}
						Main.updateProgress((int) (currentPercent / percentScale));
					}
				}
			}

			server.exit();

			logs.updateLogs("Update Complete! Have a nice day!");
		} catch (Exception e) {
			logs.updateLogs("Exception caught! - " + e, Logger.FULL_LOG);
			e.printStackTrace();
			errorInUpdates = true;
		} finally {
			closeWorker(server);
		}

	}

}
