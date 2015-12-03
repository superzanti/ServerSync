package com.superzanti.serversync.proxy;

import com.superzanti.serversync.SyncClient;

public class ClientProxy extends CommonProxy {
	
	private static SyncClient syncclient;

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public boolean isServer() {
        return false;
    }
    
    /**
     * @return Current instance of SyncClient
     */
    public static SyncClient getClient() {
    	return syncclient;
    }
    
    public static void newClient() {
    	syncclient = new SyncClient();
    }

}