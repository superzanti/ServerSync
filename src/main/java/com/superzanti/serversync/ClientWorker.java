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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Maps;
import com.superzanti.serversync.util.Md5;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This worker handles all the checking/updating/deleting of files for the client
 * @author superzanti
 */
@SideOnly(Side.CLIENT)
public class ClientWorker implements Runnable {

	private static Socket socket;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;
	private static InetAddress host = null;

	private static boolean errorInUpdates;
	private static boolean updateHappened;
	private static boolean checkFinished;

	protected ClientWorker() {

		errorInUpdates = false;
		updateHappened = false;
		checkFinished = false;

	}

	/**
	 * Updates file and flags if restart is required
	 * @param filePath String representation of the files path, used to request a file from the server
	 * @param currentFile Clients file being currently worked on
	 * @throws IOException
	 * @throws Exception
	 */
	private void updateFile(String filePath, File currentFile) throws IOException, Exception {

		oos.writeObject(ServerSyncConfig.SECURE_UPDATE);
		oos.flush();
		oos.writeObject(filePath);
		oos.flush();

		currentFile.getParentFile().mkdirs();
		FileOutputStream wr = new FileOutputStream(currentFile);
		byte[] outBuffer = new byte[socket.getReceiveBufferSize()];
		int bytesReceived = 0;
		while ((bytesReceived = ois.read(outBuffer)) > 0) {
			wr.write(outBuffer, 0, bytesReceived);
		}
		wr.flush();
		wr.close();
		reinitConn();
		
		updateHappened = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		//TODO check and update serversync.cfg first to avoid the need to double run
		// use the ip address of the server to get the host
		try {
			host = InetAddress.getByName(ServerSyncConfig.SERVER_IP);
		} catch (UnknownHostException e) {
			ServerSync.logger.error("Exception caught! - " + e);
			e.printStackTrace();
			errorInUpdates = true;
		}
		socket = null;
		oos = null;
		ois = null;
		SyncClient.updateScreenWorking(1, "Connecting to server...");
		try {
			// establish socket connection to server
			ServerSync.logger.info("Establishing a socket connection to the server...");

			// socket = new Socket(host.getHostName(),
			// ServerSyncRegistry.SERVER_PORT);
			socket = new Socket();
			socket.connect(new InetSocketAddress(host.getHostName(), ServerSyncConfig.SERVER_PORT), 5000);

			SyncClient.updateScreenWorking(2, "Socket established...");

			// write to socket using ObjectOutputStream
			ServerSync.logger.info("Creaing input/output streams...");
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());

			SyncClient.updateScreenWorking(3, "Checking to see if updates are needed...");
			oos.writeObject(ServerSyncConfig.SECURE_CHECK);
			oos.flush();
			String lastUpdate = (String) ois.readObject();
			ServerSync.logger.info("Our version is:" + ServerSyncConfig.LAST_UPDATE);
			ServerSync.logger.info("The server's version is:" + lastUpdate);

			oos.writeObject(ServerSyncConfig.SECURE_CHECKMODS);
			oos.flush();
			String serverModList = (String) ois.readObject();
			Map<String, ModContainer> clientModList_ = Maps.newHashMap(Loader.instance().getIndexedModList());
			Map<String, ModContainer> clientModList = Maps.newHashMap(Loader.instance().getIndexedModList());
			for (Map.Entry<String, ModContainer> modEntry : clientModList_.entrySet()) {
				Path modPath = Paths.get(modEntry.getValue().getSource().getAbsolutePath());
				Path rootPath = Paths.get("").toAbsolutePath();
				String relativeModPath = "./" + rootPath.relativize(modPath);
				if (ServerSyncConfig.IGNORE_LIST.contains(relativeModPath.replace('\\', '/'))) {
					clientModList.remove(modEntry.getKey());
				}
			}
			ServerSync.logger.info("Syncable client mods are: " + clientModList.toString());
			ServerSync.logger.info("Syncable server mods are: " + serverModList.toString());
			if (!serverModList.toString().equals(clientModList.toString())) {
				ServerSync.logger
						.info("The mods between server and client are incompatable... Force updating...");
			}

			if (!lastUpdate.equals(ServerSyncConfig.LAST_UPDATE)
					|| !serverModList.toString().equals(clientModList.toString())) {

				ServerSync.logger.info("Sending requests to Socket Server...");

				// get all files on server
				ServerSync.logger.info("Getting the files on the server...");
				oos.writeObject(ServerSyncConfig.SECURE_RECURSIVE);
				oos.flush();
				// read the server response message
				ArrayList<String> fileTree = new ArrayList<String>();
				fileTree = (ArrayList<String>) ois.readObject();
				ServerSync.logger.info(fileTree);

				SyncClient.updateScreenWorking(4, "Got filetree from server...");

				// get all the files at home so we can update the progress bar
				ArrayList<String> allList = new ArrayList<String>();
				allList.addAll(dirContents("./mods"));
				allList.addAll(dirContents("./config"));

				SyncClient.updateScreenWorking(5, "Got filetree from client...");

				ServerSync.logger.info("Ignoring: " + ServerSyncConfig.IGNORE_LIST);

				// run calculations to figure out how big the bar is
				float numberOfFiles = allList.size() + fileTree.size();
				float percentScale = numberOfFiles / 92;
				float currentPercent = 0;

				for (String singleFile : fileTree) {
					// Update status
					currentPercent++;

					// Readable/Usable path saves resources instead of multiple
					// string method calls
					String rPath = singleFile.replace("\\", "/");

					// Get file at rPath location
					File currentFile = new File(rPath);
					// Get mod/config's name (last entry in path series)
					String fileName = currentFile.getName();

					SyncClient.updateScreenWorking((int) (5 + (currentPercent / percentScale)),
							"Checking server's " + rPath);
					
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
								ServerSync.logger.info("Ignoring: " + fileName);

								// Exists but dosen't match
							} else {
								ServerSync.logger.info(fileName + " Does not match... Updating...");
								ServerSync.logger.info("Server Checksum: " + serverChecksum);
								ServerSync.logger.info("Our Checksum: " + Md5.md5String(currentFile));
								SyncClient.updateScreenWorking((int) (5 + (currentPercent / percentScale)),
										"Updating " + fileName);

								updateFile(rPath, currentFile);
							}

							// Matches
						} else {
							ServerSync.logger.info("We have a match! " + fileName);
						}
					} else {
						if (ServerSyncConfig.IGNORE_LIST.contains(rPath)) {
							ServerSync.logger.info("Ignoring: " + fileName);
							
							// Dosen't exist
						} else {
							ServerSync.logger.info(rPath + " Does not exist... Updating...");
							SyncClient.updateScreenWorking((int) (5 + (currentPercent / percentScale)),
									"Updating " + fileName);

							updateFile(rPath, currentFile);
						}
					}
				}

				for (String singleFile : allList) {
					currentPercent++;
					String rPath = singleFile.replace('\\', '/');
					String filePathSaveable = rPath.replaceFirst(".", "").replace("/", "_$_");
					
					File fileToDelete = new File(rPath);
					String fileName = fileToDelete.getName();

					SyncClient.updateScreenWorking((int) (5 + (currentPercent / percentScale)),
							"Checking client's " + rPath);

					ServerSync.logger.info("Checking client's files against the server's...");
					oos.writeObject(ServerSyncConfig.SECURE_EXISTS);
					oos.flush();
					oos.writeObject(rPath);
					oos.flush();

					// check for files that need to be deleted
					String doesExist = (String) ois.readObject();

					if (ServerSyncConfig.IGNORE_LIST.contains(rPath)) {
						ServerSync.logger.info("Ignoring: " + fileName);
					} else {
						// Not present in server list
						if (doesExist.equalsIgnoreCase("false")) {
							ServerSync.logger.info(rPath + " Does not match... Deleting...");
							SyncClient.updateScreenWorking((int) (5 + (currentPercent / percentScale)),
									"Deleting client's " + fileName);

							// File fails to delete
							if (!fileToDelete.delete()) {
								ServerSync.logger.info("Failed to delete " + fileName);
								
								fileToDelete.deleteOnExit(); // For if it gets fixed, will cause exit code to not be needed
								// exit code will not break if this manages to delete the file
								
								File referenceFile = new File("mods/serversync_delete/" + filePathSaveable);
								ServerSync.logger.info("Attempting to create reference to " + fileName + " in: "
										+ referenceFile.getAbsolutePath());

								referenceFile.getParentFile().mkdirs();
								FileOutputStream wr = new FileOutputStream(referenceFile);
								wr.write("D".getBytes(), 0, 1);
								wr.flush();
								wr.close();
							}
							updateHappened = true;
						}
					}
				}
				// I found that these lines are not needed since the client
				// updates server sync anyway.
				// ServerSyncRegistry.config.getCategory("StorageVariables").get("LAST_UPDATE").set(lastUpdate);
				// ServerSyncRegistry.config.save();
			} else {
				SyncClient.updateScreenWorking(50, "No Updates Needed :D");
				ServerSync.logger.info("No Updates Needed");
			}

			SyncClient.updateScreenWorking(98, "Telling Server to Exit...");

			ServerSync.logger.info("Update Complete! Have a nice day!");
			oos.writeObject(ServerSyncConfig.SECURE_EXIT);
			oos.flush();
		} catch (Exception e) {
			ServerSync.logger.error("Exception caught! - " + e);
			e.printStackTrace();
			errorInUpdates = true;
		} finally {
			try {
				SyncClient.updateScreenWorking(99, "Closing connections...");

				if (oos != null)
					oos.close();
				if (ois != null)
					ois.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				ServerSync.logger.error("Exception caught! - " + e);
				e.printStackTrace();
				errorInUpdates = true;
			} // close resources here!
			ServerSync.logger.info("All of serversync's sockets to the server have been closed.");
		}

		SyncClient.updateScreenWorking(100, "Finished!");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			ServerSync.logger.error("Exception caught! - " + e);
			e.printStackTrace();
		}
		checkFinished = true;
		return;
	}

	private static ArrayList<String> dirContents(String dir) {
		ServerSync.logger.info("Getting all of " + dir.replace('\\', '/') + "'s folder contents");
		File f = new File(dir);
		File[] files = f.listFiles();
		ArrayList<String> dirList = new ArrayList<String>();
		// Loop through all the directories and only add to the list if it's a
		// file
		for (File file : files) {
			if (file.isDirectory()) {
				dirList.addAll(dirContents(file.getPath()));
			} else {
				dirList.add(file.toString());
			}
		}
		return dirList;
	}

	private static void reinitConn() throws Exception {
		ServerSync.logger.info("Reinitializing the connection...");
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
		ServerSync.logger.info("Sending requests to Socket Server...");
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

}
