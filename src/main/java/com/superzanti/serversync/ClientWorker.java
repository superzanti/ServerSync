package com.superzanti.serversync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.superzanti.serversync.util.FileIgnoreMatcher;
import com.superzanti.serversync.util.FileIncludeMatcher;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.Server;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.errors.InvalidSyncFileException;

import runme.Main;

/**
 * Deals with all of the synchronizing for the client, this works without
 * starting minecraft
 * 
 * @author Rheimus
 *
 */
public class ClientWorker implements Runnable {

	private boolean errorInUpdates = false;
	private boolean updateHappened = false;
	private boolean finished = false;

	private Server server;

	private List<SyncFile> clientFiles;
	private List<SyncFile> ignoredClientSideFiles;

	public ClientWorker() {
		clientFiles = new ArrayList<SyncFile>();
		ignoredClientSideFiles = new ArrayList<SyncFile>(20);
		errorInUpdates = false;
		updateHappened = false;
		finished = false;
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

	private void closeWorker() {
		if (server == null) {
			return;
		}

		if (server.close()) {
			Logger.debug("Successfully closed all connections");
		}

		if (!updateHappened && !errorInUpdates) {
			Logger.debug("No update required");
			// TODO Why the manual GUI manipulation here, could use logger?
			Main.clientGUI.updateText(Main.strings.getString("update_not_needed"));
			Main.clientGUI.updateProgress(100);
		} else {
			Logger.debug(Main.strings.getString("update_happened"));
			Main.clientGUI.updateProgress(100);
		}

		if (errorInUpdates) {
			Logger.error(Main.strings.getString("update_error"));
		}

		Main.clientGUI.enableSyncButton();
	}
	
	private void populateClientFiles(ArrayList<String> directories) {
		List<Path> clientFilePaths = new ArrayList<>();
		List<Path> clientConfigPaths = PathUtils.fileListDeep(Paths.get("config/"));
		clientFiles = new ArrayList<SyncFile>(200);
		
		for (String directory : directories) {
			if (directory.equals("config")) {
				continue;
			}

			List<Path> _files = PathUtils.fileListDeep(Paths.get(directory + "/"));
			if (_files != null) {
				clientFilePaths.addAll(_files);
			}
		}

		if (!clientFilePaths.isEmpty()) {
			FileIgnoreMatcher ignoredFiles = new FileIgnoreMatcher();

			for (Path path : clientFilePaths) {
				if (ignoredFiles.matches(path)) {
					Logger.log(Main.strings.getString("ignoring") + " " + path.toString());
				} else {
					clientFiles.add(SyncFile.StandardSyncFile(path));
				}
			}
		}

		if (clientConfigPaths != null) {
			FileIncludeMatcher includedFiles = new FileIncludeMatcher();

			for (Path path : clientConfigPaths) {
				if (includedFiles.matches(path)) {
					clientFiles.add(SyncFile.ConfigSyncFile(path));
				}
			}
		}
	}

	@Override
	public void run() {
		Main.clientGUI.disableSyncButton();
		
		server = new Server(this, Main.CONFIG.SERVER_IP, Main.CONFIG.SERVER_PORT);
		boolean updateNeeded = false;
		updateHappened = false;

		if (!server.connect()) {
			errorInUpdates = true;
			this.closeWorker();
			return;
		}

		ArrayList<String> syncableDirectories = server.getSyncableDirectories();
		if (syncableDirectories == null) {
			errorInUpdates = true;
			closeWorker();
			return;
		}

		if (syncableDirectories.isEmpty()) {
			Logger.log(Main.strings.getString("no_syncable_directories"));
			finished = true;
			closeWorker();
			return;
		}
		
		populateClientFiles(syncableDirectories);

		Logger.log(Main.strings.getString("config_check"));

		// TODO is this needed now? check against last updated perhaps
		if (!server.getConfig()) {
			Logger.error("Failed to obtain config from server");
			errorInUpdates = true;
			closeWorker();
			return;
		}

		Logger.debug("Checking Server.isUpdateNeeded()");
		Logger.debug(clientFiles.toString());
		updateNeeded = server.isUpdateNeeded(clientFiles);

		/* MAIN PROCESSING CHUNK */
		if (updateNeeded) {
			updateHappened = true;
			Logger.log(Main.strings.getString("mods_incompatable"));
			Logger.log("<------> " + "Getting files" + " <------>");

			// get all files on server
			Logger.log(Main.strings.getString("mods_get"));
			ArrayList<SyncFile> serverFiles = server.getFiles();

			if (serverFiles == null) {
				// TODO add to TDB
				Logger.log("Failed to get files from server, check detailed log in minecraft/logs");
				errorInUpdates = true;
				closeWorker();
				return;
			}

			if (serverFiles.isEmpty()) {
				// TODO add to TDB
				Logger.log("Server has no syncable files");
				finished = true;
				closeWorker();
				return;
			}

			/* CLIENT MODS */
			if (!Main.CONFIG.REFUSE_CLIENT_MODS) {
				Logger.log(Main.strings.getString("mods_accepting_clientmods"));

				ArrayList<SyncFile> serverClientOnlyMods = server.getClientOnlyFiles();

				if (serverClientOnlyMods == null) {
					// TODO add to TDB
					Logger.log("Failed to access servers client only mods");
					errorInUpdates = true;
				} else {
					serverFiles.addAll(serverClientOnlyMods);
				}
			} else {
				Logger.log(Main.strings.getString("mods_refusing_clientmods"));
			}

			Logger.debug(Main.strings.getString("ignoring") + " " + Main.CONFIG.FILE_IGNORE_LIST);

			// run calculations to figure out how big the progress bar is
			// TODO check this logic
			float numberOfFiles = clientFiles.size() + serverFiles.size();
			float percentScale = numberOfFiles / 100;
			float currentPercent = 0;

			/* UPDATING */
			Logger.log("<------> " + Main.strings.getString("update_start") + " <------>");

			/* COMMON MODS */
			for (SyncFile serverFile : serverFiles) {
				currentPercent++;
				SyncFile clientFile;
				if (serverFile.isClientSideOnlyFile) {
					// TODO link this to a config value
					clientFile = SyncFile.ClientOnlySyncFile(serverFile.getClientSidePath());
					ignoredClientSideFiles.add(clientFile);
					Logger.log(Main.strings.getString("mods_clientmod_added") + ": " + clientFile.getFileName());
				} else {
					clientFile = SyncFile.StandardSyncFile(serverFile.getFileAsPath());
				}

				boolean exists = Files.exists(clientFile.getFileAsPath());

				if (exists) {
					try {						
						if (!clientFile.equals(serverFile)) {
							server.updateFile(serverFile, clientFile);
						} else {
							Logger.log(clientFile.getFileName() + " " + Main.strings.getString("up_to_date"));
						}
					} catch (InvalidSyncFileException e) {
						//TODO stub invalid file handling
						Logger.debug(e);
					}
				} else {
					// only need to check for ignore here as we are working
					// on the servers file tree
					if (serverFile.matchesIgnoreListPattern() && !serverFile.isClientSideOnlyFile) {
						Logger.log("<>" + Main.strings.getString("ignoring") + " " + serverFile.getFileName());
					} else {
						Logger.debug(serverFile.getFileName() + " " + Main.strings.getString("does_not_exist"));
						server.updateFile(serverFile, clientFile);
					}
				}

				Main.clientGUI.updateProgress((int) (currentPercent / percentScale));
			}

			/* DELETION */
			Logger.log("<------> " + Main.strings.getString("delete_start") + " <------>");
			for (SyncFile clientFile : clientFiles) {
				currentPercent++;

				if (clientFile.matchesIgnoreListPattern()) {
					// User created ignore rules
					Logger.debug(Main.strings.getString("ignoring") + " " + clientFile.getFileName());
				} else {
					Logger.debug(Main.strings.getString("client_check") + " " + clientFile.getFileName());

					boolean servedByServer = false;
					for (SyncFile ignoredClientFile : ignoredClientSideFiles) {
						// Client side files provided by the server
						try {							
							if (clientFile.equals(ignoredClientFile)) {
								servedByServer = true;
								break;
							}
						} catch (InvalidSyncFileException e) {
							//TODO stub invalid sync file handling
							e.printStackTrace();
						}
					}
					if (servedByServer) {
						Logger.debug(Main.strings.getString("ignoring") + " " + clientFile.getFileName());
						continue;
					}

					boolean exists = server.modExists(clientFile);

					if (!exists) {
						Logger.debug(clientFile.getFileName() + " " + Main.strings.getString("does_not_match")
								+ Main.strings.getString("delete_attempt"));

						if (clientFile.delete()) {
							Logger.log(
									"<>" + clientFile.getFileName() + " " + Main.strings.getString("delete_success"));
						} else {
							Logger.log("!!! failed to delete: " + clientFile.getFileName() + " !!!");
						}
						updateHappened = true;
					}
					Main.clientGUI.updateProgress((int) (currentPercent / percentScale));
				}
			}
			
			//TODO complete this with user prompt to pick which duplicate to keep
			/* DUPLICATE CHECK */
			populateClientFiles(syncableDirectories);
			HashMap<String, SyncFile> modList = new HashMap<String, SyncFile>(200);
			ArrayList<SyncFile> dupes = new ArrayList<SyncFile>(10);
			for (SyncFile clientFile : clientFiles) {
				if (clientFile.getModInformation() != null) {
					System.out.println(clientFile.getFileName());
					if (modList.get(clientFile.getModInformation().name) != null) {	
						Logger.log("<!> Potential duplicate: " + clientFile.getFileName() + " - " + clientFile.getModInformation().name);
						dupes.add(clientFile);
					} else {
						modList.put(clientFile.getModInformation().name, clientFile);					
					}					
				} else {
					//TODO what to do when the file has no mod information available
				}	
			}
			System.out.println(dupes);
		}
		Logger.log(Main.strings.getString("update_complete"));

		// Teardown
		closeWorker();

	}

}
