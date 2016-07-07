package runme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;

import com.superzanti.lib.RefStrings;
import com.superzanti.serversync.ClientWorker;
import com.superzanti.serversync.SyncConfig;

public class Main {

	/* AWT EVENT DISPATCHER THREAD */

	public static final String SECURE_FILESIZE = "11b4278c7e5a79003db77272c1ed2cf5";
	public static final String SECURE_PUSH_CLIENTMODS = "0ad95bb1734520dc1fa3c737f8a57d91";

	private static JTextArea TA_info = new JTextArea();
	private static JLabel ipLabel = new JLabel("IP Address");
	private static JTextField TF_ipAddress = new JTextField();
	private static JLabel portLabel = new JLabel("Port");
	private static JTextField TF_port = new JTextField();
	private static JButton B_sync = new JButton("Sync");
	private static JFrame F_root;
	private static JPanel P_serverDetails;
	public static ResourceBundle strings;
	private static final JProgressBar PB_fileProgress = new JProgressBar();
	private static final JPanel P_information = new JPanel();
	private static TitledBorder tA_border_title;

	private static void GUI() {
		Dimension sDetailsElements = new Dimension(120, 20);
		SoftBevelBorder TF_border = new SoftBevelBorder(BevelBorder.LOWERED, new Color(64, 64, 64), new Color(192, 192, 192), new Color(64, 64, 64), new Color(0, 0, 0));

		F_root = new JFrame("Serversync");
		F_root.getContentPane().setBackground(new Color(0, 0, 0));
		F_root.setResizable(false);
		F_root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		F_root.setPreferredSize(new Dimension(600, 300));
		F_root.setIconImage(new ImageIcon(ClassLoader.getSystemResource("ServersyncLogo.png")).getImage());

		P_serverDetails = new JPanel();
		P_serverDetails.setBackground(Color.DARK_GRAY);
		P_serverDetails.setPreferredSize(new Dimension(143, 300));

		B_sync.setPreferredSize(sDetailsElements);
		B_sync.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		TF_ipAddress.setCaretColor(Color.WHITE);
		TF_ipAddress.setHorizontalAlignment(SwingConstants.CENTER);
		TF_ipAddress.setForeground(new Color(50, 205, 50));
		TF_ipAddress.setBorder(TF_border);
		TF_ipAddress.setBackground(new Color(30, 30, 30));
		TF_ipAddress.setPreferredSize(sDetailsElements);
		TF_port.setCaretColor(Color.WHITE);
		TF_port.setHorizontalAlignment(SwingConstants.CENTER);
		TF_port.setForeground(new Color(50, 205, 50));
		TF_port.setBackground(new Color(30, 30, 30));
		TF_port.setBorder(TF_border);
		TF_port.setPreferredSize(sDetailsElements);

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
		ipLabel.setForeground(Color.WHITE);

		ipLabel.setLabelFor(TF_ipAddress);
		portLabel.setForeground(Color.WHITE);
		portLabel.setLabelFor(TF_port);

		P_serverDetails.add(ipLabel);
		P_serverDetails.add(TF_ipAddress);
		P_serverDetails.add(portLabel);
		P_serverDetails.add(TF_port);
		P_serverDetails.add(B_sync);
		P_information.setBorder(new EmptyBorder(0, 0, 0, 0));

		F_root.getContentPane().add(P_information, BorderLayout.EAST);
		P_information.setLayout(new BorderLayout(0, 0));
		P_information.add(PB_fileProgress, BorderLayout.NORTH);
		PB_fileProgress.setForeground(Color.GREEN);
		PB_fileProgress.setStringPainted(true);
		PB_fileProgress.setVisible(false);
		PB_fileProgress.setBorder(BorderFactory.createEmptyBorder());
		TA_info.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		TA_info.setBackground(Color.GRAY);
		
		JScrollPane sp_console = new JScrollPane(TA_info);
		sp_console.setBorder(new EmptyBorder(0, 0, 0, 0));
		sp_console.setViewportBorder(new EmptyBorder(0, 0, 0, 0));
		P_information.add(sp_console);
		sp_console.setPreferredSize(new Dimension(450, 300));

		TA_info.setLineWrap(true);
		TA_info.setWrapStyleWord(true);
		TA_info.setEditable(false);
		tA_border_title = new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Information", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(255, 255, 255));
		CompoundBorder TA_border = new CompoundBorder(tA_border_title, new EmptyBorder(5, 10, 5, 10));
		TA_info.setBorder(TA_border);
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

	public static void GUIInitStrings() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				F_root.setTitle(strings.getString("title") +" - "+ RefStrings.VERSION);
				ipLabel.setText(strings.getString("server_address"));
				portLabel.setText(strings.getString("server_port"));
				B_sync.setText(strings.getString("go_button"));
				B_sync.setToolTipText(strings.getString("button_tooltip"));
				tA_border_title.setTitle(strings.getString("console_title"));
				TA_info.repaint();
			}
		});
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				GUI();
			}

		});
		try {
			System.out.println("Loading language file: " + SyncConfig.locale.toString());
			strings = ResourceBundle.getBundle("assets.serversync.MessagesBundle", SyncConfig.locale);
		} catch (MissingResourceException e) {
			updateText("No language file available for: " + SyncConfig.locale + ", defaulting to en_US");
			Thread.sleep(500); // Give user time to read the message
			strings = ResourceBundle.getBundle("assets.serversync.lang.MessagesBundle", new Locale("en", "US"));
		} finally {
			GUIInitStrings();
		}
	}

	public static void updateText(String string) {
		// TODO UI appears to freeze under strain
		TA_info.setText(string);
	}

	public static void updateProgress(int progress) {
		B_sync.setText(progress + "%");
	}

	public static void updateFileProgress(String message, int progress) {
		if (!PB_fileProgress.isVisible() && progress < 100) {			
			PB_fileProgress.setVisible(true);
		}
		
		PB_fileProgress.setString("<"+progress+"%> " + message);
		PB_fileProgress.setValue(progress);
		
		if (message == null) {
			PB_fileProgress.setVisible(false);
			PB_fileProgress.setString(null);
		}
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
