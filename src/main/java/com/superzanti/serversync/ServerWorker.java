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
import com.superzanti.serversync.util.errors.InvalidSyncFileException;
import com.superzanti.serversync.util.errors.MessageError;
import com.superzanti.serversync.util.errors.UnknownMessageError;

import runme.Main;

/**
 * This worker handles requests from the client continuously until told to exit
 * using SECURE_EXIT These workers are assigned per socket connection i.e. one
 * per client
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

	protected ServerWorker(Socket socket, ServerSocket theServer, EnumMap<EServerMessage, String> comsMessages) {
		clientsocket = socket;
		messages = comsMessages;
		clientConnectionStarted = new Date();
		dateFormatter = DateFormat.getDateTimeInstance();
		timeout = new Timer();

		ServerSetup.serverLog.addToConsole("Connection established with " + clientsocket + dateFormatter.format(clientConnectionStarted));
		ServerSetup.serverLog.addToConsole(ServerSetup.directories.toString());
	}

	@Override
	public void run() {
		try {
			ois = new ObjectInputStream(clientsocket.getInputStream());
			oos = new ObjectOutputStream(clientsocket.getOutputStream());
			oos.flush();
		} catch (IOException e) {
			ServerSetup.serverLog.addToConsole("Failed to create client streams");
			e.printStackTrace();
		}

		while (!clientsocket.isClosed()) {
			String message = null;
			try {
				timeout = new Timer(true);
				timeout.schedule(new ServerTimeout(this), 60000);
				message = (String) ois.readObject();
				ServerSetup.serverLog.addToConsole("Recieved message from: " + clientsocket.getInetAddress());
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
				if (message.equals(Main.HANDSHAKE)) {
					ServerSetup.serverLog.addToConsole("Sending coms messages");
					oos.writeObject(messages);
					oos.flush();
					continue;
				}

				if (!messages.containsValue(message)) {
					try {
						ServerSetup.serverLog.addToConsole("Unknown message recieved from: " + clientsocket.getInetAddress());
						oos.writeObject(new UnknownMessageError(message));
						oos.flush();
					} catch (IOException e) {
						ServerSetup.serverLog.addToConsole("Failed to write error to client " + clientsocket);
						e.printStackTrace();
					}
					timeout = new Timer();
					timeout.schedule(new ServerTimeout(this), 5000);
					continue;
				}

				if (message.equals(messages.get(EServerMessage.INFO_LAST_UPDATE))) {
					ServerSetup.serverLog.addToConsole("Sending last updated timestamp");
					oos.writeObject(Main.CONFIG.LAST_UPDATE);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.UPDATE_NEEDED))) {
					int checkLevel = ois.readInt();
					ArrayList<String> serverFileNames = new ArrayList<String>(200);
					if (checkLevel == 3) {
						ServerSetup.serverLog.addToConsole("Client Requested a list of all files");
						serverFileNames.addAll(SyncFile.listModNames(ServerSetup.allFiles));
					} else {
						ServerSetup.serverLog.addToConsole("Client is refusing client only files, sending standard file list");
						serverFileNames.addAll(SyncFile.listModNames(ServerSetup.standardSyncableFiles));
					}
					ServerSetup.serverLog.addToConsole("Sending list of syncable mods");

					serverFileNames.removeAll(new ArrayList<String>(Main.CONFIG.FILE_IGNORE_LIST));

					ServerSetup.serverLog.addToConsole("Syncable mods are: " + serverFileNames.toString());
					oos.writeObject(serverFileNames);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_GET_LIST))) {
					ServerSetup.serverLog.addToConsole("Sending servers file list to " + clientsocket);
					
					oos.writeObject(ServerSetup.standardSyncableFiles);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.UPDATE_GET_SYNCABLE_DIRECTORIES))) {
					ServerSetup.serverLog
							.addToConsole("Sending list of syncable directories: " + ServerSetup.directories);
					oos.writeObject(ServerSetup.directories);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_COMPARE))) {
					ServerSetup.serverLog.addToConsole("Comparing clients file against server " + clientsocket);
					File theFile;
					try {
						theFile = (File) ois.readObject();
						String serverChecksum = Md5.md5String(theFile);
						oos.writeObject(serverChecksum);
						oos.flush();
					} catch (ClassNotFoundException e) {
						ServerSetup.serverLog.addToConsole("Failed to read object from client " + clientsocket);
						e.printStackTrace();
						oos.writeObject(new MessageError("Failed to read file", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}

				if (message.equals(messages.get(EServerMessage.UPDATE_GET_CLIENT_ONLY_FILES))) {
					ServerSetup.serverLog.addToConsole("Sending client only file list");
					oos.writeObject(ServerSetup.clientOnlyFiles);
					oos.flush();
					continue;
				}

				// Main file update message
				if (message.equals(messages.get(EServerMessage.UPDATE))) {

					SyncFile file;
					try {
						//TODO update this to NIO
						file = (SyncFile) ois.readObject();
						File f = file.getFile();
						ServerSetup.serverLog.addToConsole("Writing " + f + " to client " + clientsocket + "...");
						byte[] buff = new byte[clientsocket.getSendBufferSize()];
						int bytesRead = 0;
						InputStream in = new FileInputStream(f);
						if ((bytesRead = in.read(buff)) == -1) {
							// End of file
							oos.writeBoolean(false);
						} else {
							oos.writeBoolean(true);
							oos.write(buff, 0, bytesRead);

							while ((bytesRead = in.read(buff)) > 0) {
								// oos.writeObject("BLOB");
								oos.write(buff, 0, bytesRead);
							}
						}
						in.close();
						oos.flush();
						// oos.writeObject("EOF");
						ServerSetup.serverLog.addToConsole("Finished writing file to client " + clientsocket);

					} catch (ClassNotFoundException e) {
						ServerSetup.serverLog.addToConsole("Failed to read object from client " + clientsocket);
						e.printStackTrace();
						oos.flush();
						oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_GET_CONFIG))) {
					ServerSetup.serverLog.addToConsole("Sending config info to client...");
					HashMap<String, List<String>> rules = new HashMap<String, List<String>>();
					rules.put("ignore", Main.CONFIG.FILE_IGNORE_LIST);
					rules.put("include", Main.CONFIG.CONFIG_INCLUDE_LIST);
					// TODO add security info in transfer
					oos.writeObject(rules);
					oos.flush();
					continue;
				}

				if (message.equals(messages.get(EServerMessage.INFO_GET_FILESIZE))) {
					ServerSetup.serverLog.addToConsole("Writing filesize to client " + clientsocket + "...");

					SyncFile theFile;
					try {
						theFile = (SyncFile) ois.readObject();
						oos.writeLong(Files.size(theFile.getFileAsPath()));
						oos.flush();
					} catch (ClassNotFoundException e) {
						ServerSetup.serverLog.addToConsole("Failed to read object from client " + clientsocket);
						e.printStackTrace();
						oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}

				if (message.equals(messages.get(EServerMessage.FILE_EXISTS))) {
					try {
						int checkLevel = ois.readInt();
						SyncFile clientFile = (SyncFile) ois.readObject();
						boolean exists = false;
						
						if (checkLevel == 3) {
							for (SyncFile serverFile : ServerSetup.allFiles) {
								try {									
									if (serverFile.equals(clientFile)) {
										exists = true;
									}
								} catch (InvalidSyncFileException e) {
									//TODO stub invalid file handling
									e.printStackTrace();
								}
							}
						} else {
							for (SyncFile serverFile : ServerSetup.standardSyncableFiles) {
								try {									
									if (serverFile.equals(clientFile)) {
										exists = true;
									}
								} catch (InvalidSyncFileException e) {
									//TODO stub invalid file handling
									e.printStackTrace();
								}
							}
						}
						
						if (exists) {
							System.out.println(clientFile.getFileName() + " exists");
							oos.writeBoolean(true);
							oos.flush();
						} else {
							System.out.println(clientFile.getFileName() + " does not exist");
							oos.writeBoolean(false);
							oos.flush();
						}
					} catch (ClassNotFoundException e) {
						ServerSetup.serverLog.addToConsole("Failed to read object from client " + clientsocket);
						e.printStackTrace();
						oos.writeObject(new MessageError("Failed to read filePath", EErrorType.STREAM_ACCESS));
						oos.flush();
					}
					continue;
				}
			} catch (SocketException e) {
				ServerSetup.serverLog.addToConsole("Client " + clientsocket + " colsed by timeout");
				break;
			} catch (IOException e) {
				ServerSetup.serverLog.addToConsole("Failed to write to " + clientsocket + " client stream");
				e.printStackTrace();
				break;
			}

			if (message.equals(messages.get(EServerMessage.EXIT))) {
				break;
			}
		}
		
		ServerSetup.serverLog.addToConsole("Closing connection with: " + clientsocket);
		teardown();
		return; // End thread, probably not needed here as it is the terminal point of the thread anyway
	}
	
	private void teardown() {
		try {
			timeout = null;
			
			if (!clientsocket.isClosed()) {				
				clientsocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void timeoutShutdown() {
		try {
			ServerSetup.serverLog.addToConsole("Client connection timed out, closing " + clientsocket);
			
			if (!clientsocket.isClosed()) {				
				clientsocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
