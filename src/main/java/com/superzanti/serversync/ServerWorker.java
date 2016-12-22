package com.superzanti.serversync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.superzanti.serversync.util.Md5;
import com.superzanti.serversync.util.SyncFile;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import runme.Main;

/**
 * This worker handles requests from the client continuously until told to exit using SECURE_EXIT
 * @author superzanti
 */
@SideOnly(Side.SERVER)
public class ServerWorker implements Runnable {
	
	private static Socket clientsocket;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;
	// Contains all mods on the server, including client-side mods etc
    private static ArrayList<SyncFile> allList = new ArrayList<SyncFile>();
    // Contians mods located in the servers clientmods directory
    private static ArrayList<SyncFile> clientOnlyList = new ArrayList<SyncFile>();
    private static ArrayList<String> dirList = new ArrayList<String>();
	
	protected ServerWorker(Socket socket, ArrayList<SyncFile> allFiles, ArrayList<SyncFile> clientFiles, ArrayList<String> dirList, ServerSocket theServer){
		clientsocket = socket;
		allList = allFiles;
		ServerWorker.dirList = dirList;
		clientOnlyList = clientFiles;
		ServerSync.logger.info("Connection established with " + clientsocket);
		return;
	}

	@Override
	public void run() {
		try
		{
			ois = new ObjectInputStream(clientsocket.getInputStream());
			oos = new ObjectOutputStream(clientsocket.getOutputStream());
			oos.flush();
			while(true) {
				
				String message = (String) ois.readObject();
				ServerSync.logger.info("Received message: "+message+" from connection "+clientsocket);
		
				if(message.equals(SyncConfig.MESSAGE_CHECK)) {
					oos.writeObject(SyncConfig.LAST_UPDATE);
					oos.flush();
				}
				
				if(message.equals(SyncConfig.MESSAGE_UPDATE_NEEDED)) {
					ArrayList<String> serverModList = SyncFile.listModNames(allList);
					
					// Remove client only mods and server only mods from comparison
					serverModList.removeAll(new ArrayList<String>(SyncConfig.IGNORE_LIST));
					
					ServerSync.logger.info("Syncable mods are: " + serverModList.toString());
					oos.writeObject(serverModList);
					oos.flush();
				}
				
				if(message.equals(SyncConfig.MESSAGE_GET_FILE_LIST)) {
					oos.writeObject(allList);
					oos.flush();
				}
				
				if(message.equals(SyncConfig.MESSAGE_GET_SYNCABLE_DIRECTORIES)) {
					oos.writeObject(dirList);
					oos.flush();
				}
				
				if(message.equals(SyncConfig.MESSAGE_COMPARE)) {
					File theFile = (File) ois.readObject();
					String serverChecksum = Md5.md5String(theFile);
					oos.writeObject(serverChecksum);
					oos.flush();
				}
				
				if(message.equals(Main.SECURE_PUSH_CLIENTMODS)) {
					oos.writeObject(clientOnlyList);
					oos.flush();
				}
				
				if(message.equals(SyncConfig.MESSAGE_UPDATE)) {
					ServerSync.logger.info("Writing file to client...");
					String theFile = (String) ois.readObject();
					File f = new File(theFile.replace("\\", "/"));
					byte[] buff = new byte[clientsocket.getSendBufferSize()];
					int bytesRead = 0;
					InputStream in = new FileInputStream(f);
					while((bytesRead = in.read(buff))>0) {
						oos.write(buff,0,bytesRead);
					}
					in.close();
					oos.flush();
					break;
				}
				
				if(message.equals(SyncConfig.MESSAGE_GET_CONFIG)) {
					ServerSync.logger.info("Sending config info to client...");
					HashMap<String, List<String>> rules = new HashMap<String, List<String>>();
					rules.put("ignore", SyncConfig.IGNORE_LIST);
					rules.put("include", SyncConfig.INCLUDE_LIST);
					//TODO add security info in transfer
					oos.writeObject(rules);
					oos.flush();
				}
				
				if(message.equals(SyncConfig.MESSAGE_SEC_HANDSHAKE)) {
					HashMap<String,String> security = new HashMap<String, String>();
					security.put("SECURE_CHECK", SyncConfig.MESSAGE_CHECK);
					security.put("SECURE_CHECKMODS", SyncConfig.MESSAGE_UPDATE_NEEDED);
					security.put("SECURE_CHECKSUM", SyncConfig.MESSAGE_COMPARE);
					security.put("SECURE_EXISTS", SyncConfig.MESSAGE_FILE_EXISTS);
					security.put("SECURE_EXIT", SyncConfig.MESSAGE_SERVER_EXIT);
					security.put("SECURE_RECURSIVE", SyncConfig.MESSAGE_GET_FILE_LIST);
					security.put("SECURE_UPDATE", SyncConfig.MESSAGE_UPDATE);
					oos.writeObject(security);
					oos.flush();
				}
				
				if(message.equals(Main.SECURE_FILESIZE)) {
					//TODO update to NIO
					ServerSync.logger.info("Writing filesize to client...");
					String theFile = (String) ois.readObject();
					Path p = Paths.get(theFile.replace("\\", "/"));
					oos.writeLong(Files.size(p));
					oos.flush();
				}
				
				if(message.equals(SyncConfig.MESSAGE_FILE_EXISTS)) {
					String theMod = (String) ois.readObject();
					boolean exists = false;
					for(SyncFile m : allList) {
						if (m.fileName.equals(theMod)) {
							exists = true;
							break;
						}
					}
					if (!exists) {
						for(SyncFile m : clientOnlyList) {
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
				}
				
				if(message.equals(SyncConfig.MESSAGE_SERVER_EXIT)) {
					break;
				}
			}
			ServerSync.logger.info("Connection "+clientsocket+" is closing.");
		}
		catch(Exception e)
		{ 
			ServerSync.logger.info("Error occured: "+e);
			e.printStackTrace();
		} finally {
			try {
				if(oos != null)
					oos.close();
				if(ois != null)
					ois.close();
				if(clientsocket !=null)
					clientsocket.close();
			} catch (IOException e) {
				ServerSync.logger.info("Error occured: "+e);
				e.printStackTrace();
			}
		}
		return;
	}
}
