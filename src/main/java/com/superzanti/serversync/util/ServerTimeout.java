package com.superzanti.serversync.util;

import java.util.TimerTask;

import com.superzanti.serversync.server.ServerWorker;

/**
 * Generic timeout for disconnecting unresponsive clients
 * @author Rheimus
 */
public class ServerTimeout extends TimerTask {
	
	ServerWorker worker;
	
	public ServerTimeout(ServerWorker worker) {
		this.worker = worker;
	}

	@Override
	public void run() {
		this.worker.timeoutShutdown();
	}
	
}
