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
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ServerSyncConfig {
	public static Configuration config;
	public static String SERVER_IP;
	public static int SERVER_PORT;
	public static int MINECRAFT_PORT;
	public static String SECURE_CHECK;
	public static String SECURE_CHECKMODS;
	public static String SECURE_RECURSIVE;
	public static String SECURE_CHECKSUM;
	public static String SECURE_UPDATE;
	public static String SECURE_EXISTS;
	public static String SECURE_EXIT;
	public static Boolean PUSH_CLIENT_MODS;
	public static final String GET_CONFIG = "GIMME";
	public static List<String> ClientMods = new ArrayList<String>();
	public static List<String> IGNORE_LIST;
	public static List<String> INCLUDE_LIST;
	public static String LAST_UPDATE;
	private static Property ignoreList;
	private static Property includeList;
	public static boolean pullServerConfig = true;
	public static boolean configPresent = false;

	@Deprecated
	public static int BUTTON_ID;

	/**
	 * Loads/Initializes config parameters from serversync.cfg
	 * 
	 * @param PreEvent
	 *            forge pre-initialization event
	 */
	public static void init(FMLPreInitializationEvent PreEvent) {
		config = new Configuration(PreEvent.getSuggestedConfigurationFile());
		try {
			setupConfig();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void init(File configFile) {
		config = new Configuration(configFile);
		try {
			setupConfig();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void getServerDetailsDirty(Path configFile) throws IOException {
		//TODO read as proper file format?
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
		MINECRAFT_PORT = Integer.parseInt(getChunk(chars, "MINECRAFT_PORT=").trim());
		SERVER_IP = getChunk(chars, "SERVER_IP=").trim();
		SERVER_PORT = Integer.parseInt(getChunk(chars, "SERVER_PORT=").trim());
		SECURE_CHECK = getChunk(chars, "SECURE_CHECK=").trim();
		SECURE_CHECKMODS = getChunk(chars, "SECURE_CHECKMODS=").trim();
		SECURE_RECURSIVE = getChunk(chars, "SECURE_RECURSIVE=").trim();
		SECURE_CHECKSUM = getChunk(chars, "SECURE_CHECKSUM=").trim();
		SECURE_UPDATE = getChunk(chars, "SECURE_UPDATE=").trim();
		SECURE_EXISTS = getChunk(chars, "SECURE_EXISTS=").trim();
		SECURE_EXIT = getChunk(chars, "SECURE_EXIT=").trim();
		LAST_UPDATE = getChunk(chars, "LAST_UPDATE=").trim();
		PUSH_CLIENT_MODS = Boolean.valueOf(getChunk(chars, "PushClientMods=").trim());
		IGNORE_LIST = getArray(chars, "IGNORE_LIST");
		INCLUDE_LIST = getArray(chars, "INCLUDE_LIST");

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

	private static void setupConfig() throws IOException {
		//TODO add accept client mods for client config file
		config.load();
		// Attempt to reset config file if old value is detected
		if (config.hasCategory("ignoredfiles")) {			
			ServerSync.logger.info("Resetting config file");
			config.removeCategory(new ConfigCategory("ignoredfiles"));
			config.removeCategory(new ConfigCategory(Configuration.CATEGORY_GENERAL));
			config.removeCategory(new ConfigCategory("gui"));
		}
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

		PUSH_CLIENT_MODS = config.getBoolean("PUSH_CLIENT_MODS", Configuration.CATEGORY_GENERAL, false,
				"set true to push client side mods from clientmods directory, set on server");

		ignoreList = config.get("Rules", "MOD_IGNORE_LIST", new String[]{},
				"These mods are ignored by serversync, list auto updates with mods added to the clientmods directory.");
		
		includeList = config.get("Rules", "CONFIG_INCLUDE_LIST", new String[]{},
				"These configs are included, by default configs are not synced.");

		if (PUSH_CLIENT_MODS) {
			String[] oldList = ignoreList.getStringList();
			Path clientMods = Paths.get("clientmods/");
			ArrayList<Path> files = PathUtils.fileListDeep(clientMods);
			ArrayList<String> saveableFiles = new ArrayList<String>();

			for (Path path : files) {
				boolean found = false;
				String saveable = path.getFileName().toString();
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
		INCLUDE_LIST = Arrays.asList(includeList.getStringList());

		LAST_UPDATE = config.getString("LAST_UPDATE", "StorageVariables", "20150608_000500",
				"DO NOT EDIT THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING! (If you are a server feel free to change it as much as you want to update your clients)");

		// loading the configuration from its file
		config.save();
	}
}
