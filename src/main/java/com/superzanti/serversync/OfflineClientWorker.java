package com.superzanti.serversync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.superzanti.serversync.util.Log;
import com.superzanti.serversync.util.Md5;
import com.superzanti.serversync.util.PathUtils;

import runme.Main;

/**
 * The intention of this class is to provide a way to synchronize mods without
 * starting up minecraft.<br>
 * Ideally this will save a lot of time bypassing forges mod loading code
 * 
 * @author Rheimus
 *
 */
public class OfflineClientWorker implements Runnable {

	private static Socket socket;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;
	private static InetAddress host = null;

	private static boolean errorInUpdates;
	private static boolean updateHappened;
	private static boolean checkFinished;
	private static Log fullLog = new Log("serversync-detailed");
	private static Log log = new Log("serversync-ui");

	// These contain abstract paths of files
	private List<String> clientFiles;

	public OfflineClientWorker() {
		errorInUpdates = false;
		updateHappened = false;
		checkFinished = false;

		final class Shutdown extends Thread {

			@Override
			public void run() {
				fullLog.saveLog();
			}

		}
		Runtime.getRuntime().addShutdownHook(new Shutdown());

	}
	
	private void updateFile(String filePath, File currentFile) throws IOException, Exception {
		updateFile(filePath,currentFile,false);
	}

	/**
	 * Sends request to server for the file stored at filePath and updates the
	 * current file with the returned data
	 * 
	 * @param filePath
	 *            the location of the file on the server
	 * @param currentFile
	 *            the current file being worked on
	 * @throws IOException
	 * @throws Exception
	 */
	private void updateFile(String filePath, File currentFile, boolean getConfig) throws IOException, Exception {
		oos.writeObject(Main.SECURE_FILESIZE);
		oos.flush();
		oos.writeObject(filePath);
		oos.flush();

		long fileSize = 0l;

		try {
			fileSize = ois.readLong();
		} catch (Exception e) {
			System.out.println("Could not get file size");
		}

		if(!getConfig) {
			oos.writeObject(ServerSyncConfig.SECURE_UPDATE);
			oos.flush();
			oos.writeObject(filePath);
			oos.flush();
		} else {
			oos.writeObject(ServerSyncConfig.GET_CONFIG);
			oos.flush();
		}

		currentFile.getParentFile().mkdirs();
		FileOutputStream wr = new FileOutputStream(currentFile);

		byte[] outBuffer = new byte[socket.getReceiveBufferSize()];
		int bytesReceived = 0;

		double progress = 0;
		double byteP = 0;
		double factor = 0;

		while ((bytesReceived = ois.read(outBuffer)) > 0) {
			byteP++;
			factor = fileSize / bytesReceived;
			progress = Math.ceil(byteP / factor * 100);
			wr.write(outBuffer, 0, bytesReceived);
			Main.updateText("<" + (int) progress + "%> Updating " + currentFile.getName());
		}
		wr.flush();
		wr.close();
		reinitConn();

		updateHappened = true;
		updateLogs("Sucessfully updated " + currentFile.getName());
	}

	private static void reinitConn() throws Exception {
		fullLog.add("Reinitializing the connection...");
		oos.flush();
		// close our resources and set values to null
		if (oos != null)
			oos.close();
		if (ois != null)
			ois.close();
		if (socket != null)
			socket.close();
		socket = null;
		oos = null;
		ois = null;
		// socket = new Socket(host.getHostName(),
		// ServerSyncRegistry.SERVER_PORT);
		socket = new Socket();
		socket.connect(new InetSocketAddress(host.getHostName(), ServerSyncConfig.SERVER_PORT), 5000);
		// write to socket using ObjectOutputStream
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		fullLog.add("Sending requests to Socket Server...");
	}

	protected static boolean getErrors() {
		return errorInUpdates;
	}

	protected static boolean getUpdates() {
		return updateHappened;
	}

	protected static boolean getFinished() {
		return checkFinished;
	}

	protected static void setFinished(boolean newFinished) {
		checkFinished = newFinished;
		return;
	}

	protected static void updateLogs(String s, boolean... update) {
		fullLog.add(s);
		log.add(s);
		// lazyness heh
		if (update != null) {
			if (update.length == 0) {
				Main.updateText(log.getReadableContent());
			}
		} else {
			Main.updateText(log.getReadableContent());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		// use the ip address of the server to get the host
		try {
			host = InetAddress.getByName(ServerSyncConfig.SERVER_IP);
		} catch (UnknownHostException e) {
			fullLog.add("Exception caught! - " + e);
			updateLogs("Could not connect to server, check your cfg settings");
			errorInUpdates = true;
		}
		socket = null;
		oos = null;
		ois = null;

		updateLogs("< Connecting to server >");
		try {
			// establish socket connection to server
			fullLog.add("Establishing a socket connection to the server...");

			socket = new Socket();
			try {
				socket.connect(new InetSocketAddress(host.getHostName(), ServerSyncConfig.SERVER_PORT), 5000);
			} catch (Exception e) {
				updateLogs("Could not connect to: " + ServerSyncConfig.SERVER_IP + ":" + ServerSyncConfig.SERVER_PORT);
				errorInUpdates = true;
				return;
			}

			fullLog.add("Socket established...");

			// write to socket using ObjectOutputStream
			fullLog.add("Creating input/output streams...");
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			
			// Config was not found getting it from server
			if (ServerSyncConfig.pullServerConfig) {
				updateLogs("Getting servers config file");
				Path cfig = Paths.get("../config/serversync.cfg");
				updateFile("./config/serversync.cfg", cfig.toFile(), true);
				updateLogs("Reloading config");
				ServerSyncConfig.getServerDetailsDirty(cfig);
			}

			updateLogs("Checking to see if updates are needed...");
			oos.writeObject(ServerSyncConfig.SECURE_CHECK);
			oos.flush();

			String lastUpdate = (String) ois.readObject();
			updateLogs("Our version is:" + ServerSyncConfig.LAST_UPDATE, false);
			updateLogs("The server's version is:" + lastUpdate);

			oos.writeObject(ServerSyncConfig.SECURE_CHECKMODS);
			oos.flush();
			String serverModList = (String) ois.readObject();

			// Ignored mod list used for filtering purposes only in next for
			// loop
			// List<String> _clientFiles = PathUtils.fileListDeep("");
			// Mod list clone that ends up being filtered mod list
			clientFiles = PathUtils.fileListDeep(Paths.get("../mods"));
			Path flanCheck = Paths.get("../flan");
			if (Files.exists(flanCheck)) {				
				clientFiles.addAll(PathUtils.fileListDeep(flanCheck));
				fullLog.add("Found flans mod, adding content packs");
			}
			// Remove ignored mods from mod list
			// What is actually used from the ModContainer interface?
			/*
			 * 1) absolute path of mod file, can get manually seems to only be
			 * used for checking weather to run the update or not
			 */
			for (String entry : clientFiles) {
				Path modPath = Paths.get(entry).toRealPath();
				// Should get minecraft root dir
				Path rootPath = Paths.get("../").toAbsolutePath();
				String relativeModPath = "./" + rootPath.relativize(modPath);
				if (ServerSyncConfig.IGNORE_LIST.contains(relativeModPath.replace('\\', '/'))) {
					clientFiles.remove(entry);
					fullLog.add("removed " + entry + " from clientFiles");
				}
			}

			Iterator<String> cIterator = clientFiles.iterator();
			String cMods = "";
			while (cIterator.hasNext()) {
				cMods += Paths.get(cIterator.next()).getFileName();
				if (cIterator.hasNext()) {
					cMods += ", ";
				}
			}
			fullLog.add("Syncable client mods are: " + cMods);
			fullLog.add("Syncable server mods are: " + serverModList.toString());

			// This probably wont work, need to test what is returned
			// server returns list with {id = value}
			// TODO better system to compare mods
			boolean serverCompatable = serverModList.equals(clientFiles.toString());

			if (!serverCompatable) {
				updateLogs("The mods between server and client are incompatable... Force updating...", false);

				if(!ServerSyncConfig.pullServerConfig) {
					updateLogs("Updating serversync config...");
					
					String sConfigPath = "./config/serversync.cfg";
					Path configPath = Paths.get("../config/serversync.cfg");
					oos.writeObject(ServerSyncConfig.SECURE_CHECKSUM);
					oos.flush();
					oos.writeObject(sConfigPath);
					oos.flush();
					String serverChecksum = (String) ois.readObject();
					// if the checksums do not match, update the file
					if (!Md5.md5String(configPath.toFile()).equals(serverChecksum)) {
						fullLog.add("updating config");
						updateFile(sConfigPath, configPath.toFile());
						// reload config file
						fullLog.add("reloading config");
						ServerSyncConfig.getServerDetailsDirty(configPath);
					}
				}
			}

			if (!lastUpdate.equals(ServerSyncConfig.LAST_UPDATE) || !serverCompatable) {

				fullLog.add("Sending requests to Socket Server...");

				updateLogs("Getting client-side mods");
				ArrayList<String> serverClientMods = new ArrayList<String>();
				oos.writeObject(Main.SECURE_PUSH_CLIENTMODS);
				oos.flush();
				serverClientMods = (ArrayList<String>) ois.readObject();
				fullLog.add(serverClientMods.toString());
				fullLog.add("Got client mods");

				for (String file : serverClientMods) {
					String pathOnClient = file.replace("\\", "/").replace("clientmods", "mods");
					String pathOnServer = file.replace("\\", "/");
					Path currentFile = Paths.get("." + pathOnClient);
					String fileName = currentFile.getFileName().toString();

					if (Files.exists(currentFile) && !Files.isDirectory(currentFile)) {
						oos.writeObject(ServerSyncConfig.SECURE_CHECKSUM);
						oos.flush();
						oos.writeObject(pathOnServer);
						oos.flush();
						String serverChecksum = (String) ois.readObject();
						// if the checksums do not match, update the file
						if (!Md5.md5String(currentFile.toFile()).equals(serverChecksum)) {
							fullLog.add("Server Checksum: " + serverChecksum);
							fullLog.add("Our Checksum: " + Md5.md5String(currentFile.toFile()));

							updateFile(pathOnServer, currentFile.toFile());

							// Matches
						} else {
							updateLogs(fileName + " exists or is up to date");
						}
						// Does not exist
					} else {
						fullLog.add(pathOnClient + " Does not exist...");
						updateFile(pathOnServer, currentFile.toFile());
					}
				}

				// get all files on server
				updateLogs("Getting mods...");
				oos.writeObject(ServerSyncConfig.SECURE_RECURSIVE);
				oos.flush();
				// read the server response message
				ArrayList<String> fileTree = new ArrayList<String>();
				fileTree = (ArrayList<String>) ois.readObject();
				fullLog.add(fileTree.toString());

				fullLog.add("Got filetree from server...");

				// get all the files at home so we can update the progress bar
				ArrayList<String> allList = new ArrayList<String>();
				allList.addAll(PathUtils.fileListDeep(Paths.get("../mods")));
				allList.addAll(PathUtils.fileListDeep(Paths.get("../config")));

				fullLog.add("Got filetree from client...");

				fullLog.add("Ignoring: " + ServerSyncConfig.IGNORE_LIST);

				// run calculations to figure out how big the bar is
				float numberOfFiles = allList.size() + fileTree.size();
				float percentScale = numberOfFiles / 100;
				float currentPercent = 0;

				// Parse servers file tree
				for (String singleFile : fileTree) {
					// Update status
					currentPercent++;

					// Readable/Usable path saves resources instead of multiple
					// string method calls
					String rPath = singleFile.replace("\\", "/");
					String clientPath = "." + rPath;

					// Get file at rPath location
					File currentFile = new File(clientPath);

					// Get mod/config's name (last entry in path series)
					String fileName = currentFile.getName();

					fullLog.add("Checking server's " + rPath);

					// Exists
					if (currentFile.exists() && !currentFile.isDirectory()) {
						oos.writeObject(ServerSyncConfig.SECURE_CHECKSUM);
						oos.flush();
						oos.writeObject(rPath);
						oos.flush();
						String serverChecksum = (String) ois.readObject();
						// if the checksums do not match, update the file
						if (!Md5.md5String(currentFile).equals(serverChecksum)) {
							if (ServerSyncConfig.IGNORE_LIST.contains(rPath)) {
								updateLogs(fileName + "set to ignore");

								// Exists but dosen't match
							} else {
								fullLog.add("Server Checksum: " + serverChecksum);
								fullLog.add("Our Checksum: " + Md5.md5String(currentFile));

								updateFile(rPath, currentFile);
							}

							// Matches
						} else {
							updateLogs(fileName + " exists or is up to date");
						}
					} else {
						// only need to check for ignore here as we are working
						// on the servers file tree
						if (ServerSyncConfig.IGNORE_LIST.contains(rPath)) {
							updateLogs("<>" + fileName + "set to ignore");

							// Dosen't exist
						} else {
							fullLog.add(rPath + " Does not exist...");
							updateFile(rPath, currentFile);
						}
					}
					Main.updateProgress((int) (currentPercent / percentScale));
				}

				/* DELETION */
				// Parse clients file tree
				for (String singleFile : allList) {
					currentPercent++;
					String rPath = singleFile.replace('\\', '/');
					String serverReadablePath = rPath.replaceFirst(".", "");
					String filePathSaveable = rPath.replaceFirst(".", "").replace("/", "_$_");

					File fileToDelete = new File(rPath);
					String fileName = fileToDelete.getName();

					fullLog.add("Checking client's " + rPath + " against server");
					oos.writeObject(ServerSyncConfig.SECURE_EXISTS);
					oos.flush();
					oos.writeObject(serverReadablePath);
					oos.flush();

					// check for files that need to be deleted
					String doesExist = (String) ois.readObject();
					if (ServerSyncConfig.IGNORE_LIST.contains(serverReadablePath)) {
						fullLog.add(fileName + " set to ignore");
					} else {
						// Not present in server list
						if (doesExist.equalsIgnoreCase("false")) {
							fullLog.add(rPath + " Does not match... attempting phase 1 delete");

							// File fails to delete
							if (!fileToDelete.delete()) {
								fullLog.add("Failed to delete flagging for deleteOnExit");

								fileToDelete.deleteOnExit(); // For if it gets
																// fixed, will
																// cause exit
																// code to not
																// be needed
								// exit code will not break if this manages to
								// delete the file

								File referenceFile = new File("mods/serversync_delete/" + filePathSaveable);
								fullLog.add("Creating reference to " + fileName + " in: "
										+ referenceFile.getAbsolutePath());

								referenceFile.getParentFile().mkdirs();
								FileOutputStream wr = new FileOutputStream(referenceFile);
								wr.write("D".getBytes(), 0, 1);
								wr.flush();
								wr.close();
							} else {
								updateLogs("<>" + fileName + " deleted");
							}
							updateHappened = true;
						}
						Main.updateProgress((int) (currentPercent / percentScale));
					}
				}
				// I found that these lines are not needed since the client
				// updates server sync anyway.
				// ServerSyncRegistry.config.getCategory("StorageVariables").get("LAST_UPDATE").set(lastUpdate);
				// ServerSyncRegistry.config.save();
			} else {
				updateLogs("No Updates Needed");
				Thread.sleep(1000);
			}

			fullLog.add("Telling Server to Exit...");

			updateLogs("Update Complete! Have a nice day!");
			oos.writeObject(ServerSyncConfig.SECURE_EXIT);
			oos.flush();
		} catch (Exception e) {
			fullLog.add("Exception caught! - " + e);
			e.printStackTrace();
			errorInUpdates = true;
		} finally {
			try {
				fullLog.add("Closing connections...");

				if (oos != null)
					oos.close();
				if (ois != null)
					ois.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				fullLog.add("Exception caught! - " + e);
				e.printStackTrace();
				errorInUpdates = true;
			} // close resources here!
			fullLog.add("All of serversync's sockets to the server have been closed.");
			if (errorInUpdates) {
				updateLogs(
						"Errors occured, please check ip/port details are correct. For a detailed log check the logs folder in your minecraft directory");
			}
		}

		try {
			if (!updateHappened) {
				Main.updateText("No update needed, for a full log check the logs folder in the minecraft directory");
				Thread.sleep(1000);
				Main.updateProgress(100);
			} else {
				fullLog.add("Files updated");
				Thread.sleep(1000);
				Main.updateProgress(100);
			}
			Thread.sleep(100);
		} catch (InterruptedException e) {
			fullLog.add("Exception caught! - " + e);
		} finally {
			// fullLog.saveLog();
		}
		checkFinished = true;
		return;
	}

}
