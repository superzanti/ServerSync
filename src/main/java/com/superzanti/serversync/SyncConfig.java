package com.superzanti.serversync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.MCConfigReader.MCCArray;
import com.superzanti.serversync.util.MCConfigReader.MCCElement;
import com.superzanti.serversync.util.MCConfigReader.MCCReader;
import com.superzanti.serversync.util.MCConfigReader.MCCWriter;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Handles all functionality to do with serversyncs config file and
 * other configuration properties
 * @author Rheimus
 *
 */
public class SyncConfig {
	public static Configuration config;
	private static Path configPath;
	// Connection details //////////////////
	public static String SERVER_IP;
	public static int SERVER_PORT;
	public static int MINECRAFT_PORT;
	////////////////////////////////////////
	// Server messages /////////////////////
	public static String MESSAGE_CHECK;
	public static String MESSAGE_UPDATE_NEEDED;
	public static String MESSAGE_GET_FILE_LIST;
	public static String MESSAGE_COMPARE;
	public static String MESSAGE_UPDATE;
	public static String MESSAGE_FILE_EXISTS;
	public static String MESSAGE_SERVER_EXIT;
	public static final String MESSAGE_GET_SYNCABLE_DIRECTORIES = "I WANT THAT ONE";
	public static final String MESSAGE_GET_CONFIG = "GIMME";
	public static final String MESSAGE_SEC_HANDSHAKE = "SHAKE_THAT";
	////////////////////////////////////////
	public static Boolean PUSH_CLIENT_MODS;
	public static Boolean REFUSE_CLIENT_MODS = false;
	public static List<String> ClientMods = new ArrayList<String>();
	public static String LAST_UPDATE;
	// Our lists ///////////////////////////
	public static List<String> IGNORE_LIST;
	public static List<String> INCLUDE_LIST;
	public static List<String> DIR_LIST;
	// Used by forge's config loader //
	private static Property ignoreList;
	private static Property includeList;
	private static Property dirList;
	////////////////////////////////////////
	
	public static boolean serverSide = false;
	public static boolean pullServerConfig = true;
	public static boolean configPresent = false;
	public static Locale locale = Locale.getDefault(); //TODO update this to be in the config

	/**
	 * Loads/Initializes config parameters from serversync.cfg
	 * 
	 * @param PreEvent
	 *            forge pre-initialization event
	 */
	public static void init(FMLPreInitializationEvent PreEvent) {
		File cf = PreEvent.getSuggestedConfigurationFile();
		configPath = cf.toPath();
		config = new Configuration(cf);
		try {
			setupConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void init(File configFile) {
		configPath = configFile.toPath();
		config = new Configuration(configFile);
		try {
			setupConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create default client side config
	 * @param config
	 * @return
	 */
	public static boolean createClient(Path config) {
		try {
			// Probably redundant here but hey just in case
			if (!Files.exists(config)) {
				Files.createDirectories(config.getParent()); // Fix if config dir does not exist
				Files.createFile(config);
			}
			MCCWriter cWriter = new MCCWriter(Files.newBufferedWriter(config));
			cWriter.writeOpenCategory("general");
			ArrayList<String> comments = new ArrayList<String>();
			comments.add("Set this to true to refuse client mods pushed by the server, [default: false]");
			cWriter.writeElement(new MCCElement("general", "B", "REFUSE_CLIENT_MODS", "false", comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.writeOpenCategory("rules");
			comments.add("These configs are included, by default configs are not synced.");
			cWriter.writeElement(new MCCElement("rules", "S", "CONFIG_INCLUDE_LIST", new ArrayList<String>(), comments));
			cWriter.newLines(2);
			comments.clear();
			comments.add("These mods are ignored by serversync, add your client mods here to stop serversync deleting them.");
			cWriter.writeElement(new MCCElement("rules", "S", "MOD_IGNORE_LIST", new ArrayList<String>(), comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.writeOpenCategory("serverconnection");
			comments.add("The port in which the minecraft server is running, not the serversync port [range: 1 ~ 49151, default: 25565]");
			cWriter.writeElement(new MCCElement("serverconnection", "I", "MINECRAFT_PORT", "25565", comments));
			cWriter.newLines(2);
			comments.clear();
			comments.add("The IP address of the server [default: 127.0.0.1]");
			cWriter.writeElement(new MCCElement("serverconnection", "S", "SERVER_IP", "127.0.0.1", comments));
			cWriter.newLines(2);
			comments.clear();
			comments.add("The port that your server will be serving on [range: 1 ~ 49151, default: 38067]");
			cWriter.writeElement(new MCCElement("serverconnection", "I", "SERVER_PORT", "38067", comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.writeOpenCategory("storagevariables");
			comments.add("DO NOT EDIT THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING! (If you are a server feel free to change it as much as you want to update your clients) [default: 20150608_000500]");
			cWriter.writeElement(new MCCElement("storagevariables", "S", "LAST_UPDATE", "20150608_000500", comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.close();
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean updateClient() {
		try {
			MCCWriter cWriter = new MCCWriter(Files.newBufferedWriter(configPath,StandardOpenOption.TRUNCATE_EXISTING));
			cWriter.writeOpenCategory("general");
			ArrayList<String> comments = new ArrayList<String>();
			comments.add("Set this to true to refuse client mods pushed by the server, [default: false]");
			cWriter.writeElement(new MCCElement("general", "B", "REFUSE_CLIENT_MODS", String.valueOf(REFUSE_CLIENT_MODS), comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.writeOpenCategory("rules");
			comments.add("These configs are included, by default configs are not synced.");
			cWriter.writeElement(new MCCElement("rules", "S", "CONFIG_INCLUDE_LIST", new ArrayList<String>(SyncConfig.INCLUDE_LIST), comments));
			cWriter.newLines(2);
			comments.clear();
			comments.add("These mods are ignored by serversync, add your client mods here to stop serversync deleting them.");
			cWriter.writeElement(new MCCElement("rules", "S", "MOD_IGNORE_LIST", new ArrayList<String>(SyncConfig.IGNORE_LIST), comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.writeOpenCategory("serverconnection");
			comments.add("The port in which the minecraft server is running, not the serversync port [range: 1 ~ 49151, default: 25565]");
			cWriter.writeElement(new MCCElement("serverconnection", "I", "MINECRAFT_PORT", String.valueOf(SyncConfig.MINECRAFT_PORT), comments));
			cWriter.newLines(2);
			comments.clear();
			comments.add("The IP address of the server [default: 127.0.0.1]");
			cWriter.writeElement(new MCCElement("serverconnection", "S", "SERVER_IP", SyncConfig.SERVER_IP, comments));
			cWriter.newLines(2);
			comments.clear();
			comments.add("The port that your server will be serving on [range: 1 ~ 49151, default: 38067]");
			cWriter.writeElement(new MCCElement("serverconnection", "I", "SERVER_PORT", String.valueOf(SyncConfig.SERVER_PORT), comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.writeOpenCategory("storagevariables");
			comments.add("DO NOT EDIT THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING! (If you are a server feel free to change it as much as you want to update your clients) [default: 20150608_000500]");
			cWriter.writeElement(new MCCElement("storagevariables", "S", "LAST_UPDATE", SyncConfig.LAST_UPDATE, comments));
			cWriter.newLine();
			comments.clear();
			cWriter.writeCloseCategory();
			
			cWriter.close();
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void getServerDetails(Path configFile) throws IOException {
		//TODO read as proper file format?
		configPath = configFile;
		MCCReader cReader = new MCCReader(Files.newBufferedReader(configFile));
		MCCArray eArray = new MCCArray();
		MCCElement element;
		while ((element = cReader.readNextElement()) != null) {
			eArray.add(element);
		}
		cReader.close();
		
		MINECRAFT_PORT = eArray.getElementByName("MINECRAFT_PORT").getInt();
		SERVER_IP = eArray.getElementByName("SERVER_IP").getString();
		SERVER_PORT = eArray.getElementByName("SERVER_PORT").getInt();
		if (serverSide) {
			MESSAGE_CHECK = eArray.getElementByName("SECURE_CHECK").getString();
			MESSAGE_UPDATE_NEEDED = eArray.getElementByName("SECURE_CHECKMODS").getString();
			MESSAGE_GET_FILE_LIST = eArray.getElementByName("SECURE_RECURSIVE").getString();
			MESSAGE_COMPARE = eArray.getElementByName("SECURE_CHECKSUM").getString();
			MESSAGE_UPDATE = eArray.getElementByName("SECURE_UPDATE").getString();
			MESSAGE_FILE_EXISTS = eArray.getElementByName("SECURE_EXISTS").getString();
			MESSAGE_SERVER_EXIT = eArray.getElementByName("SECURE_EXIT").getString();
			PUSH_CLIENT_MODS = eArray.getElementByName("PUSH_CLIENT_MODS").getBoolean();
		} else {
			REFUSE_CLIENT_MODS = eArray.getElementByName("REFUSE_CLIENT_MODS").getBoolean();
		}
		LAST_UPDATE = eArray.getElementByName("LAST_UPDATE").getString();
		IGNORE_LIST = eArray.getElementByName("MOD_IGNORE_LIST").getList();
		INCLUDE_LIST = eArray.getElementByName("CONFIG_INCLUDE_LIST").getList();

		System.out.println("finished loading config");
	}

	/**
	 * Used to setup the Forge side config file when running as a server
	 * @throws IOException If I/O error occurs
	 */
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

		MESSAGE_CHECK = config.getString("SECURE_CHECK", "ServerEncryption", "0ba4439ee9a46d9d9f14c60f88f45f87",
				"The check command security key phrase");
		MESSAGE_UPDATE_NEEDED = config.getString("SECURE_CHECKMODS", "ServerEncryption", "3dd3152ae3e427aa2817df12570ea708",
				"The check-mods command security key phrase");
		MESSAGE_GET_FILE_LIST = config.getString("SECURE_RECURSIVE", "ServerEncryption", "f8e45531a3ea3d5c1247b004985175a4",
				"The recursive command security key phrase");
		MESSAGE_COMPARE = config.getString("SECURE_CHECKSUM", "ServerEncryption", "226190d94b21d1b0c7b1a42d855e419d",
				"The checksum command security key phrase");
		MESSAGE_UPDATE = config.getString("SECURE_UPDATE", "ServerEncryption", "3ac340832f29c11538fbe2d6f75e8bcc",
				"The update command security key phrase");
		MESSAGE_FILE_EXISTS = config.getString("SECURE_EXISTS", "ServerEncryption", "e087923eb5dd1310f5f25ddd5ae5b580",
				"The exists command security key phrase");
		MESSAGE_SERVER_EXIT = config.getString("SECURE_EXIT", "ServerEncryption", "f24f62eeb789199b9b2e467df3b1876b",
				"The exit command security key phrase");

		PUSH_CLIENT_MODS = config.getBoolean("PUSH_CLIENT_MODS", Configuration.CATEGORY_GENERAL, false,
				"set true to push client side mods from clientmods directory, set on server");

		ignoreList = config.get("Rules", "MOD_IGNORE_LIST", new String[]{},
				"These mods are ignored by serversync, list auto updates with mods added to the clientmods directory.");
		
		includeList = config.get("Rules", "CONFIG_INCLUDE_LIST", new String[]{},
				"These configs are included, by default configs are not synced.");
		
		dirList = config.get("Rules", "DIRECTORIES_INCLUDE_LIST", new String[]{"mods","config"},
				"These directories are included, by default mods and configs are included.");

		if (PUSH_CLIENT_MODS) {
			String[] oldList = ignoreList.getStringList();
			Path clientMods = Paths.get("clientmods/");
			if (Files.exists(clientMods)) {
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
				
				for (String fileName : oldList) {
					// add in previous entries
					saveableFiles.add(fileName);
				}
				// for the lulz, should sort files with mods first followed by configs
				Collections.sort(saveableFiles);
				Collections.reverse(saveableFiles);
				
				ignoreList.set(saveableFiles.toArray(new String[] {}));
			} else {
				Files.createDirectories(clientMods);
			}
		}

		IGNORE_LIST = Arrays.asList(ignoreList.getStringList());
		INCLUDE_LIST = Arrays.asList(includeList.getStringList());
		DIR_LIST = Arrays.asList(dirList.getStringList());

		LAST_UPDATE = config.getString("LAST_UPDATE", "StorageVariables", "20150608_000500",
				"DO NOT EDIT THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING! (If you are a server feel free to change it as much as you want to update your clients)");

		// loading the configuration from its file
		config.save();
	}
}
