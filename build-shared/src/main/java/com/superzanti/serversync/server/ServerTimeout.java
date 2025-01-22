package com.superzanti.serversync.server;

import java.util.TimerTask;

/**
 * Generic timeout for disconnecting unresponsive clients
 * @author Rheimus
 */
public class ServerTimeout extends TimerTask {
	
	final ServerWorker worker;
	
	public ServerTimeout(ServerWorker worker) {
		this.worker = worker;
	}

	@Override
	public void run() {
		this.worker.timeoutShutdown();
	}
	
}
