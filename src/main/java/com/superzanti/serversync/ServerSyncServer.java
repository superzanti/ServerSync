package com.superzanti.serversync;

/**
 * Wrapper for executables to call when starting in server mode
 */
public class ServerSyncServer {
    public static void main(String[] args) {
        ServerSync.main(new String[]{"server"});
    }
}
