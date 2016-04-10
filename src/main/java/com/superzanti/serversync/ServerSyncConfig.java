package com.superzanti.serversync;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.superzanti.serversync.util.PathUtils;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ServerSyncConfig {
	protected static Configuration config;
	protected static String SERVER_IP;
	protected static int SERVER_PORT;
	protected static int MINECRAFT_PORT;
	protected static String SECURE_CHECK;
	protected static String SECURE_CHECKMODS;
	protected static String SECURE_RECURSIVE;
	protected static String SECURE_CHECKSUM;
	protected static String SECURE_UPDATE;
	protected static String SECURE_EXISTS;
	protected static String SECURE_EXIT;
	public static final String GET_CONFIG = "GIMME";
	public static boolean pullServerConfig = false;
	protected static List<String> ClientMods = new ArrayList<String>();
	public static Boolean PUSH_CLIENT_MODS;
	public static List<String> IGNORE_LIST;
	public static int BUTTON_ID;
	public static String LAST_UPDATE;
	private static Property ignoreList;

	/**
	 * Loads/Initializes config parameters from serversync.cfg
	 * 
	 * @param PreEvent
	 *            forge pre-initialization event
	 */
	public static void init(FMLPreInitializationEvent PreEvent) {
		config = new Configuration(PreEvent.getSuggestedConfigurationFile());
		config.load();
		setupConfig();
	}

	public static void init(File configFile) {
		config = new Configuration(configFile);
		config.load();
		setupConfig();
	}
	
	public static void setServerIp(String ip) {
		SERVER_IP = ip;
	}
	
	public static void setServerPort(int port) {
		SERVER_PORT = port;
	}

	public static void getServerDetailsDirty(Path configFile) throws IOException {
		BufferedReader br = Files.newBufferedReader(configFile);
		String chars = "";
		while (true) {
			if (br.ready()) {
				chars += br.readLine();
			} else {
				break;
			}
		}
		chars = chars.replaceAll("[}{]", " ");
		// System.out.println(chars);
		String port = getChunk(chars, "MINECRAFT_PORT=").trim();
		String ip = getChunk(chars, "SERVER_IP=").trim();
		List<String> ignoredFiles = getArray(chars, "IGNORE_LIST");
		String serverPort = getChunk(chars, "SERVER_PORT=").trim();
		String secureCheck = getChunk(chars, "SECURE_CHECK=").trim();
		String secureCheckMods = getChunk(chars, "SECURE_CHECKMODS=").trim();
		String secureRecursive = getChunk(chars, "SECURE_RECURSIVE=").trim();
		String secureChecksum = getChunk(chars, "SECURE_CHECKSUM=").trim();
		String secureUpdate = getChunk(chars, "SECURE_UPDATE=").trim();
		String secureExists = getChunk(chars, "SECURE_EXISTS=").trim();
		String secureExit = getChunk(chars, "SECURE_EXIT=").trim();
		String lastUpdate = getChunk(chars, "LAST_UPDATE=").trim();

		SERVER_IP = ip;
		SERVER_PORT = Integer.parseInt(serverPort);
		IGNORE_LIST = ignoredFiles;
		MINECRAFT_PORT = Integer.parseInt(port);
		SECURE_CHECK = secureCheck;
		SECURE_CHECKMODS = secureCheckMods;
		SECURE_RECURSIVE = secureRecursive;
		SECURE_CHECKSUM = secureChecksum;
		SECURE_UPDATE = secureUpdate;
		SECURE_EXISTS = secureExists;
		SECURE_EXIT = secureExit;
		LAST_UPDATE = lastUpdate;
		System.out.println("finished loading config");
	}

	private static String getChunk(String config, String target) {
		String proc = "";
		proc = config.substring(config.indexOf(target) + target.length());
		proc = proc.substring(0, proc.indexOf(" "));

		return proc;
	}

	private static List<String> getArray(String config, String target) {
		List<String> proc = new ArrayList<String>();
		String _proc = "";
		_proc = config.substring(config.indexOf(target) + target.length());
		_proc = _proc.substring(2, _proc.indexOf(">"));
		String[] dirtyArray = _proc.split("        ");
		for (String e : dirtyArray) {
			proc.add(e.trim());
		}
		return proc;
	}

	private static void setupConfig() {
		SERVER_IP = config.getString("SERVER_IP", "ServerConnection", "127.0.0.1", "The IP address of the server");
		SERVER_PORT = config.getInt("SERVER_PORT", "ServerConnection", 38067, 1, 49151,
				"The port that your server will be serving on");
		MINECRAFT_PORT = config.getInt("MINECRAFT_PORT", "ServerConnection", 25565, 1, 49151,
				"The port in which the minecraft server is running, not the serversync port");

		SECURE_CHECK = config.getString("SECURE_CHECK", "ServerEncryption", "0ba4439ee9a46d9d9f14c60f88f45f87",
				"The check command security key phrase");
		SECURE_CHECKMODS = config.getString("SECURE_CHECKMODS", "ServerEncryption", "3dd3152ae3e427aa2817df12570ea708",
				"The check-mods command security key phrase");
		SECURE_RECURSIVE = config.getString("SECURE_RECURSIVE", "ServerEncryption", "f8e45531a3ea3d5c1247b004985175a4",
				"The recursive command security key phrase");
		SECURE_CHECKSUM = config.getString("SECURE_CHECKSUM", "ServerEncryption", "226190d94b21d1b0c7b1a42d855e419d",
				"The checksum command security key phrase");
		SECURE_UPDATE = config.getString("SECURE_UPDATE", "ServerEncryption", "3ac340832f29c11538fbe2d6f75e8bcc",
				"The update command security key phrase");
		SECURE_EXISTS = config.getString("SECURE_EXISTS", "ServerEncryption", "e087923eb5dd1310f5f25ddd5ae5b580",
				"The exists command security key phrase");
		SECURE_EXIT = config.getString("SECURE_EXIT", "ServerEncryption", "f24f62eeb789199b9b2e467df3b1876b",
				"The exit command security key phrase");

		PUSH_CLIENT_MODS = config.getBoolean("PushClientMods", Configuration.CATEGORY_GENERAL, false,
				"set true to push client side mods from clientmods directory, set on server");

		List<String> defaultList = new ArrayList<String>();
		defaultList.add("./mods/CustomMainMenu-MC1.7.10-1.5.jar");
		defaultList.add("./config/CustomMainMenu/mainmenu.json");
		defaultList.add("./config/forge.cfg");
		defaultList.add("./config/forgeChunkLoading.cfg");
		defaultList.add("./config/splash.properties");
		defaultList.add("./config/NEI/client.cfg");

		ignoreList = config.get("IgnoredFiles", "IGNORE_LIST", defaultList.toArray(new String[] {}),
				"These files are ignored by serversync, list auto updates with files added to the clientmods directory. \r\nDO NOT IGNORE serversync.cfg");

		if (PUSH_CLIENT_MODS) {
			String[] oldList = ignoreList.getStringList();
			Path clientMods = Paths.get("clientmods/");
			ArrayList<String> files = PathUtils.fileListDeep(clientMods);
			ArrayList<String> saveableFiles = new ArrayList<String>();

			for (String path : files) {
				boolean found = false;
				String saveable = "./" + path.replace("\\", "/").replace("clientmods", "mods");
				// Duplicate check
				for (String oldPath : oldList) {
					if (oldPath.equals(saveable)) {
						found = true;
						break;
					}
				}
				if (!found) {
					// file not found in ignore list
					saveableFiles.add(saveable);
				}
			}

			for (String path : oldList) {
				// add in previous entries
				saveableFiles.add(path);
			}
			// for the lulz, should sort files with mods first followed by configs
			Collections.sort(saveableFiles);
			Collections.reverse(saveableFiles);

			ignoreList.set(saveableFiles.toArray(new String[] {}));
		}

		IGNORE_LIST = Arrays.asList(ignoreList.getStringList());

		BUTTON_ID = config.getInt("ButtonID", "GUI", 6001, 0, 2147483647,
				"The ID of the button that connects to the server and updates");

		LAST_UPDATE = config.getString("LAST_UPDATE", "StorageVariables", "20150608_000500",
				"DO NOT EDIT THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING! (If you are a server feel free to change it as much as you want to update your clients)");

		// loading the configuration from its file
		config.save();
	}
}
