package com.superzanti.serversync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
	
	// These contain abstract paths of files
	private List<String> clientFiles;

	public OfflineClientWorker() {
		
		errorInUpdates = false;
		updateHappened = false;
		checkFinished = false;

	}

	private void updateFile(String filePath, File currentFile) throws IOException, Exception {

		System.out.println("updating");
		System.out.println(currentFile.getPath());
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

	private static void reinitConn() throws Exception {
		Main.updateText("Reinitializing the connection...");
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
		Main.updateText("Sending requests to Socket Server...");
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

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		// use the ip address of the server to get the host
		try {
			host = InetAddress.getByName(ServerSyncConfig.SERVER_IP);
			System.out.println(ServerSyncConfig.SERVER_IP);
		} catch (UnknownHostException e) {
			Main.updateText("Exception caught! - " + e);
			e.printStackTrace();
			errorInUpdates = true;
		}
		socket = null;
		oos = null;
		ois = null;
		
		Main.updateText("Connecting to server...");
		try {
			// establish socket connection to server
			Main.updateText("Establishing a socket connection to the server...");

			// socket = new Socket(host.getHostName(),
			// ServerSyncRegistry.SERVER_PORT);
			socket = new Socket();
			try {
			socket.connect(new InetSocketAddress(host.getHostName(), ServerSyncConfig.SERVER_PORT), 5000);
			} catch (SocketException e) {
				Main.updateText("Could not connect to: " + ServerSyncConfig.SERVER_IP+":"+ServerSyncConfig.SERVER_PORT);
				Thread.sleep(5000);
				errorInUpdates = true;
				return;
			}

			Main.updateText("Socket established...");

			// write to socket using ObjectOutputStream
			Main.updateText("Creating input/output streams...");
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());

			Main.updateText("Checking to see if updates are needed...");
			oos.writeObject(ServerSyncConfig.SECURE_CHECK);
			oos.flush();
			
			String lastUpdate = (String) ois.readObject();
			Main.updateText("Our version is:" + ServerSyncConfig.LAST_UPDATE);
			Main.updateText("The server's version is:" + lastUpdate);

			oos.writeObject(ServerSyncConfig.SECURE_CHECKMODS);
			oos.flush();
			String serverModList = (String) ois.readObject();
			
			// Ignored mod list used for filtering purposes only in next for loop
			//List<String> _clientFiles = PathUtils.fileListDeep("");
			// Mod list clone that ends up being filtered mod list
			clientFiles = PathUtils.fileListDeep(Paths.get("../mods"));
			
			// Remove ignored mods from mod list
			// What is actually used from the ModContainer interface?
			/*
			 * 1) absolute path of mod file, can get manually
			 * seems to only be used for checking weather to run the update or not
			 */
			for (String entry : clientFiles) {
				Path modPath = Paths.get(entry).toRealPath();
				// Should get minecraft root dir
				Path rootPath = Paths.get("../").toAbsolutePath();
				String relativeModPath = "./" + rootPath.relativize(modPath);
				if (ServerSyncConfig.IGNORE_LIST.contains(relativeModPath.replace('\\', '/'))) {
					clientFiles.remove(entry);
					System.out.println("removed " + entry + " from clientFiles");
				}
			}
			
			System.out.println(serverModList);
			
			Main.updateText("Syncable client mods are: " + clientFiles.toString());
			//Thread.sleep(5000);
			Main.updateText("Syncable server mods are: " + serverModList.toString());
			//Thread.sleep(5000); // use to read serverModList
			
			// This probably wont work, need to test what is returned
			// server returns list with {id = value}
			//TODO better system to compare mods
			boolean serverCompatable = serverModList.equals(clientFiles.toString());
			
			if (!serverCompatable) {
				Main.updateText("The mods between server and client are incompatable... Force updating...");
				Main.updateText("Updating serversync config...");
				
				String sConfigPath = "./config/serversync.cfg";
				Path configPath = Paths.get("../config/serversync.cfg");
				
				oos.writeObject(ServerSyncConfig.SECURE_CHECKSUM);
				oos.flush();
				oos.writeObject(sConfigPath);
				oos.flush();
				String serverChecksum = (String) ois.readObject();
				// if the checksums do not match, update the file
				if (!Md5.md5String(configPath.toFile()).equals(serverChecksum)) {
					System.out.println("updating config");
					updateFile(sConfigPath, configPath.toFile());
					// reload config file
					System.out.println("reloading config");
					ServerSyncConfig.getServerDetailsDirty(configPath);
				}
			}

			if (!lastUpdate.equals(ServerSyncConfig.LAST_UPDATE) || !serverCompatable) {

				Main.updateText("Sending requests to Socket Server...");

				// get all files on server
				Main.updateText("Getting the files on the server...");
				oos.writeObject(ServerSyncConfig.SECURE_RECURSIVE);
				oos.flush();
				// read the server response message
				ArrayList<String> fileTree = new ArrayList<String>();
				fileTree = (ArrayList<String>) ois.readObject();
				Main.updateText(fileTree.toString());

				Main.updateText("Got filetree from server...");

				// get all the files at home so we can update the progress bar
				ArrayList<String> allList = new ArrayList<String>();
				allList.addAll(PathUtils.fileListDeep(Paths.get("../mods")));
				allList.addAll(PathUtils.fileListDeep(Paths.get("../config")));

				Main.updateText("Got filetree from client...");

				Main.updateText("Ignoring: " + ServerSyncConfig.IGNORE_LIST);

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

					//TODO add progress tracking
					Main.updateText("Checking server's " + rPath);
					
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
								Main.updateText("Ignoring: " + fileName);

								// Exists but dosen't match
							} else {
								Main.updateText(fileName + " Does not match... Updating...");
								Main.updateText("Server Checksum: " + serverChecksum);
								Main.updateText("Our Checksum: " + Md5.md5String(currentFile));
								Main.updateText("Updating " + fileName);

								updateFile(rPath, currentFile);
							}

							// Matches
						} else {
							Main.updateText("We have a match! " + fileName);
						}
					} else {
						// only need to check for ignore here as we are working on the servers file tree
						if (ServerSyncConfig.IGNORE_LIST.contains(rPath)) {
							Main.updateText("Ignoring: " + fileName);
							
							// Dosen't exist
						} else {
							Main.updateText(rPath + " Does not exist... Updating...");
							Main.updateText("Updating " + fileName);

							updateFile(rPath, currentFile);
						}
					}
					Main.updateProgress((int)(currentPercent / percentScale));
				}

				// Parse clients file tree
				for (String singleFile : allList) {
					currentPercent++;
					String rPath = singleFile.replace('\\', '/');
					String serverReadablePath = rPath.replaceFirst(".", "");
					String filePathSaveable = rPath.replaceFirst(".", "").replace("/", "_$_");
					
					File fileToDelete = new File(rPath);
					String fileName = fileToDelete.getName();

					Main.updateText("Checking client's " + rPath);

					Main.updateText("Checking client's files against the server's...");
					oos.writeObject(ServerSyncConfig.SECURE_EXISTS);
					oos.flush();
					oos.writeObject(serverReadablePath);
					oos.flush();

					// check for files that need to be deleted
					String doesExist = (String) ois.readObject();

					if (ServerSyncConfig.IGNORE_LIST.contains(rPath)) {
						Main.updateText("Ignoring: " + fileName);
					} else {
						// Not present in server list
						if (doesExist.equalsIgnoreCase("false")) {
							Main.updateText(rPath + " Does not match... Deleting...");
							System.out.println(fileName + " deleted");
							Main.updateText("Deleting client's " + fileName);

							// File fails to delete
							if (!fileToDelete.delete()) {
								Main.updateText("Failed to delete " + fileName);
								
								fileToDelete.deleteOnExit(); // For if it gets fixed, will cause exit code to not be needed
								// exit code will not break if this manages to delete the file
								
								File referenceFile = new File("mods/serversync_delete/" + filePathSaveable);
								Main.updateText("Attempting to create reference to " + fileName + " in: "
										+ referenceFile.getAbsolutePath());

								referenceFile.getParentFile().mkdirs();
								FileOutputStream wr = new FileOutputStream(referenceFile);
								wr.write("D".getBytes(), 0, 1);
								wr.flush();
								wr.close();
							}
							updateHappened = true;
						}
						Main.updateProgress((int)(currentPercent / percentScale));
					}
				}
				// I found that these lines are not needed since the client
				// updates server sync anyway.
				// ServerSyncRegistry.config.getCategory("StorageVariables").get("LAST_UPDATE").set(lastUpdate);
				// ServerSyncRegistry.config.save();
			} else {
				Main.updateText("No Updates Needed");
				Thread.sleep(1000);
			}

			Main.updateText("Telling Server to Exit...");

			Main.updateText("Update Complete! Have a nice day!");
			oos.writeObject(ServerSyncConfig.SECURE_EXIT);
			oos.flush();
		} catch (Exception e) {
			Main.updateText("Exception caught! - " + e);
			e.printStackTrace();
			errorInUpdates = true;
		} finally {
			try {
				Main.updateText("Closing connections...");

				if (oos != null)
					oos.close();
				if (ois != null)
					ois.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				Main.updateText("Exception caught! - " + e);
				e.printStackTrace();
				errorInUpdates = true;
			} // close resources here!
			Main.updateText("All of serversync's sockets to the server have been closed.");
			if (errorInUpdates) {
				Main.updateText("Errors occured, please check serversync.cfg has been set up correctly");
			}
		}
		
		try {
			if (!updateHappened) {
				Main.updateText("No update needed");
				Thread.sleep(1000);
				Main.updateText("Finished!");
			} else {
				Main.updateText("Files updated");
				Thread.sleep(1000);
				Main.updateText("Finished!");
			}
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Main.updateText("Exception caught! - " + e);
			e.printStackTrace();
		}
		checkFinished = true;
		return;
	}

}
