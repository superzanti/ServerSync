package runme;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.sun.glass.events.KeyEvent;
import com.superzanti.serversync.OfflineClientWorker;
import com.superzanti.serversync.ServerSyncConfig;

public class Main {
	private static Delete deleteOldMods = new Delete();
	private static OfflineClientWorker updateMods = new OfflineClientWorker();

	public static final String SECURE_FILESIZE = "11b4278c7e5a79003db77272c1ed2cf5";
	public static final String SECURE_PUSH_CLIENTMODS = "0ad95bb1734520dc1fa3c737f8a57d91";
	private static int mode = 10;

	private static JTextArea TA_info = new JTextArea();
	private static JTextArea TA_ipAddress = new JTextArea();
	private static JTextArea TA_port = new JTextArea();
	private static JButton B_sync = new JButton();
	private static JFrame F_root;
	private static JPanel P_serverDetails;

	private static void GUI() {
		Dimension sDetailsElements = new Dimension(140, 20);

		F_root = new JFrame("Serversync");
		F_root.setResizable(false);
		F_root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		F_root.setPreferredSize(new Dimension(600, 300));
		F_root.setIconImage(new ImageIcon(ClassLoader.getSystemResource("tap.png")).getImage());

		P_serverDetails = new JPanel();
		P_serverDetails.setPreferredSize(new Dimension(143, 300));

		B_sync.setPreferredSize(sDetailsElements);
		TA_ipAddress.setPreferredSize(sDetailsElements);
		TA_port.setPreferredSize(sDetailsElements);

		B_sync.setToolTipText("Starts sync process");
		B_sync.setText("Sync");
		TA_ipAddress.setOpaque(true);
		TA_port.setOpaque(true);
		TA_ipAddress.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "none");
		TA_ipAddress.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "none");
		TA_port.setInputMap(JComponent.WHEN_FOCUSED, TA_ipAddress.getInputMap());

		TA_ipAddress.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(java.awt.event.KeyEvent e) {
				return;
			}

			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
					TA_ipAddress.transferFocus();
				}
			}

			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				return;
			}
		});
		TA_port.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(java.awt.event.KeyEvent e) {
				return;
			}

			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					F_root.requestFocus();
					startWorker(mode);
				}
			}

			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				return;
			}
		});
		Container pane = F_root.getContentPane();

		JLabel ipLabel = new JLabel("IP Address");
		JLabel portLabel = new JLabel("Port");
		ipLabel.setLabelFor(TA_ipAddress);
		portLabel.setLabelFor(TA_port);

		JScrollPane sp_console = new JScrollPane(TA_info);
		sp_console.setPreferredSize(new Dimension(450, 300));

		P_serverDetails.add(ipLabel);
		P_serverDetails.add(TA_ipAddress);
		P_serverDetails.add(portLabel);
		P_serverDetails.add(TA_port);
		P_serverDetails.add(B_sync);

		TA_info.setLineWrap(true);
		TA_info.setWrapStyleWord(true);
		TA_info.setOpaque(true);
		TA_info.setEditable(false);
		pane.add(sp_console, BorderLayout.LINE_END);
		pane.add(P_serverDetails, BorderLayout.LINE_START);

		// Listeners
		B_sync.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				startWorker(mode);
			}
		});

		// Display window
		F_root.pack();
		F_root.setLocationRelativeTo(null);
		F_root.setVisible(true);

		Path config = Paths.get("../config/serversync.cfg");
		if (Files.exists(config)) {
			updateText("serversync.cfg found");
			System.out.println("attempting to init config file: " + config.toAbsolutePath().toString());
			// ServerSyncConfig.init(config.toFile());
			try {
				ServerSyncConfig.getServerDetailsDirty(config);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			TA_ipAddress.setText(ServerSyncConfig.SERVER_IP);
			TA_port.setText(Integer.toString(ServerSyncConfig.SERVER_PORT));
			ServerSyncConfig.pullServerConfig = false;
			ServerSyncConfig.configPresent = true;
			TA_port.requestFocus();
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
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
				TA_info.setText(string);
			}

		});
	}

	public static void updateProgress(final int progress) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				F_root.setTitle("Serversync - " + progress + "%");
			}

		});
	}

	private static void startWorker(int mode) {
		boolean error = false;
		Thread t;
		if (TA_ipAddress.getText().equals("") || TA_port.getText().equals("")) {
			updateText("No config found, requesting details");
			if (TA_ipAddress.getText().equals("")) {
				String serverIP = (String) JOptionPane.showInputDialog("Server IP address");
				TA_ipAddress.setText(serverIP);
			}
			if (TA_port.getText().equals("")) {
				String serverPort = (String) JOptionPane.showInputDialog("Server Port (numbers only)");
				TA_port.setText(serverPort);
			}
			ServerSyncConfig.pullServerConfig = true;
		}


		int port = 0;
		try {
			port = Integer.parseInt(TA_port.getText());
		} catch (NumberFormatException e) {
			error = true;
			updateText("Invalid port, please use only numbers");
		}
		ServerSyncConfig.SERVER_PORT = port;
		ServerSyncConfig.SERVER_IP = TA_ipAddress.getText();

		switch (mode) {
		case 0:
			updateText("Starting deletion process...");
			t = new Thread(deleteOldMods);
			t.start();
			break;
		default:
			if (!error) {
				updateText("Starting update process...");
				t = new Thread(updateMods);
				t.start();
			}
			break;
		}
	}

}
