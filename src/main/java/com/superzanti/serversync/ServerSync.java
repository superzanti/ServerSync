package com.superzanti.serversync;

import com.superzanti.serversync.client.ClientWorker;
import com.superzanti.serversync.config.ConfigLoader;
import com.superzanti.serversync.config.SyncConfig;
import com.superzanti.serversync.gui.GUI_Client;
import com.superzanti.serversync.server.ServerSetup;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.enums.EConfigType;
import com.superzanti.serversync.util.enums.EServerMode;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

@Command(name = "ServerSync", mixinStandardHelpOptions = true, version = "3.6.0", description = "A utility for synchronizing a server<->client style game.")
public class ServerSync implements Callable<Integer> {

    /* AWT EVENT DISPATCHER THREAD */

    public static final String APPLICATION_TITLE = "Serversync";
    public static final String GET_SERVER_INFO = "SERVER_INFO";
    public static EServerMode MODE;

    public static GUI_Client clientGUI;
    public static ResourceBundle strings;

    public static Path rootDir = Paths.get(System.getProperty("user.dir"));

    @Option(names = {"-r", "--root"}, description = "The root directory of the game, defaults to the current working directory.")
    private String rootDirectory = System.getProperty("user.dir");
    @Option(names = {"-o", "--progress", "progress-only"}, description = "Only show progress indication. Ignored if '-s', '--server' is specified.")
    private boolean modeProgressOnly = false;
    @Option(names = {"-q", "--quiet", "silent"}, description = "Remove all GUI interaction. Ignored if '-s', '--server' is specified.")
    private boolean modeQuiet = false;
    @Option(names = {"-s", "--server", "server"}, description = "Run the program in server mode.")
    private boolean modeServer = false;
    @Option(names = {"-a", "--address"}, description = "The address of the server you wish to connect to.")
    private String serverAddress;
    @Option(names = {"-p", "--port"}, description = "The port the server is running on.")
    private int serverPort = -1;
    @Option(names = {"-i", "--ignore"}, arity = "1..*", description = "A glob pattern or series of patterns for files to ignore")
    private String[] ignorePatterns;
    @Option(names = {"-l", "--lang"}, description = "A language code to set the UI language e.g. zh_CN or en_US")
    private String languageCode;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ServerSync()).execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    @Override
    public Integer call() {
        ServerSync.rootDir = Paths.get(rootDirectory);
        if (modeServer) {
            runInServerMode();
        } else {
            runInClientMode();
        }
        return 0;
    }

    private void commonInit() {
        Locale locale = SyncConfig.getConfig().LOCALE;
        if (languageCode != null) {
            String[] lParts = languageCode.split("[_-]");
            locale = new Locale(lParts[0], lParts[1]);
            SyncConfig.getConfig().LOCALE = locale;
        }
        if (serverAddress != null) {
            SyncConfig.getConfig().SERVER_IP = serverAddress;
        }
        if (serverPort > 0) {
            SyncConfig.getConfig().SERVER_PORT = serverPort;
        }
        if (ignorePatterns != null) {
            SyncConfig.getConfig().FILE_IGNORE_LIST = Arrays.asList(ignorePatterns);
        }

        try {
            Logger.log("Loading language file: " + locale);
            strings = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", locale);
        } catch (MissingResourceException e) {
            Logger.log("No language file available for: " + locale + ", defaulting to en_US");
            strings = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", new Locale("en", "US"));
        }
    }

    private void runInServerMode() {
        ServerSync.MODE = EServerMode.SERVER;
        new Logger("server");
        Logger.setSystemOutput(true);
        try {
            ConfigLoader.load(EConfigType.SERVER);
        } catch (IOException e) {
            Logger.error("Failed to load server config");
            Logger.debug(e);
        }
        commonInit();

        ServerSetup setup = new ServerSetup();
        Thread serverThread = new Thread(setup, "Server client listener");
        serverThread.start();
    }

    private void runInClientMode() {
        ServerSync.MODE = EServerMode.CLIENT;
        new Logger("client");
        SyncConfig config = SyncConfig.getConfig();
        try {
            ConfigLoader.load(EConfigType.CLIENT);
        } catch (IOException e) {
            Logger.error("Failed to load client config");
            e.printStackTrace();
        }
        commonInit();

        Thread clientThread;
        if (modeQuiet) {
            clientGUI = null;
            new Thread(new ClientWorker()).start();
        } else if (modeProgressOnly) {
            // TODO setup a progress only version of the GUI
            clientGUI = new GUI_Client();
            clientGUI.setIPAddress(config.SERVER_IP);
            clientGUI.setPort(config.SERVER_PORT);
            clientGUI.build(config.LOCALE);

            clientThread = new Thread(new ClientWorker(), "Client processing");
            clientThread.start();
            try {
                clientThread.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.exit(1);
            }
            System.exit(0);
        } else {
            clientGUI = new GUI_Client();
            clientGUI.setIPAddress(config.SERVER_IP);
            clientGUI.setPort(config.SERVER_PORT);
            clientGUI.build(config.LOCALE);
        }
    }
}