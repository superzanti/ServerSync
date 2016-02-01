package runme;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.superzanti.serversync.OfflineClientWorker;
import com.superzanti.serversync.ServerSyncConfig;

public class Main {
	private static Delete deleteOldMods = new Delete();
	private static OfflineClientWorker updateMods = new OfflineClientWorker();
	
	public static final String SECURE_FILESIZE = "11b4278c7e5a79003db77272c1ed2cf5";
	public static final String SECURE_PUSH_CLIENTMODS = "0ad95bb1734520dc1fa3c737f8a57d91";
	
	private static boolean configError = false;
	private static int mode = 10;
	
	private static JTextArea info = new JTextArea();
	private static JFrame rootFrame;
	
	private static void GUI() {
		rootFrame = new JFrame("Serversync");
		rootFrame.setResizable(false);
		rootFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		rootFrame.setPreferredSize(new Dimension(600,300));
		Container pane = rootFrame.getContentPane();
		
		JScrollPane scrollable = new JScrollPane(info);
		scrollable.setPreferredSize(new Dimension(500,300));

		info.setLineWrap(true);
		info.setOpaque(true);
		info.setEditable(false);
		pane.add(scrollable, BorderLayout.LINE_END);
		
		// Display window
		rootFrame.pack();
		rootFrame.setLocationRelativeTo(null);
		rootFrame.setVisible(true);

		startWorker(mode);
	}

	public static void main(String[] args) throws InterruptedException, IOException  {	
		if (args.length > 0) {
			for (String arg : args) {
				if (arg.equalsIgnoreCase("delete")) {
					mode = 0;
				}
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				GUI();
			}
			
		});
	}

	public static void updateText(final String string) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				info.setText(string);
			}
			
		});
	}
	
	public static void updateProgress(final int progress) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				rootFrame.setTitle("Serversync - " + progress + "%");
			}
			
		});
	}
	
	private static void startWorker(int mode) {
		Thread t;
		Path config = Paths.get("../config/serversync.cfg");
		System.out.println(config.toAbsolutePath().toString());
		if (Files.exists(config)) {			
			updateText("serversync.cfg found");
			System.out.println("attempting to init config file: " + config.toAbsolutePath().toString());
			//ServerSyncConfig.init(config.toFile());
			try {
				ServerSyncConfig.getServerDetailsDirty(config);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			configError = true;
		}
		
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
