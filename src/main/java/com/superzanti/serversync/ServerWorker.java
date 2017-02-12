package com.superzanti.serversync;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import com.superzanti.serversync.util.Md5;
import com.superzanti.serversync.util.ServerTimeout;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EErrorType;
import com.superzanti.serversync.util.enums.EServerMessage;
import com.superzanti.serversync.util.errors.MessageError;
import com.superzanti.serversync.util.errors.UnknownMessageError;

import runme.Main;

/**
 * This worker handles requests from the client continuously until told to exit using SECURE_EXIT
 * These workers are assigned per socket connection i.e. one per client
 * 
 * @author superzanti
 */
public class ServerWorker implements Runnable {

	private Socket clientsocket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;

	private EnumMap<EServerMessage, String> messages;
	
	private Date clientConnectionStarted;
	private DateFormat dateFormatter;
	private Timer timeout;
	
	protected ServerWorker(Socket socket, ServerSocket theServer, EnumMap<EServerMessage, String> comsMessages){
		clientsocket = socket;
		messages = comsMessages;
		clientConnectionStarted = new Date();
		dateFormatter = DateFormat.getDateInstance();
		timeout = new Timer();
		
		System.out.println("Connection established with " + clientsocket + dateFormatter.format(clientConnectionStarted));
		System.out.println(ServerSetup.directories);
	}

	@Override
	public void run() {
		try {
			ois = new ObjectInputStream(clientsocket.getInputStream());
			oos = new ObjectOutputStream(clientsocket.getOutputStream());
			oos.flush();
		} catch (IOException e) {
			System.out.println("Failed to create client streams");
			e.printStackTrace();
		}

		while(true) {
			
			if (clientsocket.isClosed()) {
				return;
			}

			String message = null;
			try {
				timeout = new Timer(true);
				timeout.schedule(new ServerTimeout(this), 60000);
				System.out.println("Waiting for message on socket, timeout started");
				message = (String) ois.readObject();
				System.out.println("Recieved message from: " + clientsocket.getInetAddress());
				timeout.cancel();
				timeout.purge();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SocketException e) {
				break;
				// client timed out
			} catch (EOFException e) { 
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (message == null) {
				continue;
			}
			
			try {
				if(message.equals(Main.HANDSHAKE)) {
					System.out.println("Sending coms messages");
					oos.writeObject(messages);
					oos.flush();
					continue;
				}
				
				if (!messages.containsValue(message)) {
					try {
						System.out.println("Unknown message recieved from: " + clientsocket.getInetAddress());
						oos.writeObject(new UnknownMessageError(message));
						oos.flush();
					} catch (IOException e) {
						System.out.println("Failed to write error to client");
						e.printStackTrace();
					}
					timeout = new Timer();
					timeout.schedule(new ServerTimeout(this) , 5000);
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.INFO_LAST_UPDATE))) {
					System.out.println("Sending last updated timestamp");
					oos.writeObject(Main.CONFIG.LAST_UPDATE);
					oos.flush();
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.UPDATE_NEEDED))) {
					System.out.println("Sending list of syncable mods");
					ArrayList<String> serverModList = SyncFile.listModNames(ServerSetup.allMods);
					
					// Remove client only mods and server only mods from comparison
					serverModList.removeAll(new ArrayList<String>(Main.CONFIG.MOD_IGNORE_LIST));
					
					System.out.println("Syncable mods are: " + serverModList.toString());
					oos.writeObject(serverModList);
					oos.flush();
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.FILE_GET_LIST))) {
					System.out.println("Sending servers file list");
					oos.writeObject(ServerSetup.allMods);
					oos.flush();
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.UPDATE_GET_SYNCABLE_DIRECTORIES))) {
					System.out.println("Sending list of syncable directories: " + ServerSetup.directories);
					oos.writeObject(ServerSetup.directories);
					oos.flush();
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.FILE_COMPARE))) {
					System.out.println("Comparing clients file against server");
					File theFile;
					try {
						theFile = (File) ois.readObject();
						String serverChecksum = Md5.md5String(theFile);
						oos.writeObject(serverChecksum);
						oos.flush();
					} catch (ClassNotFoundException e) {
						System.out.println("Failed to read object from client");
						e.printStackTrace();
						oos.writeObject(new MessageError("Failed to read file", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.UPDATE_GET_CLIENT_ONLY_FILES))) {
					System.out.println("Sending client only file list");
					oos.writeObject(ServerSetup.clientMods);
					oos.flush();
					continue;
				}
				
				// Main file update message
				if(message.equals(messages.get(EServerMessage.UPDATE))) {
					System.out.println("Writing file to client...");
					
					
					String filePathName;
					try {
						filePathName = (String) ois.readObject();
						File f = new File(filePathName.replace("\\", "/"));
						byte[] buff = new byte[clientsocket.getSendBufferSize()];
						int bytesRead = 0;
						InputStream in = new FileInputStream(f);
						while((bytesRead = in.read(buff))>0) {
							//oos.writeObject("BLOB");
							oos.write(buff,0,bytesRead);
						}
						in.close();
						oos.flush();
						//oos.writeObject("EOF");
						System.out.println("Finished writing file to client");
						
					} catch (ClassNotFoundException e) {
						System.out.println("Failed to read object from client");
						e.printStackTrace();
						oos.flush();
						oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.FILE_GET_CONFIG))) {
					System.out.println("Sending config info to client...");
					HashMap<String, List<String>> rules = new HashMap<String, List<String>>();
					rules.put("ignore", Main.CONFIG.MOD_IGNORE_LIST);
					rules.put("include", Main.CONFIG.CONFIG_INCLUDE_LIST);
					//TODO add security info in transfer
					oos.writeObject(rules);
					oos.flush();
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.INFO_GET_FILESIZE))) {
					System.out.println("Writing filesize to client...");
					
					String theFile;
					try {
						theFile = (String) ois.readObject();
						Path p = Paths.get(theFile.replace("\\", "/"));
						oos.writeLong(Files.size(p));
						oos.flush();
					} catch (ClassNotFoundException e) {
						System.out.println("Failed to read object from client");
						e.printStackTrace();
						oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}
				
				if(message.equals(messages.get(EServerMessage.FILE_EXISTS))) {
					String theMod;
					try {
						theMod = (String) ois.readObject();
						boolean exists = false;
						for(SyncFile m : ServerSetup.allMods) {
							if (m.fileName.equals(theMod)) {
								exists = true;
								break;
							}
						}
						if (!exists) {
							for(SyncFile m : ServerSetup.clientMods) {
								if (m.fileName.equals(theMod)) {
									exists = true;
									break;
								}
							}
						}
						if(exists) {
							oos.writeBoolean(true);
							oos.flush();
						}
						else {
							oos.writeBoolean(false);
							oos.flush();
						}
					} catch (ClassNotFoundException e) {
						System.out.println("Failed to read object from client");
						e.printStackTrace();
						oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}
			} catch(SocketException e) { 
				System.out.println("Client socket colsed by timeout");
				break;
			} catch(IOException e) {
				System.out.println("Failed to write to client stream");
				e.printStackTrace();
				break;
			}

			if(message.equals(messages.get(EServerMessage.EXIT))) {
				break;
			}
		}
		System.out.println("Closing connection with: " + clientsocket);
		return; // End thread
	}
	
	public void shutdown() {
		try {
			System.out.println("Client connection timed out, closing socket");
			clientsocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
