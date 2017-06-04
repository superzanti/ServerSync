package com.superzanti.serversync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.superzanti.serversync.util.FileIgnoreMatcher;
import com.superzanti.serversync.util.FileIncludeMatcher;
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

	private boolean errorInUpdates = false;
	private boolean updateHappened = false;
	private boolean finished = false;
	private Logger logs;

	private Server server;

	private List<SyncFile> clientFiles;
	private List<SyncFile> ignoredClientSideFiles = new ArrayList<SyncFile>(20);

	public ClientWorker() {
		logs = new Logger();
		clientFiles = new ArrayList<SyncFile>();
		errorInUpdates = false;
		updateHappened = false;
		finished = false;
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

	private void closeWorker() {
		if (server == null) {
			return;
		}

		if (server.close()) {
			logs.updateLogs("Successfully closed all connections", Logger.FULL_LOG);
		}

		if (!updateHappened && !errorInUpdates) {
			logs.updateLogs("No update required", Logger.FULL_LOG);
			Main.clientGUI.updateText(Main.strings.getString("update_not_needed"));
			Main.clientGUI.updateProgress(100);
		} else {
			logs.updateLogs(Main.strings.getString("update_happened"), Logger.FULL_LOG);
			Main.clientGUI.updateProgress(100);
		}
		if (errorInUpdates) {
			logs.updateLogs(Main.strings.getString("update_error"));
		}

		Main.clientGUI.enableSyncButton();
	}

	@Override
	public void run() {
		Main.clientGUI.disableSyncButton();
		List<Path> clientConfigPaths = PathUtils.fileListDeep(Paths.get("config/"));
		List<Path> clientFilePaths = new ArrayList<>();

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
			logs.updateLogs(Main.strings.getString("no_syncable_directories"));
			finished = true;
			closeWorker();
			return;
		}

		for (String directory : syncableDirectories) {
			if (directory.equals("config")) {
				continue;
			}

			List<Path> _files = PathUtils.fileListDeep(Paths.get(directory + "/"));
			if (_files != null) {
				clientFilePaths.addAll(_files);
			}
		}

		// Populate Clients SyncFiles ////////////////////////////////
		if (!clientFilePaths.isEmpty()) {
			FileIgnoreMatcher ignoredFiles = new FileIgnoreMatcher();

			for (Path path : clientFilePaths) {
				if (ignoredFiles.matches(path)) {
					logs.updateLogs(Main.strings.getString("ignoring") + " " + path.toString());
				} else {
					clientFiles.add(SyncFile.StandardSyncFile(path));
				}
			}
		}
		//////////////////////////////////////////////////////////////

		if (clientConfigPaths != null) {
			FileIncludeMatcher includedFiles = new FileIncludeMatcher();

			for (Path path : clientConfigPaths) {
				if (includedFiles.matches(path)) {
					clientFiles.add(SyncFile.ConfigSyncFile(path));
				}
			}
		}

		logs.updateLogs(Main.strings.getString("config_check"));

		// TODO is this needed now? check against last updated perhaps
		if (!server.getConfig()) {
			logs.updateLogs("Failed to obtain config from server");
			errorInUpdates = true;
			this.closeWorker();
			return;
		}

		logs.updateLogs("Checking if update is needed", Logger.FULL_LOG);
		updateNeeded = server.isUpdateNeeded(clientFiles);

		/* MAIN PROCESSING CHUNK */
		if (updateNeeded) {
			logs.updateLogs("Update required", Logger.FULL_LOG);
			updateHappened = true;
			logs.updateLogs(Main.strings.getString("mods_incompatable"));
			logs.updateLogs("<------> " + "Getting files" + " <------>");

			// get all files on server
			logs.updateLogs(Main.strings.getString("mods_get"));
			ArrayList<SyncFile> serverFiles = server.getFiles();

			if (serverFiles == null) {
				logs.updateLogs("Failed to get files from server, check detailed log in minecraft/logs");
				errorInUpdates = true;
				closeWorker();
				return;
			}

			if (serverFiles.isEmpty()) {
				logs.updateLogs("Server has no syncable files");
				finished = true;
				closeWorker();
				return;
			}

			/* CLIENT MODS */
			if (!Main.CONFIG.REFUSE_CLIENT_MODS) {
				logs.updateLogs(Main.strings.getString("mods_accepting_clientmods"));

				ArrayList<SyncFile> serverClientOnlyMods = server.getClientOnlyFiles();

				if (serverClientOnlyMods == null) {
					logs.updateLogs("Failed to access servers client only mods");
					errorInUpdates = true;
				} else {
					serverFiles.addAll(serverClientOnlyMods);
				}
			} else {
				logs.updateLogs(Main.strings.getString("mods_refusing_clientmods"));
			}

			logs.updateLogs(Main.strings.getString("ignoring") + " " + Main.CONFIG.FILE_IGNORE_LIST, Logger.FULL_LOG);

			// run calculations to figure out how big the progress bar is
			// TODO check this logic
			float numberOfFiles = clientFiles.size() + serverFiles.size();
			float percentScale = numberOfFiles / 100;
			float currentPercent = 0;

			/* UPDATING */
			logs.updateLogs("<------> " + Main.strings.getString("update_start") + " <------>");

			/* COMMON MODS */
			for (SyncFile serverFile : serverFiles) {
				currentPercent++;
				SyncFile clientFile;
				if (serverFile.isClientSideOnlyFile) {
					// TODO link this to a config value
					clientFile = SyncFile.ClientOnlySyncFile(serverFile.getClientSidePath());
					this.ignoredClientSideFiles.add(clientFile);
					logs.updateLogs(Main.strings.getString("mods_clientmod_added") + ": " + clientFile.getFileName());
				} else {
					clientFile = SyncFile.StandardSyncFile(serverFile.getFileAsPath());
				}

				boolean exists = Files.exists(clientFile.getFileAsPath());

				if (exists) {
					if (!clientFile.equals(serverFile)) {
						server.updateFile(serverFile, clientFile);
					} else {
						logs.updateLogs(clientFile.getFileName() + " " + Main.strings.getString("up_to_date"),
								Logger.FULL_LOG);
					}
				} else {
					// only need to check for ignore here as we are working
					// on the servers file tree
					if (serverFile.matchesIgnoreListPattern() && !serverFile.isClientSideOnlyFile) {
						logs.updateLogs("<>" + Main.strings.getString("ignoring") + " " + serverFile.getFileName());
					} else {
						logs.updateLogs(serverFile.getFileName() + " " + Main.strings.getString("does_not_exist"),
								Logger.FULL_LOG);
						server.updateFile(serverFile, clientFile);
					}
				}

				Main.clientGUI.updateProgress((int) (currentPercent / percentScale));
			}

			/* DELETION */
			logs.updateLogs("<------> " + Main.strings.getString("delete_start") + " <------>");
			for (SyncFile clientFile : clientFiles) {
				currentPercent++;

				if (clientFile.matchesIgnoreListPattern()) {
					// User created ignore rules
					logs.updateLogs(Main.strings.getString("ignoring") + " " + clientFile.getFileName(),
							Logger.FULL_LOG);
				} else {
					logs.updateLogs(Main.strings.getString("client_check") + " " + clientFile.getFileName(),
							Logger.FULL_LOG);

					boolean servedByServer = false;
					for (SyncFile ignoredClientFile : this.ignoredClientSideFiles) {
						// Client side files provided by the server
						if (ignoredClientFile.equals(clientFile)) {
							servedByServer = true;
						}

					}
					if (servedByServer) {
						logs.updateLogs(Main.strings.getString("ignoring") + " " + clientFile.getFileName(),
								Logger.FULL_LOG);
						continue;
					}

					boolean exists = server.modExists(clientFile);

					if (!exists) {
						logs.updateLogs(clientFile.getFileName() + " " + Main.strings.getString("does_not_match")
								+ Main.strings.getString("delete_attempt"), Logger.FULL_LOG);

						if (clientFile.delete()) {
							logs.updateLogs(
									"<>" + clientFile.getFileName() + " " + Main.strings.getString("delete_success"));
						} else {
							logs.updateLogs("!!! failed to delete: " + clientFile.getFileName() + " !!!");
						}
						updateHappened = true;
					}
					Main.clientGUI.updateProgress((int) (currentPercent / percentScale));
				}
			}
		}
		logs.updateLogs(Main.strings.getString("update_complete"));

		// Teardown
		closeWorker();

	}

}
