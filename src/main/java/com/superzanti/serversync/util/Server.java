package com.superzanti.serversync.util;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

import com.superzanti.serversync.ClientWorker;
import com.superzanti.serversync.gui.FileProgress;
import com.superzanti.serversync.util.enums.EServerMessage;

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
	private EnumMap<EServerMessage, String> SCOMS;

	public Server(ClientWorker caller, String ip, int port) {
		IP_ADDRESS = ip;
		PORT = port;
		logs = caller.getLogger();
	}

	@SuppressWarnings("unchecked")
	public boolean connect() {
		try {
			host = InetAddress.getByName(IP_ADDRESS);
		} catch (UnknownHostException e) {
			logs.updateLogs(Main.strings.getString("connection_failed_host") + ": " + IP_ADDRESS);
			return false;
		}

		logs.updateLogs(Main.strings.getString("connection_attempt_server"), Logger.FULL_LOG);
		clientSocket = new Socket();

		logs.updateLogs("< " + Main.strings.getString("connection_message") + " >");
		try {
			clientSocket.connect(new InetSocketAddress(host.getHostName(), PORT), 5000);
		} catch (IOException e) {
			logs.updateLogs(Main.strings.getString("connection_failed_server") + ": " + IP_ADDRESS + ":" + PORT);
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
		
		try {
			oos.writeObject(Main.HANDSHAKE);
		} catch (IOException e) {
			logs.outputError(Main.HANDSHAKE);
		}
		
		try {
			SCOMS = (EnumMap<EServerMessage, String>) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			logs.inputError(e.getMessage());
		}
		
		System.out.println(SCOMS);

		return true;
	}
	
	/**
	 * Gets the set of directories that this server wishes to sync
	 * @return A List of syncable directories or null if directories could not be accessed
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getSyncableDirectories() {
		String message = SCOMS.get(EServerMessage.UPDATE_GET_SYNCABLE_DIRECTORIES);
		try {
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			logs.updateLogs("Failed to write object (" + message + ") to output stream", Logger.FULL_LOG);
			//TODO handle retrys / stream sanitation
		}
		
		ArrayList<String> dirs = null;
		
		try {
			dirs = (ArrayList<String>) ois.readObject();
			return dirs;
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
	private void exit() {
		String message = SCOMS.get(EServerMessage.EXIT);
		logs.updateLogs(Main.strings.getString("debug_server_exit"), Logger.FULL_LOG);
		
		try {
			oos.writeObject(message);
			oos.flush();
		} catch(IOException e) {
			logs.updateLogs("Failed to write object (" + message + ") to client output stream", Logger.FULL_LOG);
		}
	}

	/**
	 * Releases resources related to this server instance, MUST call this when interaction is finished if a server is opened
	 * @return true if client successfully closes all connections
	 */
	public boolean close() {
		exit();
		logs.updateLogs(Main.strings.getString("debug_server_close"), Logger.FULL_LOG);
		try {
			if (oos != null)
				oos.close();
			if (ois != null)
				ois.close();
			if (clientSocket != null && !clientSocket.isClosed())
				clientSocket.close();
		} catch (IOException e) {
			logs.updateLogs("Failed to close client socket: " + e.getMessage(), Logger.FULL_LOG);
			return false;
		}
		logs.updateLogs(Main.strings.getString("debug_server_close_success"), Logger.FULL_LOG);
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean isUpdateNeeded(List<SyncFile> clientMods) {
		String message = SCOMS.get(EServerMessage.UPDATE_NEEDED);
		try {
			// TODO check last updated information, maybe
			
			System.out.println("Sending update check to server");
			oos.writeObject(message);
			oos.flush();
			
			// List of mod names
			System.out.println("Reading data from server");
			ArrayList<String> serverModNames = (ArrayList<String>) ois.readObject();
			System.out.println("finished reading mod names: " + serverModNames);
			System.out.println("reading names from clientMods");
			ArrayList<String> clientModNames = SyncFile.listModNames(clientMods);
			System.out.println("finished reading clientmod names: " + clientModNames);
			
			// Remove client only mods and other user ignored files from comparison list
			clientModNames.removeAll(new ArrayList<String>(Main.CONFIG.FILE_IGNORE_LIST));

			logs.updateLogs(Main.strings.getString("info_syncable_client") + ": " + clientModNames.toString(), Logger.FULL_LOG);
			logs.updateLogs(Main.strings.getString("info_syncable_server") + ": " + serverModNames.toString(), Logger.FULL_LOG);
			
			ArrayList<String> _SMNC = (ArrayList<String>) serverModNames.clone();
			ArrayList<String> _CMNC = (ArrayList<String>) clientModNames.clone();
			
			_SMNC.removeAll(clientModNames);
			_CMNC.removeAll(serverModNames);
			System.out.println(_SMNC + " | " +_CMNC);

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
		String message = SCOMS.get(EServerMessage.FILE_GET_LIST);
		try {
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			logs.updateLogs("Failed to write object (" + message + ") to client output stream");
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
		String message = SCOMS.get(EServerMessage.UPDATE_GET_CLIENT_ONLY_FILES);
		try {
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			logs.outputError(message);
		}

		try {
			ArrayList<SyncFile> serverMods = new ArrayList<SyncFile>();
			serverMods = (ArrayList<SyncFile>) ois.readObject();
			logs.updateLogs(Main.strings.getString("debug_files_client_only"), Logger.FULL_LOG);

			return serverMods;
		} catch (ClassNotFoundException e) {
			logs.updateLogs("Failed to read class: " + e.getMessage(), Logger.FULL_LOG);
		} catch (IOException e) {
			logs.inputError(e.getMessage());
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public boolean getConfig() {
		String message = SCOMS.get(EServerMessage.FILE_GET_CONFIG);
		try {			
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			logs.outputError(message);
		}
		
		try {
			HashMap<String, List<String>> rules = (HashMap<String, List<String>>) ois.readObject();
			ArrayList<String> ignored = new ArrayList<String>(rules.get("ignore"));
			ArrayList<String> included = new ArrayList<String>(rules.get("include"));
			
			ArrayList<String> myIgnored = new ArrayList<String>(Main.CONFIG.FILE_IGNORE_LIST);
			ArrayList<String> myIncluded = new ArrayList<String>(Main.CONFIG.CONFIG_INCLUDE_LIST);
			
			ignored.removeAll(myIgnored);
			included.removeAll(myIncluded);
			
			if (!ignored.isEmpty() || !included.isEmpty()) {
				logs.updateLogs(Main.strings.getString("info_config_desync"));
				Main.CONFIG.FILE_IGNORE_LIST.addAll(ignored);
				Main.CONFIG.CONFIG_INCLUDE_LIST.addAll(included);
				Main.CONFIG.writeConfigUpdates();
			}
			return true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			logs.inputError(e.getMessage());
			return false;
		}
	}

	public boolean modExists(SyncFile mod) {
		String message = SCOMS.get(EServerMessage.FILE_EXISTS);
		try {
			oos.writeObject(message);
			oos.flush();
		} catch(IOException e) {
			logs.outputError(message);
			return false;
		}
		
		try {			
			oos.writeObject(mod.getFileName());
			oos.flush();
		} catch(IOException e) {
			logs.outputError(mod.getFileName());
			return false;
		}

		try {
			return ois.readBoolean();
		} catch (IOException e) {
			logs.inputError(e.getMessage());
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
	 */
	public boolean updateFile(SyncFile filePath, SyncFile currentFile) {
		String message = SCOMS.get(EServerMessage.INFO_GET_FILESIZE);
		try {
			logs.updateLogs("Fetching file size from server", Logger.FULL_LOG);
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			logs.outputError(message);
			return false;
		}
		
		try {			
			logs.updateLogs("Sending file path to server", Logger.FULL_LOG);
			oos.writeObject(filePath);
			oos.flush();
		} catch(IOException e) {
			logs.outputError(filePath);
			return false;
		}
		
		// TODO update to NIO

		long numberOfBytesToRecieve = 0L;
		FileProgress GUIUpdater = new FileProgress();

		try {
			numberOfBytesToRecieve = ois.readLong();
		} catch (IOException e) {
			logs.updateLogs(Main.strings.getString("debug_files_size_failed"), Logger.FULL_LOG);
			return false;
		}

		message = SCOMS.get(EServerMessage.UPDATE);
		try {			
			oos.writeObject(message);
			oos.flush();
		} catch(IOException e) {
			logs.outputError(message);
			return false;
		}
		
		try {			
			oos.writeObject(filePath);
			oos.flush();
		} catch(IOException e) {
			logs.outputError(filePath);
			return false;
		}
		
		Path pFile = currentFile.getFileAsPath();
		try {			
			Files.createDirectories(pFile.getParent());
		} catch (IOException e) {
			logs.updateLogs("Could not create parent directories for: " + currentFile.getFileName(), Logger.FULL_LOG);
		}
		
		if (Files.exists(pFile)) {
			try {
				Files.delete(pFile);
				Files.createFile(pFile);
			} catch (IOException e) {
				System.out.println("Failed to delete file: " + pFile.getFileName().toString());
			}
		}
		
		try {			
			//TODO NIO this
			logs.updateLogs("Attempting to write file (" + currentFile + ")", Logger.FULL_LOG);
			FileOutputStream wr = new FileOutputStream(currentFile.getFile());
			
			byte[] outBuffer = new byte[clientSocket.getReceiveBufferSize()];
			int bytesReceived = 0;
			long bytesRecievedSoFar = 0L;
			
			double factor = 0;
			
			if(ois.readBoolean()) {
				// Not empty file
				while ((bytesReceived = ois.read(outBuffer)) > 0) {
					bytesRecievedSoFar += bytesReceived;
					factor = (double) bytesRecievedSoFar / numberOfBytesToRecieve;
					System.out.println(factor);
					System.out.println(bytesRecievedSoFar + " / " + numberOfBytesToRecieve);
					wr.write(outBuffer, 0, bytesReceived);
					GUIUpdater.updateProgress((int)Math.ceil(factor * 100), currentFile.getFileName());
					if (factor == 1) {
						break;
					}
				}
			} else {
				System.out.println("Empty file: " + currentFile.getFileName());
			}
			
			GUIUpdater.fileFinished();
			wr.flush();
			wr.close();
			System.out.println("Finished writing file");
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
		
		logs.updateLogs(Main.strings.getString("update_success") + ": " + currentFile.getFileName());
		return true;
	}
}
