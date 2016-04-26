package com.superzanti.serversync.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.superzanti.serversync.ClientWorker;
import com.superzanti.serversync.SyncConfig;

import runme.Main;

/**
 * Interacts with a server running serversync
 * 
 * @author Rheimus
 *
 */
public class Server {

	public final String IP_ADDRESS;
	public final int PORT;
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private Socket clientSocket = null;
	private InetAddress host = null;
	private Logger logs;

	public Server(ClientWorker caller, String ip, int port) {
		IP_ADDRESS = ip;
		PORT = port;
		logs = caller.getLogger();
	}

	public boolean connect() throws IOException {
		host = InetAddress.getByName(SyncConfig.SERVER_IP);
		logs.updateLogs("Establishing a socket connection to the server...", Logger.FULL_LOG);

		clientSocket = new Socket();
		logs.updateLogs("< Connecting to server >");

		clientSocket.connect(new InetSocketAddress(host.getHostName(), SyncConfig.SERVER_PORT), 5000);

		// write to socket using ObjectOutputStream
		logs.updateLogs("Creating input/output streams...", Logger.FULL_LOG);
		oos = new ObjectOutputStream(clientSocket.getOutputStream());
		ois = new ObjectInputStream(clientSocket.getInputStream());

		return true;
	}

	public void exit() throws IOException {
		logs.updateLogs("Telling server to exit...", Logger.FULL_LOG);
		oos.writeObject(SyncConfig.SECURE_EXIT);
		oos.flush();
	}

	public boolean close() {
		logs.updateLogs("Closing connections...", Logger.FULL_LOG);
		try {
			if (oos != null)
				oos.close();
			if (ois != null)
				ois.close();
			if (clientSocket != null && !clientSocket.isClosed())
				clientSocket.close();
		} catch (IOException e) {
			logs.updateLogs("Exception caught! - " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}
		logs.updateLogs("All of serversync's sockets to the server have been closed.", Logger.FULL_LOG);
		return true;
	}

	public boolean reinitConnection() {
		logs.updateLogs("Reinitializing the connection...", Logger.FULL_LOG);
		try {
			oos.flush();
			// close our resources and set values to null
			if (oos != null)
				oos.close();
			if (ois != null)
				ois.close();
			if (clientSocket != null && !clientSocket.isClosed())
				clientSocket.close();
			clientSocket = null;
			oos = null;
			ois = null;
		} catch (IOException e) {
			logs.updateLogs("Failed to reset streams: " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}

		clientSocket = new Socket();
		try {
			clientSocket.connect(new InetSocketAddress(host.getHostName(), SyncConfig.SERVER_PORT), 5000);
		} catch (IOException e) {
			logs.updateLogs("Could not connect to server at: " + IP_ADDRESS + ":" + PORT);
			return false;
		}

		logs.updateLogs("Sending requests to socket server...", Logger.FULL_LOG);
		try {
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ois = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			logs.updateLogs("Failed to obtain streams: " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean isUpdateNeeded(List<SyncFile> clientMods) {
		try {
			// TODO check last updated information

			oos.writeObject(SyncConfig.SECURE_CHECKMODS);
			oos.flush();
			// List of mod names
			ArrayList<String> serverModNames = (ArrayList<String>) ois.readObject();
			ArrayList<String> clientModNames = SyncFile.listModNames(clientMods);

			// Remove ignored mods from mod list
			logs.updateLogs("Syncable client mods are: " + clientModNames.toString(), Logger.FULL_LOG);
			logs.updateLogs("Syncable server mods are: " + serverModNames.toString(), Logger.FULL_LOG);

			serverModNames.removeAll(clientModNames);

			if (serverModNames.size() == 0) {
				return false;
			}
		} catch (Exception e) {
			logs.updateLogs("Failed to get update information: " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<SyncFile> getFiles() throws IOException {
		oos.writeObject(SyncConfig.SECURE_RECURSIVE);
		oos.flush();

		try {
			ArrayList<SyncFile> serverMods = new ArrayList<SyncFile>();
			serverMods = (ArrayList<SyncFile>) ois.readObject();
			logs.updateLogs("Recieved server file tree", Logger.FULL_LOG);

			return serverMods;
		} catch (ClassNotFoundException e) {
			logs.updateLogs("Failed to read class: " + e.getMessage(), Logger.FULL_LOG);
		}
		return null;
	}

	public boolean getConfig() throws IOException {
		oos.writeObject(SyncConfig.GET_CONFIG);
		oos.flush();

		Path config = Paths.get("../config/serversync.cfg");
		Files.createDirectories(config.getParent());

		FileOutputStream wr = new FileOutputStream(config.toFile());

		byte[] outBuffer = new byte[clientSocket.getReceiveBufferSize()];
		int bytesReceived = 0;

		while ((bytesReceived = ois.read(outBuffer)) > 0) {
			wr.write(outBuffer, 0, bytesReceived);
		}
		wr.flush();
		wr.close();
		reinitConnection();

		logs.updateLogs("Sucessfully updated config");
		logs.updateLogs("Reloading config");
		SyncConfig.getServerDetailsDirty(config);
		return true;
	}

	public boolean modExists(SyncFile mod) throws IOException {
		oos.writeObject(SyncConfig.SECURE_EXISTS);
		oos.flush();
		oos.writeObject(mod.fileName);
		oos.flush();

		boolean modExists = ois.readBoolean();

		return modExists;
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
	public boolean updateFile(String filePath, File currentFile) throws IOException, Exception {
		oos.writeObject(Main.SECURE_FILESIZE);
		oos.flush();
		oos.writeObject(filePath);
		oos.flush();
		/*
		 * Path root = Paths.get("../"); Path relPath =
		 * root.relativize(currentFile.toPath()); currentFile =
		 * relPath.toFile();
		 */

		// TODO update to NIO

		long fileSize = 0l;
		boolean gotFileSize = false;

		try {
			fileSize = ois.readLong();
			gotFileSize = true;
		} catch (Exception e) {
			System.out.println("Could not get file size");
		}

		oos.writeObject(SyncConfig.SECURE_UPDATE);
		oos.flush();
		oos.writeObject(filePath);
		oos.flush();

		currentFile.getParentFile().mkdirs();
		FileOutputStream wr = new FileOutputStream(currentFile);

		byte[] outBuffer = new byte[clientSocket.getReceiveBufferSize()];
		int bytesReceived = 0;

		double progress = 0;
		double byteP = 0;
		double factor = 0;

		while ((bytesReceived = ois.read(outBuffer)) > 0) {
			if (gotFileSize) {
				byteP++;
				factor = fileSize / bytesReceived;
				progress = Math.ceil(byteP / factor * 100);
			}
			wr.write(outBuffer, 0, bytesReceived);
			Main.updateText("<" + (int) progress + "%> Updating " + currentFile.getName());
		}
		wr.flush();
		wr.close();
		reinitConnection();

		logs.updateLogs("Sucessfully updated " + currentFile.getName());
		return true;
	}
}
