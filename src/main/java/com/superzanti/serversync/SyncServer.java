package com.superzanti.serversync;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

/**
 * Sets up listener for clients on the server
 * @author superzanti
 */
@SideOnly(Side.SERVER)
public class SyncServer implements Runnable {
	
	//static ServerSocket variable
    private static ServerSocket server;
    
    //this is what's in our folders
	private static ArrayList<String> allList = new ArrayList<String>();
	private static ArrayList<String> clientList = new ArrayList<String>();

	protected SyncServer(){
	    ServerSync.logger.info("Getting ./mod contents");
	    allList.addAll(dirContents("./mods"));
	    ServerSync.logger.info("Getting ./config contents");
		allList.addAll(dirContents("./config"));
		if (ServerSyncConfig.PUSH_CLIENT_MODS) {
			ServerSync.logger.info("Getting ./clientmods contents");
			clientList.addAll(dirContents("./clientmods"));
		}
		return;
	}
	
	@Override
	public void run(){
        //create the socket server object
		ServerSync.logger.info("Creating new server socket");
        try {
			server = new ServerSocket(ServerSyncConfig.SERVER_PORT);
		} catch (IOException e) {
			ServerSync.logger.info("Error occured."+e);
			e.printStackTrace();
		}
        //keep listens indefinitely until receives 'exit' call or program terminates
        ServerSync.logger.info("Now accepting clients...");
        while(true){
            try
            {
            	Socket socket = server.accept();
            	ServerWorker sc;
            	if (ServerSyncConfig.PUSH_CLIENT_MODS) { 
            		sc = new ServerWorker(socket, allList, clientList, server);
            	} else {
            		sc = new ServerWorker(socket, allList, server);
            	}
                new Thread(sc).start();
            }
            catch(Exception e)
            {
            	ServerSync.logger.info("Error occured."+e);
            	e.printStackTrace();
            }
        }
	}
	
	private static ArrayList<String> dirContents(String dir) {
		ServerSync.logger.info("Getting all of " + dir.replace('\\', '/') + "'s folder contents");
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

}
