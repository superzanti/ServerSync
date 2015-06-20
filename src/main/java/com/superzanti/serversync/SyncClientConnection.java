package com.superzanti.serversync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class SyncClientConnection implements Runnable{
	
	private static Socket socket;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;
	private static InetAddress host = null;
	
	private static boolean errorInUpdates;
	private static boolean updateHappened;
	private static boolean checkFinished;

	protected SyncClientConnection(){
		
		errorInUpdates = false;
		updateHappened = false;
		checkFinished = false;
		
	}
	
	@Override
	public void run() {
		// use the ip address of the server to get the host
        try {
			host = InetAddress.getByName(ServerSyncRegistry.SERVER_IP);
		} catch (UnknownHostException e) {
			ServerSyncRegistry.logger.error("Exception caught! - " + e);
			e.printStackTrace();
			errorInUpdates = true;
		}
        socket = null;
        oos = null;
        ois = null;
        SyncClient.updateScreenWorking(1,"Connecting to server...");
		try {
		    //establish socket connection to server
			ServerSyncRegistry.logger.info("Establishing a socket connection to the server...");
			//socket = new Socket(host.getHostName(), ServerSyncRegistry.SERVER_PORT);
			socket = new Socket();
			socket.connect(new InetSocketAddress(host.getHostName(), ServerSyncRegistry.SERVER_PORT), 5000);
			
			SyncClient.updateScreenWorking(2,"Socket established...");
			
			//write to socket using ObjectOutputStream
			ServerSyncRegistry.logger.info("Creaing input/output streams...");
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			
			SyncClient.updateScreenWorking(3,"Checking to see if updates are needed...");
			oos.writeObject(ServerSyncRegistry.SECURE_CHECK);
			oos.flush();
			String lastUpdate = (String) ois.readObject();
			ServerSyncRegistry.logger.info("Our version is:" + ServerSyncRegistry.LAST_UPDATE);
			ServerSyncRegistry.logger.info("The server's version is:" + lastUpdate);
			
			oos.writeObject(ServerSyncRegistry.SECURE_CHECKMODS);
			oos.flush();
			Integer serverModList = (Integer) ois.readObject();
			Map<String,ModContainer> clientModList_ = Maps.newHashMap(Loader.instance().getIndexedModList());
			System.out.println(Loader.instance().getActiveModList());
			Integer clientModList = clientModList_.hashCode();
			if(serverModList != clientModList){
				ServerSyncRegistry.logger.info("The mods between server and client are incompatable... Force updating...");
			}
			
			if(!lastUpdate.equals(ServerSyncRegistry.LAST_UPDATE) || serverModList != clientModList){
				
				ServerSyncRegistry.logger.info("Sending requests to Socket Server...");
				
				//get all files on server
				ServerSyncRegistry.logger.info("Getting the files on the server...");
				oos.writeObject(ServerSyncRegistry.SECURE_RECURSIVE);
				oos.flush();
				//read the server response message
				ArrayList<String> fileTree = new ArrayList<String>();
				fileTree = (ArrayList) ois.readObject();
				ServerSyncRegistry.logger.info(fileTree);
				
				SyncClient.updateScreenWorking(4,"Got filetree from server...");
				
				//get all the files at home so we can update the progress bar
				ArrayList<String> allList = new ArrayList<String>();
				allList.addAll(dirContents("./mods"));
				allList.addAll(dirContents("./config"));
				
				SyncClient.updateScreenWorking(5,"Got filetree from client...");
				
				ServerSyncRegistry.logger.info("Ignoring: " + ServerSyncRegistry.IGNORE_LIST);
			    
			    // run calculations to figure out how big the bar is
			    float numberOfFiles = allList.size() + fileTree.size();
			    float percentScale = numberOfFiles/92;
			    float currentPercent = 0;
				
				for(String singleFile : fileTree){
					currentPercent = currentPercent + 1;
					SyncClient.updateScreenWorking((int)(5+(currentPercent/percentScale)),"Checking server's " + singleFile.replace('\\', '/'));
					File f = new File(singleFile.replace('\\', '/'));
					if(f.exists() && !f.isDirectory()){
						oos.writeObject(ServerSyncRegistry.SECURE_CHECKSUM);
						oos.flush();
						oos.writeObject(singleFile.replace('\\', '/'));
						oos.flush();
						String serverChecksum = (String) ois.readObject();
						// if the checksums do not match, update the file
						if(!Md5.md5String(f).equals(serverChecksum)){
							if (ServerSyncRegistry.IGNORE_LIST.contains(singleFile.replace('\\',  '/'))){
								ServerSyncRegistry.logger.info("Ignoring: " + singleFile.replace('\\', '/'));
							}else{
								ServerSyncRegistry.logger.info(singleFile.replace('\\', '/') + " Does not match... Updating...");
								ServerSyncRegistry.logger.info("Server Checksum: " + serverChecksum);
								ServerSyncRegistry.logger.info("Our Checksum: " + Md5.md5String(f));
								oos.writeObject(ServerSyncRegistry.SECURE_UPDATE);
								oos.flush();
								oos.writeObject(singleFile.replace('\\', '/'));
								oos.flush();
								
								SyncClient.updateScreenWorking((int)(5+(currentPercent/percentScale)),"Updating " + singleFile.replace('\\', '/'));
								
								// download the file
								File updated = new File(singleFile.replace('\\', '/'));
								//updated.deleteOnExit();
								//ofcourse that didn't work
								updated.getParentFile().mkdirs();
								FileOutputStream wr = new FileOutputStream(updated);
								byte[] outBuffer = new byte[socket.getReceiveBufferSize()];
								int bytesReceived = 0;
								while((bytesReceived = ois.read(outBuffer))>0) {
									wr.write(outBuffer, 0, bytesReceived);
								}
								wr.flush();
								wr.close();
								reinitConn();
								updateHappened = true;
							}
						} else {
							ServerSyncRegistry.logger.info("We have a match! "+ singleFile.replace('\\', '/'));
						}
					} else {
						if (ServerSyncRegistry.IGNORE_LIST.contains(singleFile.replace('\\',  '/'))){
							ServerSyncRegistry.logger.info("Ignoring: " + singleFile.replace('\\', '/'));
						}else{
							ServerSyncRegistry.logger.info(singleFile.replace('\\', '/') + " Does not exist... Updating...");
							oos.writeObject(ServerSyncRegistry.SECURE_UPDATE);
							oos.flush();
							oos.writeObject(singleFile.replace('\\', '/'));
							oos.flush();
							
							SyncClient.updateScreenWorking((int)(5+(currentPercent/percentScale)),"Updating " + singleFile.replace('\\', '/'));
							
							// download the file
							File updated = new File(singleFile.replace('\\', '/'));
							//updated.deleteOnExit();
							//ofcourse that didn't work
							updated.getParentFile().mkdirs();
							FileOutputStream wr = new FileOutputStream(updated);
							byte[] outBuffer = new byte[socket.getReceiveBufferSize()];
							int bytesReceived = 0;
							while((bytesReceived = ois.read(outBuffer))>0) {
								wr.write(outBuffer, 0, bytesReceived);
							}
							wr.flush();
							wr.close();
							reinitConn();
							updateHappened = true;
						}
					}
				}
			    
			    for (String singleFile : allList){
			    	currentPercent++;
					SyncClient.updateScreenWorking((int)(5+(currentPercent/percentScale)),"Checking client's " + singleFile.replace('\\', '/'));
					
			    	ServerSyncRegistry.logger.info("Checking client's files against the server's...");
			    	oos.writeObject(ServerSyncRegistry.SECURE_EXISTS);
					oos.flush();
					oos.writeObject(singleFile.replace('\\', '/'));
					oos.flush();
					
					// check for files that need to be deleted
					String doesExist = (String) ois.readObject();
					
					if (ServerSyncRegistry.IGNORE_LIST.contains(singleFile.replace('\\',  '/'))){
						ServerSyncRegistry.logger.info("Ignoring: " + singleFile.replace('\\', '/'));
					}else{
						if(doesExist.equalsIgnoreCase("false")){
							ServerSyncRegistry.logger.info(singleFile.replace('\\', '/') + " Does not match... Deleting...");
							SyncClient.updateScreenWorking((int)(5+(currentPercent/percentScale)),"Deleting client's " + singleFile.replace('\\', '/'));
							File deleteMe = new File(singleFile.replace('\\', '/'));
							deleteMe.getParentFile().mkdirs();
							FileOutputStream wr = new FileOutputStream(deleteMe);
							wr.write("D".getBytes(), 0, 1);
							wr.flush();
							wr.close();
							deleteMe.deleteOnExit();
							updateHappened = true;
						}
						//reinitConn();
					}
			    }
			    // I found that these lines are not needed since the client updates server sync anyway.
			    //ServerSyncRegistry.config.getCategory("StorageVariables").get("LAST_UPDATE").set(lastUpdate);
			    //ServerSyncRegistry.config.save();
			} else {
				SyncClient.updateScreenWorking(50, "No Updates Needed :D");
				ServerSyncRegistry.logger.info("No Updates Needed");
			}
		    
		    SyncClient.updateScreenWorking(98,"Telling Server to Exit...");
			
		    ServerSyncRegistry.logger.info("Update Complete! Have a nice day!");
			oos.writeObject(ServerSyncRegistry.SECURE_EXIT);
			oos.flush();
		} catch (Exception e) {
			ServerSyncRegistry.logger.error("Exception caught! - " + e);
			e.printStackTrace();
			errorInUpdates = true;
		} finally {
			try {
				SyncClient.updateScreenWorking(99,"Closing connections...");

				if(oos != null)
					oos.close();
				if(ois != null)
					ois.close();
				if(socket !=null)
					socket.close();
			} catch (IOException e) {
				ServerSyncRegistry.logger.error("Exception caught! - " + e);
				e.printStackTrace();
				errorInUpdates = true;
			} //close resources here!
			ServerSyncRegistry.logger.info("All of serversync's sockets to the server have been closed.");
		}
		
		SyncClient.updateScreenWorking(100,"Finished!");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			ServerSyncRegistry.logger.error("Exception caught! - " + e);
			e.printStackTrace();
		}
		checkFinished = true;
		return;
	}
	
	private static ArrayList<String> dirContents(String dir) {
		ServerSyncRegistry.logger.info("Getting all of " + dir.replace('\\', '/') + "'s folder contents");
		File f = new File(dir);
		File[] files = f.listFiles();
		ArrayList<String> dirList = new ArrayList<String>();
		// Loop through all the directories and only add to the list if it's a file
		for (File file : files) {
			if (file.isDirectory()) {
				dirList.addAll(dirContents(file.getPath()));
			} else {
				dirList.add(file.toString());
			}
		}
		return dirList;
	}
	
	private static void reinitConn() throws Exception {
		ServerSyncRegistry.logger.info("Reinitializing the connection...");
		oos.flush();
		// close our resources and set values to null
		if(oos != null)
			oos.close();
		if(ois != null)
			ois.close();
		if(socket !=null)
			socket.close();
        socket = null;
        oos = null;
        ois = null;
		//socket = new Socket(host.getHostName(), ServerSyncRegistry.SERVER_PORT);
		socket = new Socket();
		socket.connect(new InetSocketAddress(host.getHostName(), ServerSyncRegistry.SERVER_PORT), 5000);
		// write to socket using ObjectOutputStream
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		ServerSyncRegistry.logger.info("Sending requests to Socket Server...");
	}
	
	protected static boolean getErrors(){
		return errorInUpdates;
	}
	
	protected static boolean getUpdates(){
		return updateHappened;
	}
	
	protected static boolean getFinished(){
		return checkFinished;
	}
	
	protected static void setFinished(boolean newFinished){
		checkFinished = newFinished;
		return;
	}

}
