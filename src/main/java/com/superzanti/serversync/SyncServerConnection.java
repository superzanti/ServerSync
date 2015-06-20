package com.superzanti.serversync;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.SERVER)
public class SyncServerConnection implements Runnable {
	
	private static Socket clientsocket;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;
    private static ArrayList<String> allList = new ArrayList<String>();
    private static ServerSocket server;
	
	protected SyncServerConnection(Socket socket, ArrayList<String> allFiles, ServerSocket theServer){
		clientsocket = socket;
		allList = allFiles;
		server = theServer;
		ServerSyncRegistry.logger.info("Connection established with " + clientsocket);
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
				ServerSyncRegistry.logger.info("Received message: "+message+" from connection "+clientsocket);
		
				if(message.equals(ServerSyncRegistry.SECURE_CHECK)) {
					oos.writeObject(ServerSyncRegistry.LAST_UPDATE);
					oos.flush();
				}
				
				if(message.equals(ServerSyncRegistry.SECURE_CHECKMODS)){
					Map<String,ModContainer> serverModList_ = Maps.newHashMap(Loader.instance().getIndexedModList());
					Map<String,ModContainer> serverModList = Maps.newHashMap(Loader.instance().getIndexedModList());
					for (Map.Entry<String, ModContainer> modEntry : serverModList_.entrySet()){
						Path modPath = Paths.get(modEntry.getValue().getSource().getAbsolutePath());
						Path rootPath = Paths.get("").toAbsolutePath();
						String relativeModPath = "./" + rootPath.relativize(modPath);
						if (ServerSyncRegistry.IGNORE_LIST.contains(relativeModPath.replace('\\',  '/'))){
							serverModList.remove(modEntry.getKey());
						}
					}
					ServerSyncRegistry.logger.info(serverModList);
					oos.writeObject((String)serverModList.toString());
					oos.flush();
				}
				
				if(message.equals(ServerSyncRegistry.SECURE_RECURSIVE)) {
					oos.writeObject(allList);
					oos.flush();
				}
				
				if(message.equals(ServerSyncRegistry.SECURE_CHECKSUM)) {
					String theFile = (String) ois.readObject();
					File f = new File(theFile);
					String serverChecksum = Md5.md5String(f);
					oos.writeObject(serverChecksum);
					oos.flush();
				}
				
				if(message.equals(ServerSyncRegistry.SECURE_UPDATE)) {
					ServerSyncRegistry.logger.info("Writing file to client...");
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
				
				if(message.equals(ServerSyncRegistry.SECURE_EXISTS)) {
					String theFile = (String) ois.readObject();
					File f = new File(theFile);
					if(f.exists() && !f.isDirectory()) {
						oos.writeObject("true");
						oos.flush();
					}
					else {
						oos.writeObject("false");
						oos.flush();
					}
				}
				
				if(message.equals(ServerSyncRegistry.SECURE_EXIT)) {
					break;
				}
			}
			ServerSyncRegistry.logger.info("Connection "+clientsocket+" is closing.");
		}
		catch(Exception e)
		{ 
			ServerSyncRegistry.logger.info("Error occured: "+e);
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
				ServerSyncRegistry.logger.info("Error occured: "+e);
				e.printStackTrace();
			}
		}
		return;
	}
}
