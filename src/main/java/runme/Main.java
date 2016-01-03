package runme;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.superzanti.serversync.OfflineClientWorker;
import com.superzanti.serversync.ServerSyncConfig;

public class Main {
	private static Delete deleteOldMods = new Delete();
	private static OfflineClientWorker updateMods = new OfflineClientWorker();
	
	private static boolean configError = false;
	
	private static JLabel info = new JLabel("Information Zone");
	
	private static void GUI() {
		JFrame rootFrame = new JFrame("Serversync");
		rootFrame.setResizable(false);
		rootFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		rootFrame.setLocationRelativeTo(null);
		rootFrame.setPreferredSize(new Dimension(400,200));
		
		info = new JLabel("Information Zone");
		info.setHorizontalAlignment(SwingConstants.CENTER);
		rootFrame.getContentPane().add(info);
		
		// Display window
		rootFrame.pack();
		rootFrame.setVisible(true);
	}

	public static void main(String[] args) throws InterruptedException, IOException  {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				GUI();
			}
			
		});
		
		Path config = Paths.get("../config/serversync.cfg");
		System.out.println(config.toAbsolutePath().toString());
		if (Files.exists(config)) {			
			updateText("File exists");
			System.out.println("attempting to init config file: " + config.toAbsolutePath().toString());
			//ServerSyncConfig.init(config.toFile());
			ServerSyncConfig.getServerDetailsDirty(config);
		} else {
			configError = true;
		}
		
		int mode = 10;
		if (args.length > 0) {
			for (String arg : args) {
				if (arg.equalsIgnoreCase("delete")) {
					mode = 0;
				}
			}
		}
		startWorker(mode);
	}

	public static void updateText(final String string) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				info.setText(string);
			}
			
		});
	}
	
	private static void startWorker(int mode) {
		Thread t;
		switch(mode) {
		case 0:
			updateText("Starting deletion process...");
			t = new Thread(deleteOldMods);
			t.start();
			break;
		default:
			if (!configError) {
				updateText("Starting update process...");
				t = new Thread(updateMods);
				t.start();
			} else {
				updateText("Could not load serversync.cfg");
			}
			break;
		}
	}

}
