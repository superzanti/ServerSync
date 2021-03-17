package com.superzanti.serversync.client;

import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.communication.response.ServerInfo;
import com.superzanti.serversync.util.AutoClose;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EServerMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Server {
    public ObjectOutputStream output;
    public ObjectInputStream input;
    public Socket clientSocket;
    public ServerInfo info;

    public OutputStream os;
    public InputStream is;

    protected final String address;
    protected final int port;

    Server(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public static Server forClient(Client client) {
        return new Server(client.serverAddress, client.serverPort);
    }

    public boolean connect() {
        InetAddress host;
        try {
            host = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            Logger.error(ServerSync.strings.getString("connection_failed_host") + ": " + address);
            return false;
        }

        Logger.debug(ServerSync.strings.getString("connection_attempt_server"));
        clientSocket = new Socket();

        Logger.log("< " + ServerSync.strings.getString("connection_message") + " >");
        try {
            clientSocket.setPerformancePreferences(0, 1, 2);
            clientSocket.connect(new InetSocketAddress(host.getHostName(), port), 5000);
        } catch (IOException e) {
            Logger.error(ServerSync.strings.getString("connection_failed_server") + ": " + address + ":" + port);
            AutoClose.closeResource(clientSocket);
            return false;
        }

        Logger.debug(ServerSync.strings.getString("debug_IO_streams"));
        try {
            os = clientSocket.getOutputStream();
            is = clientSocket.getInputStream();
            output = new ObjectOutputStream(os);
            input = new ObjectInputStream(is);
        } catch (IOException e) {
            Logger.debug(ServerSync.strings.getString("debug_IO_streams_failed"));
            AutoClose.closeResource(clientSocket);
            return false;
        }

        try {
            output.writeUTF(ServerSync.GET_SERVER_INFO);
            output.flush();
        } catch (IOException e) {
            Logger.outputError(ServerSync.GET_SERVER_INFO);
        }

        try {
            info = (ServerInfo) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Logger.error("Failed to read server information");
            Logger.debug(e);
        }

        return true;
    }

    public void close() {
        try {
            // Output could be null if the server has never connected, i.e. the client used the wrong details
            if (output != null) {
                output.writeUTF(EServerMessage.EXIT.toString());
                output.flush();
            }
        } catch (IOException e) {
            Logger.error("Failed to close server connection");
            Logger.debug(e);
        }

        if (clientSocket != null) {
            AutoClose.closeResource(clientSocket);
        }
    }
}
