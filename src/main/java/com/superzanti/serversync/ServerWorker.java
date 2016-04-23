package com.superzanti.serversync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.superzanti.serversync.util.Md5;
import com.superzanti.serversync.util.Mod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
    private static ArrayList<Mod> allList = new ArrayList<Mod>();
    // Contians mods located in the servers clientmods directory
    private static ArrayList<Mod> clientOnlyList = new ArrayList<Mod>();
	
	protected ServerWorker(Socket socket, ArrayList<Mod> allFiles, ServerSocket theServer){
		clientsocket = socket;
		allList = allFiles;
		ServerSync.logger.info("Connection established with " + clientsocket);
		return;
	}
	
	protected ServerWorker(Socket socket, ArrayList<Mod> allFiles, ArrayList<Mod> clientMods, ServerSocket theServer) {
		this(socket,allFiles,theServer);
		clientOnlyList = clientMods;
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
		
				if(message.equals(ServerSyncConfig.SECURE_CHECK)) {
					oos.writeObject(ServerSyncConfig.LAST_UPDATE);
					oos.flush();
				}
				
				if(message.equals(ServerSyncConfig.SECURE_CHECKMODS)) {
					@SuppressWarnings("unchecked")
					ArrayList<String> serverModList = Mod.listModNames(allList);
					ServerSync.logger.info("Syncable mods are: " + serverModList.toString());
					oos.writeObject(serverModList);
					oos.flush();
				}
				
				if(message.equals(ServerSyncConfig.SECURE_RECURSIVE)) {
					/*ArrayList<String> ml = new ArrayList<String>();
					for (Mod mod : allList) {
						ml.add(mod.MODPATH.toString());
					}*/
					oos.writeObject(allList);
					oos.flush();
				}
				
				if(message.equals(ServerSyncConfig.SECURE_CHECKSUM)) {
					String theFile = (String) ois.readObject();
					File f = new File(theFile);
					String serverChecksum = Md5.md5String(f);
					oos.writeObject(serverChecksum);
					oos.flush();
				}
				
				/*// Not currently in use as clientmods are included in allList
				if(message.equals(Main.SECURE_PUSH_CLIENTMODS)) {
					oos.writeObject(clientOnlyList);
					oos.flush();
				}
				*/
				
				if(message.equals(ServerSyncConfig.SECURE_UPDATE)) {
					ServerSync.logger.info("Writing file to client...");
					String theFile = (String) ois.readObject();
					File f = new File(theFile);
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
				
				if(message.equals(ServerSyncConfig.GET_CONFIG)) {
					ServerSync.logger.info("Sending config to client...");
					File f = new File("./config/serversync.cfg");
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
				
				if(message.equals(Main.SECURE_FILESIZE)) {
					ServerSync.logger.info("Writing filesize to client...");
					String theFile = (String) ois.readObject();
					File f = new File(theFile);
					long l = f.length();
					oos.writeLong(l);
					oos.flush();
				}
				
				if(message.equals(ServerSyncConfig.SECURE_EXISTS)) {
					String theMod = (String) ois.readObject();
					boolean exists = false;
					for(Mod m : allList) {
						if (m.fileName.equals(theMod)) {
							exists = true;
							break;
						}
					}
					if (!exists) {
						for(Mod m : clientOnlyList) {
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
				
				if(message.equals(ServerSyncConfig.SECURE_EXIT)) {
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
