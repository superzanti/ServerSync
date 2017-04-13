package runme;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.superzanti.serversync.ClientWorker;
import com.superzanti.serversync.ServerSetup;
import com.superzanti.serversync.SyncConfig;
import com.superzanti.serversync.gui.GUI_Client;
import com.superzanti.serversync.gui.GUI_Client.SyncPressedListener;
import com.superzanti.serversync.gui.GUI_Server;
import com.superzanti.serversync.util.enums.EConfigType;


public class Main {
	
	/* AWT EVENT DISPATCHER THREAD */
	
	public static final String APPLICATION_TITLE = "Serversync";
	public static final String HANDSHAKE = "HANDSHAKE";
	
	public static GUI_Client clientGUI;
	public static GUI_Server serverGUI;
	
	public static ResourceBundle strings;
	
	public static SyncConfig CONFIG;
	
	
	public static void main(String[] args) throws InterruptedException, IOException {
		CONFIG = new SyncConfig(EConfigType.COMMON);
		try {
			// TODO left off here, fix locale use and other main references
			System.out.println("Loading language file: " + CONFIG.LOCALE);
			strings = ResourceBundle.getBundle("assets.serversync.MessagesBundle", CONFIG.LOCALE);
		} catch (MissingResourceException e) {
			SwingUtilities.invokeLater(new Runnable() {				
				@Override
				public void run() {
					System.out.println("No language file available for: " + CONFIG.LOCALE + ", defaulting to en_US");
				}
			});
			strings = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", new Locale("en", "US"));
		}
		
		if (args == null || args.length == 0) {
			runInClientMode();
			return;
		}
		
		for (String string : args) {
			switch(string) {
				case "server":
					runInServerMode();
			}
		}
	}
	
	private static void runInServerMode() {
		CONFIG = new SyncConfig(EConfigType.SERVER);
		ServerSetup setup = new ServerSetup();
		Thread serverThread = new Thread(setup);
		serverThread.start();
	}
	
	private static void runInClientMode() {
		CONFIG = new SyncConfig(EConfigType.CLIENT);
		
		clientGUI = new GUI_Client();
		
		clientGUI.setSyncPressedListener(new SyncPressedListener() {
			
			@Override
			public void onSyncPressed() {
				int port = clientGUI.getPort();
				String ip = clientGUI.getIPAddress();
				boolean error = false;
				
				if (ip.equals("") || port == 90000) {
					clientGUI.updateText("No config found, requesting details");
					
					if (ip.equals("")) {
						String serverIP = (String) JOptionPane.showInputDialog("Server IP address");
						ip = serverIP;
						clientGUI.setIPAddress(ip);
					}
					
					if (port == 90000) {
						String serverPort = (String) JOptionPane.showInputDialog("Server Port (numbers only)");
						port = Integer.parseInt(serverPort);
						
						if(clientGUI.setPort(port)) {
							error = true;
						}
					}
					
					SyncConfig.pullServerConfig = true;
				}
				
				if (!error) {
					CONFIG.SERVER_IP = ip;
					CONFIG.SERVER_PORT = port;
					clientGUI.updateText("Starting update process...");
					new Thread(new ClientWorker()).start();
				}
			}
		});
		System.out.println(CONFIG.SERVER_PORT);
		clientGUI.setIPAddress(CONFIG.SERVER_IP);
		clientGUI.setPort(CONFIG.SERVER_PORT);
		clientGUI.build(CONFIG.LOCALE);
	}
}