package com.superzanti.serversync.client;

public class Client {
    public final String serverAddress;
    public final int serverPort;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
}
