package com.superzanti.serversync;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.SERVER)
public class SyncServer implements Runnable {
	
	//static ServerSocket variable
    private static ServerSocket server;
    
    //this is what's in our folders
	private static ArrayList<String> allList = new ArrayList<String>();

	protected SyncServer(){
	    ServerSyncRegistry.logger.info("Getting ./mod contents");
	    allList.addAll(dirContents("./mods"));
	    ServerSyncRegistry.logger.info("Getting ./config contents");
		allList.addAll(dirContents("./config"));
		return;
	}
	
	@Override
	public void run(){
        //create the socket server object
		ServerSyncRegistry.logger.info("Creating new server socket");
        try {
			server = new ServerSocket(ServerSyncRegistry.SERVER_PORT);
		} catch (IOException e) {
			ServerSyncRegistry.logger.info("Error occured."+e);
			e.printStackTrace();
		}
        //keep listens indefinitely until receives 'exit' call or program terminates
        ServerSyncRegistry.logger.info("Now accepting clients...");
        while(true){
            try
            {
            	Socket socket = server.accept();
                SyncServerConnection sc = new SyncServerConnection(socket, allList, server);
                new Thread(sc).start();
            }
            catch(Exception e)
            {
            	ServerSyncRegistry.logger.info("Error occured."+e);
            	e.printStackTrace();
            }
        }
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

}
