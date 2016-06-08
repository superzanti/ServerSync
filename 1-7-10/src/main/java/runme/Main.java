package runme;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.superzanti.lib.RefStrings;
import com.superzanti.serversync.ClientWorker;
import com.superzanti.serversync.SyncConfig;

public class Main {
	
	/* AWT EVENT DISPATCHER THREAD */

	public static final String SECURE_FILESIZE = "11b4278c7e5a79003db77272c1ed2cf5";
	public static final String SECURE_PUSH_CLIENTMODS = "0ad95bb1734520dc1fa3c737f8a57d91";

	private static JTextArea TA_info = new JTextArea();
	private static JTextField TF_ipAddress = new JTextField();
	private static JTextField TF_port = new JTextField();
	private static JButton B_sync = new JButton();
	private static JFrame F_root;
	private static JPanel P_serverDetails;

	private static void GUI() {
		Dimension sDetailsElements = new Dimension(140, 20);

		F_root = new JFrame("Serversync " + RefStrings.VERSION);
		F_root.setResizable(false);
		F_root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		F_root.setPreferredSize(new Dimension(600, 300));
		F_root.setIconImage(new ImageIcon(ClassLoader.getSystemResource("tap.png")).getImage());

		P_serverDetails = new JPanel();
		P_serverDetails.setPreferredSize(new Dimension(143, 300));

		B_sync.setPreferredSize(sDetailsElements);
		TF_ipAddress.setPreferredSize(sDetailsElements);
		TF_port.setPreferredSize(sDetailsElements);

		B_sync.setToolTipText("Starts sync process");
		B_sync.setText("Sync");
		TF_ipAddress.setOpaque(true);
		TF_port.setOpaque(true);
		TF_ipAddress.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(java.awt.event.KeyEvent e) {
				return;
			}

			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
					TF_ipAddress.transferFocus();
				}
			}

			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				return;
			}
		});
		TF_port.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(java.awt.event.KeyEvent e) {
				return;
			}

			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					F_root.requestFocus();
					startWorker();
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
		ipLabel.setLabelFor(TF_ipAddress);
		portLabel.setLabelFor(TF_port);

		JScrollPane sp_console = new JScrollPane(TA_info);
		sp_console.setPreferredSize(new Dimension(450, 300));

		P_serverDetails.add(ipLabel);
		P_serverDetails.add(TF_ipAddress);
		P_serverDetails.add(portLabel);
		P_serverDetails.add(TF_port);
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
				startWorker();
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
				SyncConfig.getServerDetails(config);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			TF_ipAddress.setText(SyncConfig.SERVER_IP);
			TF_port.setText(Integer.toString(SyncConfig.SERVER_PORT));
			SyncConfig.pullServerConfig = false;
			SyncConfig.configPresent = true;
			TF_port.requestFocus();
		} else {
			// TODO create client config if none exists
			SyncConfig.createClient(config);
			try {
				SyncConfig.getServerDetails(config);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				GUI();
			}

		});
	}

	public static void updateText(String string) {
		// TODO UI appears to freeze under strain
		TA_info.setText(string);
	}

	public static void updateProgress(int progress) {
		B_sync.setText(progress + "%");
	}

	public static void updateFileProgress(String progress) {
		F_root.setTitle(progress);
	}

	private static void startWorker() {
		boolean error = false;
		Thread t;
		if (TF_ipAddress.getText().equals("") || TF_port.getText().equals("")) {
			updateText("No config found, requesting details");
			if (TF_ipAddress.getText().equals("")) {
				String serverIP = (String) JOptionPane.showInputDialog("Server IP address");
				TF_ipAddress.setText(serverIP);
			}
			if (TF_port.getText().equals("")) {
				String serverPort = (String) JOptionPane.showInputDialog("Server Port (numbers only)");
				TF_port.setText(serverPort);
			}
			SyncConfig.pullServerConfig = true;
		}

		int port = 0;
		try {
			port = Integer.parseInt(TF_port.getText());
			if (!(port <= 49151 && port > 0)) {
				error = true;
				updateText("Port out of range, valid range: 1 - 49151");
			}
		} catch (NumberFormatException e) {
			error = true;
			updateText("Invalid port");
		}
		SyncConfig.SERVER_PORT = port;
		SyncConfig.SERVER_IP = TF_ipAddress.getText();

		if (!error) {
			updateText("Starting update process...");
			toggleButton();
			t = new Thread(new ClientWorker());
			t.start();
		}

	}

	public static void toggleButton() {
		class ButtonToggle implements Runnable {

			@Override
			public void run() {
				if (B_sync.isEnabled()) {
					B_sync.setEnabled(false);
				} else {
					B_sync.setEnabled(true);
					B_sync.setText("Sync");
				}
			}
			
		}
		SwingUtilities.invokeLater(new ButtonToggle());
	}

}
