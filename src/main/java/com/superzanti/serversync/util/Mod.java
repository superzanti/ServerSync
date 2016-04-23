package com.superzanti.serversync.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.superzanti.serversync.ServerSyncConfig;

/**
 * Holds relevant information about mods obtianed through mcmod.info.<br>
 * <br>
 * Use CLIENT_MODPATH for any interactions on the client side, this will cause<br>
 * the mod to look in the clients mods/ directory regardless of where the file<br>
 * is on the server<br><br>
 * 
 * Useful for instance when the server sends client-side mods from the clientmods<br>
 * directory
 * 
 * @author Rheimus
 *
 */
public class Mod implements Serializable {
	private static final long serialVersionUID = -3869215783959173682L;
	public String version;
	public String name;
	public String fileName;
	transient public Path MODPATH;
	//TODO fix client modpath
	transient public Path CLIENT_MODPATH;
	public boolean clientOnlyMod = false;
	public boolean isConfig = false;
	private boolean isIgnored = false;
	public static final String UNKNOWN_VERSION = "unknown_version";
	public static final String UNKNOWN_NAME = "unknown_name";
	
	private File serMODPATH;
	private File serCLIENT_MODPATH;
	
	/**
	 * Holds various information about mods, Use CLIENT_MODPATH for client side operations<br>
	 * this will parse files to the appropriate directory for the client, such as client-only mods
	 * 
	 * @param modPath
	 * @param isMod
	 * @throws IOException
	 */
	public Mod(Path modPath, boolean isMod) throws IOException {
		MODPATH = modPath;
		Path cModPath = modPath;
		Path root = Paths.get("../");
		if (modPath.toString().contains("clientmods")) {
			clientOnlyMod = true;
			cModPath = root.relativize(Paths.get(modPath.toString().replaceFirst("clientmods", "mods")));
		} else {
			cModPath = root.relativize(cModPath);
		}
		CLIENT_MODPATH = cModPath;
		fileName = MODPATH.getFileName().toString();
		
		if (fileName.contains(".cfg")) {
			isConfig = true;
			isMod = false;
		}
		
		if (isMod) {
			populateModInformation();
		}
		
		if (version == null) {
			version = Mod.UNKNOWN_VERSION;
		}
		if (name == null) {
			name = Mod.UNKNOWN_NAME;
		}
	}
	
	/**
	 * Shortcut constructor that assumes the created file is a mod
	 * @param modPath - Path to the mod
	 * @throws IOException
	 */
	public Mod(Path modPath) throws IOException {
		// TODO make serializable
		this(modPath,true);
	}
	
	public boolean isSetToIgnore() {
		if (ServerSyncConfig.IGNORE_LIST.contains(fileName)) {
			isIgnored = true;
		}
		return isIgnored;
	}
	
	public boolean isIncluded() {
		List<String> includes = ServerSyncConfig.INCLUDE_LIST;
		// Strip witespace
		String cleanedName = fileName.replaceAll(" ", "");
		if (includes.contains(cleanedName)) {
			return true;
		}
		return false;
	}
	
	private void populateModInformation() throws IOException {
		if (Files.exists(MODPATH)) {
			JarFile packagedMod = new JarFile(MODPATH.toFile());
			JarEntry modInfo = packagedMod.getJarEntry("mcmod.info");
			if (modInfo != null) {
				InputStream is = packagedMod.getInputStream(modInfo);
				JsonReader jReader = new JsonReader(new InputStreamReader(is));
				// Returns in order?
				while (jReader.hasNext()) {
					JsonToken nextToken = jReader.peek();
					if (nextToken.equals(JsonToken.NAME)) {

						String nextName = jReader.nextName();
						if (nextName.equals("version")) {
							version = jReader.nextString();
							break; // This break should in theory always come
									// after the name has been obtained, saves
									// reading the whole file
						}
						if (nextName.equals("name")) {
							name = jReader.nextString();
						}

					}
					// Dumping parts that I don't need
					if (nextToken.equals(JsonToken.STRING)) {
						jReader.nextString();
					}
					if (nextToken.equals(JsonToken.BEGIN_ARRAY)) {
						jReader.beginArray();
					}
					if (nextToken.equals(JsonToken.END_ARRAY)) {
						jReader.endArray();
					}
					if (nextToken.equals(JsonToken.BEGIN_OBJECT)) {
						jReader.beginObject();
					}
					if (nextToken.equals(JsonToken.END_OBJECT)) {
						jReader.endObject();
					}
				}
				jReader.close();
				is.close();
			}
			packagedMod.close();
		}
	}

	/**
	 * Compares mod versions
	 * 
	 * @param serversMod
	 *            - servers version of the mod
	 * @return True if versions are the same<br>
	 *         False if versions are different or if version is unknown
	 */
	public boolean compare(Mod serversMod) {
		System.out.println(serversMod.version + " : " + version);
		if (!serversMod.version.equals(Mod.UNKNOWN_VERSION)) {
			return this.version.equals(serversMod.version);
		}
		return false;
	}

	public boolean delete() throws IOException {
		return Files.deleteIfExists(MODPATH);
	}

	public void deleteOnExit() {
		// TODO NIO version of this?
		// MODPATH.toFile().deleteOnExit(); // Old school IO method
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Files.delete(MODPATH);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	public static ArrayList<String> listModNames(List<Mod>... modLists) {
		if (modLists != null && modLists.length > 0) {
			ArrayList<String> names = new ArrayList<String>();
			int len = modLists.length;
			for (int i = 0; i < len; i++) {
				for (Mod mod : modLists[i]) {
					names.add(mod.fileName);
				}
			}
			return names;
		}
		return null;
	}

	public static ArrayList<Mod> parseList(List<String> paths) throws IOException {
		ArrayList<Mod> mods = new ArrayList<Mod>();
		for (String path : paths) {
			Path p = Paths.get(path);
			mods.add(new Mod(p));

		}
		return mods;
	}
	
	private void readObject(ObjectInputStream is) throws ClassNotFoundException, IOException {
		is.defaultReadObject();
		if (serMODPATH != null) {
			MODPATH = serMODPATH.toPath();
		}
		if (serCLIENT_MODPATH != null) {
			CLIENT_MODPATH = serCLIENT_MODPATH.toPath();
		}
	}
	
	private void writeObject(ObjectOutputStream os) throws IOException {
		serMODPATH = MODPATH.toFile();
		serCLIENT_MODPATH = CLIENT_MODPATH.toFile();
		os.defaultWriteObject();
	}

}
