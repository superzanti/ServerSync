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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.superzanti.serversync.SyncConfig;

/**
 * Holds relevant information about mods obtianed through mcmod.info.<br>
 * <br>
 * Use CLIENT_MODPATH for any interactions on the client side, this will cause
 * <br>
 * the mod to look in the clients mods/ directory regardless of where the file
 * <br>
 * is on the server<br>
 * <br>
 * 
 * Useful for instance when the server sends client-side mods from the
 * clientmods<br>
 * directory
 * 
 * @author Rheimus
 *
 */
public class SyncFile implements Serializable {
	private static final long serialVersionUID = -3869215783959173682L;
	public String version;
	public String name;
	public String fileName;
	transient public Path MODPATH;
	transient public Path CLIENT_MODPATH;
	public boolean clientOnlyMod = false;
	public boolean isConfig = false;
	private boolean isIgnored = false;
	public static final String UNKNOWN_VERSION = "unknown_version";
	public static final String UNKNOWN_NAME = "unknown_name";

	private File serMODPATH;
	private File serCLIENT_MODPATH;

	/**
	 * Holds various information about mods, Use CLIENT_MODPATH for client side
	 * operations<br>
	 * this will parse files to the appropriate directory for the client, such
	 * as client-only mods
	 * 
	 * @param modPath
	 * @param isMod
	 * @throws IOException
	 */
	public SyncFile(Path modPath, boolean isMod) throws IOException {
		MODPATH = modPath;
		Path cModPath = modPath;
		Path root = Paths.get("../");
		// TODO update this code chunk to be more OOP
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

		if (isMod && isZipJar(fileName)) {
			populateModInformation();
		}

		if (version == null) {
			version = SyncFile.UNKNOWN_VERSION;
		}
		if (name == null) {
			name = SyncFile.UNKNOWN_NAME;
		}
	}

	/**
	 * Shortcut constructor that assumes the created file is a mod
	 * 
	 * @param modPath
	 *            - Path to the mod
	 * @throws IOException
	 */
	public SyncFile(Path modPath) throws IOException {
		this(modPath, true);
	}

	public boolean isSetToIgnore() {
		if (SyncConfig.IGNORE_LIST.contains(fileName)) {
			isIgnored = true;
		}
		return isIgnored;
	}

	public boolean isIncluded() {
		List<String> includes = SyncConfig.INCLUDE_LIST;
		// Strip witespace
		String cleanedName = fileName.replaceAll(" ", "");
		if (includes.contains(cleanedName)) {
			return true;
		}
		return false;
	}

	private boolean isZipJar(String fileName) throws IOException {
		// TODO find better way to do this
		boolean isZip = false;
		if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
			isZip = true;
		}

		return isZip;
	}

	private void populateModInformation() throws IOException {
		if (Files.exists(MODPATH)) {
			JarFile packagedMod = new JarFile(MODPATH.toFile());
			JarEntry modInfo = packagedMod.getJarEntry("mcmod.info");
			if (modInfo != null) {
				InputStream is = packagedMod.getInputStream(modInfo);
				InputStreamReader read = new InputStreamReader(is);
				JsonStreamParser parser = new JsonStreamParser(read);
				
				while (parser.hasNext()) {
					JsonElement element = parser.next();
					if (element.isJsonArray()) {
						// This will be the opening document array
						JsonArray jArray = element.getAsJsonArray();
						
						// Get each array of objects
						// array 1 {"foo":"bar"}, array 2 {"foo":"bar"}
						for (JsonElement jObject : jArray) {
							// This will contain all of the mod info
							JsonObject info = jObject.getAsJsonObject();
							version = info.get("version").getAsString();
							name = info.get("name").getAsString();
						}
					}
				}
				read.close();
				is.close();
				packagedMod.close();

			}
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
	public boolean compare(SyncFile serversMod) {
		System.out.println(serversMod.version + " : " + version);
		if (!serversMod.version.equals(SyncFile.UNKNOWN_VERSION)) {
			return this.version.equals(serversMod.version);
		}
		return false;
	}

	public boolean delete() throws IOException {
		return Files.deleteIfExists(MODPATH);
	}

	public void deleteOnExit() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Files.delete(MODPATH);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static ArrayList<String> listModNames(List<SyncFile>... modLists) {
		if (modLists != null && modLists.length > 0) {
			ArrayList<String> names = new ArrayList<String>();
			int len = modLists.length;
			for (int i = 0; i < len; i++) {
				for (SyncFile mod : modLists[i]) {
					names.add(mod.fileName);
				}
			}
			return names;
		}
		return null;
	}

	public static ArrayList<SyncFile> parseList(List<Path> paths) throws IOException {
		ArrayList<SyncFile> mods = new ArrayList<SyncFile>();
		for (Path path : paths) {
			mods.add(new SyncFile(path));
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
