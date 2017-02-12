package com.superzanti.serversync.util;

import java.util.TimerTask;

import com.superzanti.serversync.ServerWorker;

public class ServerTimeout extends TimerTask {
	
	ServerWorker worker;
	
	public ServerTimeout(ServerWorker worker) {
		this.worker = worker;
	}

	@Override
	public void run() {
		this.worker.shutdown();
	}
	
}
