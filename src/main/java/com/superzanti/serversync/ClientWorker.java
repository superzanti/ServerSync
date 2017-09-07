package com.superzanti.serversync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.superzanti.serversync.util.FileIgnoreMatcher;
import com.superzanti.serversync.util.FileIncludeMatcher;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.MinecraftModInformation;
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
		populateClientFiles(directories, false);
	}
	
	private void populateClientFiles(ArrayList<String> directories, boolean ignoreRules) {
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
				if (!ignoreRules && ignoredFiles.matches(path)) {
					Logger.log(Main.strings.getString("ignoring") + " " + path.toString());
				} else {
					clientFiles.add(SyncFile.StandardSyncFile(path));
				}
			}
		}

		if (clientConfigPaths != null) {
			FileIncludeMatcher includedFiles = new FileIncludeMatcher();

			for (Path path : clientConfigPaths) {
				if (ignoreRules || includedFiles.matches(path)) {
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

		Logger.debug("Checking Server.isUpdateNeeded()");
		Logger.debug(clientFiles.toString());
		updateNeeded = server.isUpdateNeeded(clientFiles);
		updateNeeded = true; // TEMP

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
					// Ignore support for client only files, users may wish to not allow some mods out of personal preference
					if (serverFile.isClientSideOnlyFile && serverFile.matchesIgnoreListPattern()) {
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
					
					if (!serverFiles.contains(clientFile)) { 
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
			populateClientFiles(syncableDirectories, true);
			ArrayList<String> modNames = new ArrayList<>(200);
			ArrayList<String> modHashes = new ArrayList<>(200);
			ArrayList<SyncFile> dupes = new ArrayList<SyncFile>(10);
			for (SyncFile clientFile : clientFiles) {
				MinecraftModInformation modInfo = clientFile.getModInformation();
				if (modInfo != null) {
					if (modNames.contains(modInfo.name)) {	
						Logger.log("<!> Potential duplicate: " + clientFile.getFileName() + " - " + modInfo.name);
						dupes.add(clientFile);
					} else {
						modNames.add(modInfo.name);					
					}					
				} else {
					String hash = clientFile.getFileHash();
					if (modHashes.contains(hash)) {
						Logger.log("<!> Potential duplicate: " + clientFile.getFileName() + " - " + hash);
						dupes.add(clientFile);
					} else {
						modHashes.add(hash);
					}
				}	
			}
			System.out.println(dupes);
		}
		Logger.log(Main.strings.getString("update_complete"));

		// Teardown
		closeWorker();

	}

}
