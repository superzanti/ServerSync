package runme;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.client.ClientWorker;
import com.superzanti.serversync.gui.GUI_Client;
import com.superzanti.serversync.gui.GUI_Client_Mock;
import com.superzanti.serversync.gui.GUI_Server;
import com.superzanti.serversync.server.ServerSetup;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.ProgramArguments;
import com.superzanti.serversync.util.enums.EConfigType;

public class Main {

	/* AWT EVENT DISPATCHER THREAD */

	public static final String APPLICATION_TITLE = "Serversync";
	public static final String HANDSHAKE = "HANDSHAKE";

	public static GUI_Client clientGUI;
	public static GUI_Server serverGUI;

	public static ResourceBundle strings;

	public static SyncConfig CONFIG;

	public static ProgramArguments arguments;

	public static void main(String[] args) {
		arguments = new ProgramArguments(args);

		if (arguments.isServer) {
			runInServerMode();
		} else {
			runInClientMode();
		}

		// Only for testing, this cleans up old sync files
		if (arguments.cleanup) {
			System.out.println("cleaning up test files");
			System.out.println(Paths.get("").toAbsolutePath().toString());
			try {
				Path modsDir = Paths.get("mods");
				if (Files.exists(modsDir)) {
					Files.walkFileTree(modsDir, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							Files.delete(dir);
							return FileVisitResult.CONTINUE;
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void commonInit() {
		try {
			System.out.println("Loading language file: " + CONFIG.LOCALE);
			strings = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", CONFIG.LOCALE);
		} catch (MissingResourceException e) {
			System.out.println("No language file available for: " + CONFIG.LOCALE + ", defaulting to en_US");
			strings = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", new Locale("en", "US"));
		}
	}

	private static void runInServerMode() {
		new Logger("server");
		Logger.setSystemOutput(true);
		CONFIG = new SyncConfig(EConfigType.SERVER);
		commonInit();

		ServerSetup setup = new ServerSetup();
		Thread serverThread = new Thread(setup, "Server client listener");
		serverThread.start();
	}

	private static void runInClientMode() {
		new Logger("client");
		CONFIG = new SyncConfig(EConfigType.CLIENT);
		commonInit();

		Thread clientThread;
		if (arguments.syncSilent) {
			clientGUI = new GUI_Client_Mock();
			new Thread(new ClientWorker()).start();
		} else if (arguments.syncProgressOnly) {
			// TODO setup a progress only version of the GUI
			clientGUI = new GUI_Client();
			clientGUI.setIPAddress(CONFIG.SERVER_IP);
			clientGUI.setPort(CONFIG.SERVER_PORT);
			clientGUI.build(CONFIG.LOCALE);

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
			clientGUI.setIPAddress(CONFIG.SERVER_IP);
			clientGUI.setPort(CONFIG.SERVER_PORT);
			clientGUI.build(CONFIG.LOCALE);
		}
	}
}