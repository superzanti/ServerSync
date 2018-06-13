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
	private EnumMap<EServerMessage, String> SCOMS;

	public Server(ClientWorker caller, String ip, int port) {
		IP_ADDRESS = ip;
		PORT = port;
	}

	@SuppressWarnings("unchecked")
	public boolean connect() {
		try {
			host = InetAddress.getByName(IP_ADDRESS);
		} catch (UnknownHostException e) {
			Logger.error(Main.strings.getString("connection_failed_host") + ": " + IP_ADDRESS);
			return false;
		}

		Logger.debug(Main.strings.getString("connection_attempt_server"));
		clientSocket = new Socket();

		Logger.log("< " + Main.strings.getString("connection_message") + " >");
		try {
			clientSocket.connect(new InetSocketAddress(host.getHostName(), PORT), 5000);
		} catch (IOException e) {
			Logger.error(Main.strings.getString("connection_failed_server") + ": " + IP_ADDRESS + ":" + PORT);
			AutoClose.closeResource(clientSocket);
			return false;
		}

		// write to socket using ObjectOutputStream
		Logger.debug(Main.strings.getString("debug_IO_streams"));
		try {
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ois = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			Logger.debug(Main.strings.getString("debug_IO_streams_failed"));
			AutoClose.closeResource(clientSocket);
			return false;
		}
		
		try {
			oos.writeObject(Main.HANDSHAKE);
		} catch (IOException e) {
			Logger.outputError(Main.HANDSHAKE);
		}
		
		try {
			SCOMS = (EnumMap<EServerMessage, String>) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Logger.inputError(e.getMessage());
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
			Logger.debug("Failed to write object (" + message + ") to output stream");
			//TODO handle retrys / stream sanitation
		}
		
		ArrayList<String> dirs = null;
		
		try {
			dirs = (ArrayList<String>) ois.readObject();
			return dirs;
		} catch (ClassNotFoundException e) {
			Logger.debug("Failed to read class of streamed object: " + e.getMessage());
		} catch (IOException e) {
			Logger.debug("Failed to access input stream for syncable directories: " + e.getMessage());
		}
		
		return dirs;
	}

	/**
	 * Terminates the listener thread on the server for this client
	 */
	private void exit() {
		if (SCOMS == null) {
			// NO server messages set up, server must have not connected at this point
			return;
		}
		String message = SCOMS.get(EServerMessage.EXIT);
		Logger.debug(Main.strings.getString("debug_server_exit"));
		
		try {
			oos.writeObject(message);
			oos.flush();
		} catch(IOException e) {
			Logger.debug("Failed to write object (" + message + ") to client output stream");
		}
	}

	/**
	 * Releases resources related to this server instance, MUST call this when interaction is finished if a server is opened
	 * @return true if client successfully closes all connections
	 */
	public boolean close() {
		exit();
		Logger.debug(Main.strings.getString("debug_server_close"));
		try {
			if (clientSocket != null && !clientSocket.isClosed())
				clientSocket.close();
		} catch (IOException e) {
			Logger.debug("Failed to close client socket: " + e.getMessage());
			return false;
		}
		Logger.debug(Main.strings.getString("debug_server_close_success"));
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
			if (Main.CONFIG.REFUSE_CLIENT_MODS) {				
				oos.writeInt(2);
			} else {
				oos.writeInt(3);
			}
			oos.flush();
			
			// List of mod names
			ArrayList<String> serverModNames = (ArrayList<String>) ois.readObject();
			ArrayList<String> clientModNames = SyncFile.listModNames(clientMods);

			Logger.debug(Main.strings.getString("info_syncable_client") + ": " + clientModNames.toString());
			Logger.debug(Main.strings.getString("info_syncable_server") + ": " + serverModNames.toString());
			
			ArrayList<String> _SMNC = (ArrayList<String>) serverModNames.clone();
			ArrayList<String> _CMNC = (ArrayList<String>) clientModNames.clone();
			
			_SMNC.removeAll(clientModNames);
			_CMNC.removeAll(serverModNames);
			Logger.debug("Server: " + _SMNC + " | Client: " +_CMNC);

			if (_SMNC.size() == 0 && _CMNC.size() == 0) {
				return false;
			}
		} catch (Exception e) {
			Logger.debug(Main.strings.getString("update_failed") + ": " + e.getMessage());
			return false;
		}
		
		Logger.debug("reached end of Server.isUpdateNeeded()");
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
			Logger.debug(e);
		}

		try {
			ArrayList<SyncFile> serverMods = (ArrayList<SyncFile>) ois.readObject();
			Logger.debug(Main.strings.getString("debug_files_server_tree"));

			return serverMods;
		} catch (ClassNotFoundException e) {
			Logger.debug("Failed to read class: " + e.getMessage());
		} catch (IOException e) {
			Logger.debug(e);
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
			Logger.debug(e);
		}

		try {
			ArrayList<SyncFile> serverMods = (ArrayList<SyncFile>) ois.readObject();
			Logger.debug(Main.strings.getString("debug_files_client_only"));

			return serverMods;
		} catch (ClassNotFoundException | IOException e) {
			Logger.debug(e);
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
			Logger.debug(e);
		}
		
		try {
			HashMap<String, List<String>> rules = (HashMap<String, List<String>>) ois.readObject();
			ArrayList<String> ignored = new ArrayList<>(rules.get("ignore"));
			ArrayList<String> included = new ArrayList<>(rules.get("include"));
			
			ArrayList<String> myIgnored = new ArrayList<>(Main.CONFIG.FILE_IGNORE_LIST);
			ArrayList<String> myIncluded = new ArrayList<>(Main.CONFIG.CONFIG_INCLUDE_LIST);
			
			ignored.removeAll(myIgnored);
			included.removeAll(myIncluded);
			
			if (!ignored.isEmpty() || !included.isEmpty()) {
				Logger.log(Main.strings.getString("info_config_desync"));
				Main.CONFIG.FILE_IGNORE_LIST.addAll(ignored);
				Main.CONFIG.CONFIG_INCLUDE_LIST.addAll(included);
				Main.CONFIG.writeConfigUpdates();
			}
			return true;
		} catch (ClassNotFoundException | IOException e) {
			Logger.debug(e);
			return false;
		}
	}

	public boolean modExists(SyncFile mod) {
		String message = SCOMS.get(EServerMessage.FILE_EXISTS);
		try {
			oos.writeObject(message);
			oos.flush();
			if (Main.CONFIG.REFUSE_CLIENT_MODS) {				
				oos.writeInt(2);
			} else {
				oos.writeInt(3);
			}
			oos.flush();
		} catch(IOException e) {
			Logger.debug(e);
			return false;
		}
		
		try {			
			oos.writeObject(mod);
			oos.flush();
		} catch(IOException e) {
			Logger.debug(mod.getFileName());
			Logger.debug(e);
			return false;
		}

		try {
			return ois.readBoolean();
		} catch (IOException e) {
			Logger.debug(e);
			return false;
		}
	}

	/**
	 * Sends request to server for the file stored at filePath and updates the
	 * current file with the returned data
	 * 
	 */
	public boolean updateFile(SyncFile serverFile, SyncFile clientFile) {
		String message = SCOMS.get(EServerMessage.INFO_GET_FILESIZE);
		try {
			Logger.debug("Fetching file size from server");
			oos.writeObject(message);
			oos.flush();
		} catch (IOException e) {
			Logger.outputError(message);
			return false;
		}
		
		try {			
			Logger.debug("Sending file path to server");
			oos.writeObject(serverFile);
			oos.flush();
		} catch(IOException e) {
			Logger.outputError(serverFile);
			return false;
		}
		
		// TODO update to NIO

		long numberOfBytesToRecieve = 0L;
		FileProgress GUIUpdater = new FileProgress();

		try {
			numberOfBytesToRecieve = ois.readLong();
		} catch (IOException e) {
			Logger.debug(Main.strings.getString("debug_files_size_failed"));
			Logger.debug(e);
			return false;
		}

		message = SCOMS.get(EServerMessage.UPDATE);
		try {			
			oos.writeObject(message);
			oos.flush();
		} catch(IOException e) {
			Logger.debug(message);
			Logger.debug(e);
			return false;
		}
		
		try {			
			oos.writeObject(serverFile);
			oos.flush();
		} catch(IOException e) {
			Logger.outputError(serverFile);
			Logger.debug(e);
			return false;
		}
		
		Path pFile = clientFile.getFileAsPath();
		try {			
			Files.createDirectories(pFile.getParent());
		} catch (IOException e) {
			Logger.debug("Could not create parent directories for: " + clientFile.getFileName());
			Logger.debug(e);
		}
		
		if (Files.exists(pFile)) {
			try {
				Files.delete(pFile);
				Files.createFile(pFile);
			} catch (IOException e) {
				Logger.debug("Failed to delete file: " + pFile.getFileName().toString());
				Logger.debug(e);
			}
		}
		
		try {			
			//TODO NIO this
			Logger.debug("Attempting to write file (" + clientFile + ")");
			FileOutputStream wr = new FileOutputStream(clientFile.getFile());
			
			byte[] outBuffer = new byte[clientSocket.getReceiveBufferSize()];
			int bytesReceived = 0;
			long bytesRecievedSoFar = 0L;
			
			double factor = 0;
			
			if(ois.readBoolean()) {
				// Not empty file
				while ((bytesReceived = ois.read(outBuffer)) > 0) {
					bytesRecievedSoFar += bytesReceived;
					factor = (double) bytesRecievedSoFar / numberOfBytesToRecieve;
					
					wr.write(outBuffer, 0, bytesReceived);
					GUIUpdater.updateProgress((int)Math.ceil(factor * 100), clientFile.getFileName());
					if (factor == 1) {
						break;
					}
				}
			} else {
				Logger.debug("Empty file: " + clientFile.getFileName());
			}
			
			GUIUpdater.fileFinished();
			wr.flush();
			wr.close();
			Logger.debug("Finished writing file" + clientFile.getFileName());
		} catch(FileNotFoundException e) {
			Logger.debug("Failed to create file (" + clientFile + "): " + e.getMessage());
			Logger.debug(e);
			return false;
		} catch (SocketException e) {
			Logger.log(e.getMessage());
			Logger.debug(e);
			return false;
		} catch(IOException e) {
			Logger.debug(e);
			return false;
		}
		
		Logger.log(Main.strings.getString("update_success") + ": " + clientFile.getFileAsPath().toString());
		return true;
	}
}
