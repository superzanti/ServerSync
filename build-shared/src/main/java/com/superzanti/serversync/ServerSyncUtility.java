package com.superzanti.serversync;

import com.superzanti.serversync.util.enums.EServerMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class ServerSyncUtility {

    /* AWT EVENT DISPATCHER THREAD */

    public static final String APPLICATION_TITLE = "Serversync";
    public static final String GET_SERVER_INFO = "SERVER_INFO";
    public static EServerMode MODE;

    public static ResourceBundle strings;

    public static Path rootDir = Paths.get(System.getProperty("user.dir"));

}