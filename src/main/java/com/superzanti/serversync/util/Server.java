package com.superzanti.serversync.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
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
			con.updateText(Main.strings.getString("connection_failed_host") + ": " + IP_ADDRESS);
			return false;
		}

		logs.updateLogs(Main.strings.getString("connection_attempt_server"), Logger.FULL_LOG);
		clientSocket = new Socket();

		logs.updateLogs("< " + Main.strings.getString("connection_message") + " >");
		try {
			clientSocket.connect(new InetSocketAddress(host.getHostName(), PORT), 5000);
		} catch (IOException e) {
			con.updateText(Main.strings.getString("connection_failed_server") + ": " + IP_ADDRESS + ":" + PORT);
			return false;
		}

		// write to socket using ObjectOutputStream
		logs.updateLogs(Main.strings.getString("debug_IO_streams"), Logger.FULL_LOG);
		try {
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ois = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			logs.updateLogs(Main.strings.getString("debug_IO_streams_failed"), Logger.FULL_LOG);
			return false;
		}

		return true;
	}
	
	/**
	 * Gets the set of directories that this server wishes to sync
	 * @return A List of syncable directories or null if directories could not be accessed
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getSyncableDirectories() {
		try {
			oos.writeObject(SyncConfig.MESSAGE_GET_SYNCABLE_DIRECTORIES);
			oos.flush();
		} catch (IOException e) {
			logs.updateLogs("Failed to write object (" + SyncConfig.MESSAGE_GET_SYNCABLE_DIRECTORIES + ") to output stream", Logger.FULL_LOG);
		}
		
		ArrayList<String> dirs = null;
		try {
			dirs = (ArrayList<String>) ois.readObject();
		} catch (ClassNotFoundException e) {
			logs.updateLogs("Failed to read class of streamed object: " + e.getMessage(), Logger.FULL_LOG);
		} catch (IOException e) {
			logs.updateLogs("Failed to access input stream for syncable directories: " + e.getMessage(), Logger.FULL_LOG);
		}
		
		
		return dirs;
	}

	/**
	 * Terminates the listener thread on the server for this client
	 * @throws IOException
	 */
	public void exit() {
		logs.updateLogs(Main.strings.getString("debug_server_exit"), Logger.FULL_LOG);
		
		try {
			oos.writeObject(SyncConfig.MESSAGE_SERVER_EXIT);
			oos.flush();
		} catch(IOException e) {
			logs.updateLogs("Failed to write object (" + SyncConfig.MESSAGE_SERVER_EXIT + ") to client output stream", Logger.FULL_LOG);
		}
	}

	/**
	 * Releases resources related to this server instance, MUST call this when interaction is finished if a server is opened
	 * @return
	 */
	public boolean close() {
		logs.updateLogs(Main.strings.getString("debug_server_close"), Logger.FULL_LOG);
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
		logs.updateLogs(Main.strings.getString("debug_server_close_success"), Logger.FULL_LOG);
		return true;
	}

	public boolean reinitConnection() {
		logs.updateLogs(Main.strings.getString("debug_server_reconnect"), Logger.FULL_LOG);
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
			logs.updateLogs(Main.strings.getString("debug_server_reconnect_failed") + ": " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}

		clientSocket = new Socket();
		try {
			clientSocket.connect(new InetSocketAddress(host.getHostName(), SyncConfig.SERVER_PORT), 5000);
		} catch (IOException e) {
			logs.updateLogs(Main.strings.getString("connection_failed_server") + ": " + IP_ADDRESS + ":" + PORT);
			return false;
		}

		logs.updateLogs(Main.strings.getString("debug_IO_streams"), Logger.FULL_LOG);
		try {
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ois = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			logs.updateLogs(Main.strings.getString("debug_IO_streams_failed") + ": " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean isUpdateNeeded(List<SyncFile> clientMods) {
		try {
			// TODO check last updated information
			
			System.out.println("Sending update check to server");
			oos.writeObject(SyncConfig.MESSAGE_UPDATE_NEEDED);
			oos.flush();
			
			// List of mod names
			System.out.println("Reading data from server");
			ArrayList<String> serverModNames = (ArrayList<String>) ois.readObject();
			System.out.println("reading names from clientMods");
			ArrayList<String> clientModNames = SyncFile.listModNames(clientMods);
			System.out.println("finished reading names" + clientModNames);
			
			// Remove client only mods and other user ignored files from comparison list
			clientModNames.removeAll(new ArrayList<String>(SyncConfig.IGNORE_LIST));

			logs.updateLogs(Main.strings.getString("info_syncable_client") + ": " + clientModNames.toString(), Logger.FULL_LOG);
			logs.updateLogs(Main.strings.getString("info_syncable_server") + ": " + serverModNames.toString(), Logger.FULL_LOG);
			
			ArrayList<String> _SMNC = (ArrayList<String>) serverModNames.clone();
			ArrayList<String> _CMNC = (ArrayList<String>) clientModNames.clone();
			
			_SMNC.removeAll(clientModNames);
			_CMNC.removeAll(serverModNames);

			if (_SMNC.size() == 0 && _CMNC.size() == 0) {
				return false;
			}
		} catch (Exception e) {
			logs.updateLogs(Main.strings.getString("update_failed") + ": " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}
		System.out.println("reached end of update needed");
		return true;
	}

	/**
	 * Gets all mods from the server
	 * @return List of SyncFiles or null if files could not be read
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<SyncFile> getFiles() {
		try {
			oos.writeObject(SyncConfig.MESSAGE_GET_FILE_LIST);
			oos.flush();
		} catch (IOException e) {
			logs.updateLogs("Failed to write object (" + SyncConfig.MESSAGE_GET_FILE_LIST + ") to client output stream");
		}

		try {
			ArrayList<SyncFile> serverMods = new ArrayList<SyncFile>();
			serverMods = (ArrayList<SyncFile>) ois.readObject();
			logs.updateLogs(Main.strings.getString("debug_files_server_tree"), Logger.FULL_LOG);

			return serverMods;
		} catch (ClassNotFoundException e) {
			logs.updateLogs("Failed to read class: " + e.getMessage(), Logger.FULL_LOG);
		} catch (IOException e) {
			logs.updateLogs("Failed to read object from client input stream", Logger.FULL_LOG);
		}
		
		return null;
	}
	
	/**
	 * Gets all client-only mods from the server
	 * @return List of SyncFiles or null if file access fails
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<SyncFile> getClientOnlyFiles() {
		try {
			oos.writeObject(Main.SECURE_PUSH_CLIENTMODS);
			oos.flush();
		} catch (IOException e) {
			logs.updateLogs("Failed to write object (" + Main.SECURE_PUSH_CLIENTMODS + ") to client output stream", Logger.FULL_LOG);
		}

		try {
			ArrayList<SyncFile> serverMods = new ArrayList<SyncFile>();
			serverMods = (ArrayList<SyncFile>) ois.readObject();
			logs.updateLogs(Main.strings.getString("debug_files_client_only"), Logger.FULL_LOG);

			return serverMods;
		} catch (ClassNotFoundException e) {
			logs.updateLogs("Failed to read class: " + e.getMessage(), Logger.FULL_LOG);
		} catch (IOException e) {
			logs.updateLogs("Failed to read object from client input stream", Logger.FULL_LOG);
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public boolean getConfig() throws IOException {
		oos.writeObject(SyncConfig.MESSAGE_GET_CONFIG);
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
				logs.updateLogs(Main.strings.getString("info_config_desync"));
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

	public boolean modExists(SyncFile mod) {
		try {
			oos.writeObject(SyncConfig.MESSAGE_FILE_EXISTS);
			oos.flush();
		} catch(IOException e) {
			logs.updateLogs("Failed to write object (" + SyncConfig.MESSAGE_FILE_EXISTS + ") to client output stream", Logger.FULL_LOG);
			return false;
		}
		
		try {			
			oos.writeObject(mod.fileName);
			oos.flush();
		} catch(IOException e) {
			logs.updateLogs("Failed to write object (" + mod.fileName + ") to client output stream", Logger.FULL_LOG);
			return false;
		}

		try {
			return ois.readBoolean();
		} catch (IOException e) {
			logs.updateLogs("Failed to read object from clients input stream: " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean getSecurityDetails() {
		boolean successStatus = true;
		
		try {
			oos.writeObject(SyncConfig.MESSAGE_SEC_HANDSHAKE);
			oos.flush();
		} catch (IOException e) {
			logs.updateLogs("Could not send message: ("+ SyncConfig.MESSAGE_SEC_HANDSHAKE + ") to server", Logger.FULL_LOG);
			successStatus = false;
		}
		
		try {
			HashMap<String,String> security = (HashMap<String,String>)ois.readObject();
			SyncConfig.MESSAGE_CHECK = security.get("SECURE_CHECK");
			SyncConfig.MESSAGE_UPDATE_NEEDED = security.get("SECURE_CHECKMODS");
			SyncConfig.MESSAGE_COMPARE = security.get("SECURE_CHECKSUM");
			SyncConfig.MESSAGE_FILE_EXISTS = security.get("SECURE_EXISTS");
			SyncConfig.MESSAGE_SERVER_EXIT = security.get("SECURE_EXIT");
			SyncConfig.MESSAGE_GET_FILE_LIST = security.get("SECURE_RECURSIVE");
			SyncConfig.MESSAGE_UPDATE = security.get("SECURE_UPDATE");
			logs.updateLogs(Main.strings.getString("info_security_recieved"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			logs.updateLogs(e.getMessage(), Logger.FULL_LOG);
			successStatus = false;
		} catch (IOException e) {
			logs.updateLogs("Failed to read security details object from server input stream", Logger.FULL_LOG);
			successStatus = false;
		}
		
		return successStatus;
	}

	/**
	 * Sends request to server for the file stored at filePath and updates the
	 * current file with the returned data
	 * 
	 * @param filePath
	 *            the location of the file on the server
	 * @param currentFile
	 *            the current file being worked on
	 */
	public boolean updateFile(String filePath, File currentFile) {
		try {
			logs.updateLogs("Fetching file size from server", Logger.FULL_LOG);
			oos.writeObject(Main.SECURE_FILESIZE);
			oos.flush();
		} catch (IOException e) {
			logs.updateLogs("Failed to write object (" + Main.SECURE_FILESIZE + ") to client output stream", Logger.FULL_LOG);
			return false;
		}
		
		try {			
			logs.updateLogs("Sending file path to server", Logger.FULL_LOG);
			oos.writeObject(filePath);
			oos.flush();
		} catch(IOException e) {
			logs.updateLogs("Failed to write object (" + filePath + ") to client output stream", Logger.FULL_LOG);
			return false;
		}
		
		// TODO update to NIO

		long fileSize = 0L;
		boolean gotFileSize = false;
		FileProgress GUIUpdater = new FileProgress();

		try {
			fileSize = ois.readLong();
			gotFileSize = true;
		} catch (IOException e) {
			logs.updateLogs(Main.strings.getString("debug_files_size_failed"), Logger.FULL_LOG);
			return false;
		}

		try {			
			oos.writeObject(SyncConfig.MESSAGE_UPDATE);
			oos.flush();
		} catch(IOException e) {
			logs.updateLogs("Failed to write object (" + SyncConfig.MESSAGE_UPDATE + ") to client output stream", Logger.FULL_LOG);
			return false;
		}
		
		try {			
			oos.writeObject(filePath);
			oos.flush();
		} catch(IOException e) {
			logs.updateLogs("Failed to write object (" + filePath + ") to client output stream");
			return false;
		}

		currentFile.getParentFile().mkdirs();
		
		try {			
			logs.updateLogs("Attempting to write file (" + currentFile + ")", Logger.FULL_LOG);
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
			GUIUpdater.fileFinished();
			wr.flush();
			wr.close();
		} catch(FileNotFoundException e) {
			logs.updateLogs("Failed to create file (" + currentFile + "): " + e.getMessage(), Logger.FULL_LOG);
			return false;
		} catch (SocketException e) {
			logs.updateLogs(e.getMessage());
			return false;
		} catch(IOException e) {
			logs.updateLogs("Failed to read bytes from client input stream", Logger.FULL_LOG);
			return false;
		}
		
		reinitConnection();
		
		logs.updateLogs(Main.strings.getString("update_success") + ": " + currentFile.getName());
		return true;
	}
}
