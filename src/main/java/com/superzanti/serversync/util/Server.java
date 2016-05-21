package com.superzanti.serversync.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.superzanti.serversync.ClientWorker;
import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.gui.Console;
import com.superzanti.serversync.gui.FileProgress;

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

	public boolean connect() {
		Console con = new Console();
		try {
			host = InetAddress.getByName(IP_ADDRESS);
		} catch (UnknownHostException e) {
			con.updateText("Could not connect to host: " + IP_ADDRESS);
			return false;
		}

		logs.updateLogs("Establishing a socket connection to the server...", Logger.FULL_LOG);
		clientSocket = new Socket();

		logs.updateLogs("< Connecting to server >");
		try {
			clientSocket.connect(new InetSocketAddress(host.getHostName(), PORT), 5000);
		} catch (IOException e) {
			con.updateText("Could not connect to server at: " + IP_ADDRESS + ":" + PORT);
			return false;
		}

		// write to socket using ObjectOutputStream
		logs.updateLogs("Creating input/output streams...", Logger.FULL_LOG);
		try {
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ois = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			logs.updateLogs("Failed to obtain input/output streams from client", Logger.FULL_LOG);
			return false;
		}

		return true;
	}

	/**
	 * Terminates the listener thread on the server for this client
	 * @throws IOException
	 */
	public void exit() throws IOException {
		logs.updateLogs("Telling server to exit...", Logger.FULL_LOG);
		oos.writeObject(SyncConfig.SECURE_EXIT);
		oos.flush();
	}

	/**
	 * Releases resources related to this server instance, MUST call this when interaction is finished if a server is opened
	 * @return
	 */
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
			if (SyncConfig.REFUSE_CLIENT_MODS) {
				serverModNames.removeAll(new ArrayList<String>(SyncConfig.IGNORE_LIST));
			}

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
	
	@SuppressWarnings("unchecked")
	public ArrayList<SyncFile> getClientOnlyFiles() throws IOException {
		oos.writeObject(Main.SECURE_PUSH_CLIENTMODS);
		oos.flush();

		try {
			ArrayList<SyncFile> serverMods = new ArrayList<SyncFile>();
			serverMods = (ArrayList<SyncFile>) ois.readObject();
			logs.updateLogs("Recieved client only files", Logger.FULL_LOG);

			return serverMods;
		} catch (ClassNotFoundException e) {
			logs.updateLogs("Failed to read class: " + e.getMessage(), Logger.FULL_LOG);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public boolean getConfig() throws IOException {
		oos.writeObject(SyncConfig.GET_CONFIG);
		oos.flush();
		
		try {
			HashMap<String, List<String>> rules = (HashMap<String, List<String>>) ois.readObject();
			ArrayList<String> ignored = new ArrayList<String>(rules.get("ignore"));
			ArrayList<String> included = new ArrayList<String>(rules.get("include"));
			
			ArrayList<String> myIgnored = new ArrayList<String>(SyncConfig.IGNORE_LIST);
			ArrayList<String> myIncluded = new ArrayList<String>(SyncConfig.INCLUDE_LIST);
			
			ignored.removeAll(myIgnored);
			included.removeAll(myIncluded);
			
			if (!ignored.isEmpty() || !included.isEmpty()) {
				logs.updateLogs("configs out of sync, updating...");
				SyncConfig.IGNORE_LIST.addAll(ignored);
				SyncConfig.INCLUDE_LIST.addAll(included);
				SyncConfig.updateClient();
			}
			return true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public boolean modExists(SyncFile mod) throws IOException {
		oos.writeObject(SyncConfig.SECURE_EXISTS);
		oos.flush();
		oos.writeObject(mod.fileName);
		oos.flush();

		boolean modExists = ois.readBoolean();

		return modExists;
	}
	
	@SuppressWarnings("unchecked")
	public boolean getSecurityDetails() throws IOException {
		oos.writeObject(SyncConfig.SEC_HANDSHAKE);
		oos.flush();
		
		try {
			HashMap<String,String> security = (HashMap<String,String>)ois.readObject();
			SyncConfig.SECURE_CHECK = security.get("SECURE_CHECK");
			SyncConfig.SECURE_CHECKMODS = security.get("SECURE_CHECKMODS");
			SyncConfig.SECURE_CHECKSUM = security.get("SECURE_CHECKSUM");
			SyncConfig.SECURE_EXISTS = security.get("SECURE_EXISTS");
			SyncConfig.SECURE_EXIT = security.get("SECURE_EXIT");
			SyncConfig.SECURE_RECURSIVE = security.get("SECURE_RECURSIVE");
			SyncConfig.SECURE_UPDATE = security.get("SECURE_UPDATE");
			logs.updateLogs("Recieved security details from server");
			return true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
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
		FileProgress GUIUpdater = new FileProgress();

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
			GUIUpdater.updateProgress((int)progress, currentFile.getName());
		}
		GUIUpdater.resetTitle();
		wr.flush();
		wr.close();
		reinitConnection();

		logs.updateLogs("Sucessfully updated " + currentFile.getName());
		return true;
	}
}
