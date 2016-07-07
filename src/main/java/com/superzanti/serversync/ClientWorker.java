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
			if (!updateHappened && !errorInUpdates) {
				Main.updateText(Main.strings.getString("update_not_needed"));
				Main.updateProgress(100);
			} else {
				logs.updateLogs(Main.strings.getString("update_happened"), Logger.FULL_LOG);
				Main.updateProgress(100);
			}
			if (errorInUpdates) {
				logs.updateLogs(Main.strings.getString("update_error"));
			}
			Main.toggleButton();
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
			if (!server.connect()) {
				errorInUpdates = true;
				finished = true;
				return;
			}
			
			//TODO implement custom dir sync
			ArrayList<String> dirs = server.getSyncableDirectories();
			List<Path> cFiles = new ArrayList<>();
			for (String dir : dirs) {
				if (dir.equals("config")) {
					continue;
				}
				List<Path> _files = PathUtils.fileListDeep(Paths.get("../"+dir+"/"));
				if (_files != null) {
					cFiles.addAll(_files);
				}
			}
			System.out.println(cFiles);
			
			// Get clients file list //////////////////////////////////////
			List<Path> cConfigs = PathUtils.fileListDeep(Paths.get("../config/"));
			if (!cFiles.isEmpty()) {
				for (Path path : cFiles) {
					String name = path.getFileName().toString();
					if (!SyncConfig.IGNORE_LIST.contains(name)) {						
						clientFiles.add(new SyncFile(path));
					}
				}
			} else {
				logs.updateLogs(Main.strings.getString("no_syncable_directories"));
				errorInUpdates = true;
				finished = true;
				return;
			}
			//////////////////////////////////////////////////////////////

			logs.updateLogs(Main.strings.getString("config_check"));
			server.getConfig();

			// Get keys from the server /////////////////////////////////
			if (!server.getSecurityDetails()) {
				logs.updateLogs(Main.strings.getString("failed_handshake"));
				errorInUpdates = true;
				finished = true;
				return;
			}
			/////////////////////////////////////////////////////////////
			
			if (cConfigs != null) {
				for (Path path : cConfigs) {
					String fileName = path.getFileName().toString();
					if (SyncConfig.INCLUDE_LIST.contains(fileName) && !fileName.equals("serversync.cfg")) {
						clientFiles.add(new SyncFile(path, false));
					}
				}
			}
			
			System.out.println("Checking if update is needed");
			updateNeeded = server.isUpdateNeeded(clientFiles);

			if (updateNeeded) {
				System.out.println("update needed");
				updateHappened = true;
				logs.updateLogs(Main.strings.getString("mods_incompatable"));

				// get all files on server
				logs.updateLogs(Main.strings.getString("mods_get"));
				ArrayList<SyncFile> serverFiles = server.getFiles();
				/* CLIENT MODS */
				if (!SyncConfig.REFUSE_CLIENT_MODS) {
					ArrayList<SyncFile> serverCOMods = server.getClientOnlyFiles();
					serverFiles.addAll(serverCOMods);
					logs.updateLogs(Main.strings.getString("mods_accepting_clientmods"));
				} else {
					logs.updateLogs(Main.strings.getString("mods_refusing_clientmods"));
				}

				logs.updateLogs(Main.strings.getString("ignoring") + " " + SyncConfig.IGNORE_LIST, Logger.FULL_LOG);
				// run calculations to figure out how big the bar is
				float numberOfFiles = clientFiles.size() + serverFiles.size();
				float percentScale = numberOfFiles / 100;
				float currentPercent = 0;

				/* UPDATING */
				logs.updateLogs("<------> "+ Main.strings.getString("update_start") +" <------>");

				/* COMMON MODS */
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
							logs.updateLogs(file.fileName + " " + Main.strings.getString("up_to_date"), Logger.FULL_LOG);
						}
					} else {
						// only need to check for ignore here as we are working
						// on the servers file tree
						if (file.isSetToIgnore() && !file.clientOnlyMod) {
							logs.updateLogs("<>"+ Main.strings.getString("ignoring") + " " + file.fileName);
						} else {
							logs.updateLogs(file.fileName + " " + Main.strings.getString("does_not_exist"), Logger.FULL_LOG);
							server.updateFile(file.MODPATH.toString(), clientPath.toFile());
						}
					}
					Main.updateProgress((int) (currentPercent / percentScale));
				}

				/* DELETION */
				logs.updateLogs("<------> "+ Main.strings.getString("delete_start") +" <------>");
				// Parse clients file tree
				for (SyncFile file : clientFiles) {
					currentPercent++;

					// check for files that need to be deleted
					if (file.isSetToIgnore()) {
						logs.updateLogs(Main.strings.getString("ignoring") + " " + file.fileName, Logger.FULL_LOG);
					} else {
						// Not present in server list
						logs.updateLogs(Main.strings.getString("client_check") + " " + file.fileName, Logger.FULL_LOG);
						boolean exists = server.modExists(file);
						if (!exists) {
							logs.updateLogs(file.fileName + " " + Main.strings.getString("does_not_match") +  Main.strings.getString("delete_attempt"),
									Logger.FULL_LOG);

							// File fails to delete
							try {
								file.delete();
								logs.updateLogs("<>" + file.fileName + " " + Main.strings.getString("delete_success"));
							} catch (IOException e) {
								logs.updateLogs(file.fileName + " " + Main.strings.getString("delete_fail"),
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

			logs.updateLogs(Main.strings.getString("update_complete"));
		} catch (Exception e) {
			logs.updateLogs("Exception caught! - " + e, Logger.FULL_LOG);
			e.printStackTrace();
			errorInUpdates = true;
		} finally {
			closeWorker(server);
		}

	}

}
