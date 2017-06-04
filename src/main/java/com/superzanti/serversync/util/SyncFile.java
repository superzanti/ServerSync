package com.superzanti.serversync.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import com.google.gson.JsonParseException;
import com.google.gson.JsonStreamParser;

/**
 * Holds all relevant information about a synchronizable file, also handles client only files
 * 
 * @author Rheimus
 *
 */
public class SyncFile implements Serializable {
	private static final long serialVersionUID = -3869215783959173682L;
	
	public final boolean isConfigurationFile;
	public final boolean isClientSideOnlyFile;
	private MinecraftModInformation minecraftInformation;
	
	private final String fileHash;
	private final File synchronizableFile;
	public String getFileName() {
		return this.synchronizableFile.getName();
	}
	public File getFile() {
		return this.synchronizableFile;
	}
	public Path getFileAsPath() {
		return this.synchronizableFile.toPath();
	}
	public Path getClientSidePath() {
		//TODO link this to a config value
		return Paths.get(this.synchronizableFile.getPath().replaceFirst("clientmods", "mods"));
	}
	
	
	/**
	 * Factory for creating a config sync file
	 * @param fileToSync
	 * @return SyncFile file set up as a config
	 */
	public static SyncFile ConfigSyncFile(Path fileToSync) {
		return new SyncFile(fileToSync, true, false);
	}
	
	/**
	 * Factory for creating a standard sync file
	 * @param fileToSync
	 * @return SyncFile set up as a standard file (normal mods)
	 */
	public static SyncFile StandardSyncFile(Path fileToSync) {
		return new SyncFile(fileToSync, false, false);
	}
	
	/**
	 * Factory for creating a client side only sync file
	 * @param fileToSync
	 * @return SyncFile set up to be a client only file <br>(stuff the client might need but the server doesn't)
	 */
	public static SyncFile ClientOnlySyncFile(Path fileToSync) {
		return new SyncFile(fileToSync, false, true);
	}
	
	private SyncFile(Path fileToSync, boolean isConfig, boolean isClientSideOnly) {
		this.synchronizableFile = fileToSync.toFile();
		this.isConfigurationFile = isConfig;
		this.isClientSideOnlyFile = isClientSideOnly;
		this.fileHash = Md5.md5String(this.synchronizableFile);
		
		if (!isConfig) {
			this.populateModInformation();
		}
	}

	public boolean matchesIgnoreListPattern() {
		FileIgnoreMatcher ignoredFiles = new FileIgnoreMatcher();
		return ignoredFiles.matches(this.getFileAsPath());
	}

	/**
	 * Only relevant for config files, should not be used in logic for normal files
	 * 
	 * @return the inclusion state of the SyncFile if it is a config <br>
	 *         or true if the SyncFile is not a config
	 */
	public boolean matchesIncludeListPattern() {
		FileIncludeMatcher includedFiles = new FileIncludeMatcher();
		if (this.isConfigurationFile) {			
			return includedFiles.matches(this.getFileAsPath());
		} else {
			return true;
		}
	}

	/**
	 * Tests file to see if it is a packaged/zipped file
	 * 
	 * @param fileName
	 * @return true if file is a package
	 */
	private boolean isZipJar(String fileName) {
		// TODO make a better way to do this, perhaps use failure of javas
		// ZippedFile class
		boolean isZip = false;
		if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
			isZip = true;
		}

		return isZip;
	}

	private void populateModInformation() {
		if (Files.exists(this.getFileAsPath()) && this.isZipJar(this.synchronizableFile.getName())) {
			InputStream is = null;
			InputStreamReader read = null;
			JsonStreamParser parser = null;
			JarFile packagedMod = null;
			try {

				packagedMod = new JarFile(this.synchronizableFile);
				JarEntry modInfo = packagedMod.getJarEntry("mcmod.info");
				
				if (modInfo != null) {
					is = packagedMod.getInputStream(modInfo);
					read = new InputStreamReader(is);
					parser = new JsonStreamParser(read);
					
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

								// Skip conditions /////////////////////////////
								if (info == null) {
									continue;
								}

								if (!info.has("version") || !info.has("name")) {
									continue;
								}
								////////////////////////////////////////////////

								this.minecraftInformation = new MinecraftModInformation(
																	info.get("version").getAsString(), 
																	info.get("name").getAsString()
																);
							}
						}
					}

					read.close();
					is.close();
					packagedMod.close();
				}

			} catch (JsonParseException e) {
				System.out.println("File: " + this.synchronizableFile.getName() + " failed to parse mcmod.info as JSON");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
					try {
						if (read != null) read.close();
						if (is != null) is.close();
						if (packagedMod != null) packagedMod.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		} else {
			System.out.println("File: " + this.synchronizableFile.getName() + " not recognized as a minecraft mod (not a problem)");
		}
	}

	/**
	 * Compares mod versions from mcmod.info or compares file contents if
	 * version could not be found
	 * 
	 * @param serversMod
	 *            - servers version of the mod
	 * @return True if versions or content are the same<br>
	 *         False if versions are different or if version is unknown and
	 *         contents could not be read
	 */
	public boolean equals(SyncFile otherSyncFile) {
		if (otherSyncFile.minecraftInformation != null && this.minecraftInformation != null) {
			return this.minecraftInformation.version.equals(otherSyncFile.minecraftInformation.version)
					&& this.minecraftInformation.name.equals(otherSyncFile.minecraftInformation.name);
		} else {
			return this.fileHash.equals(otherSyncFile.fileHash);
		}
	}

	/**
	 * Deletes the file this SyncFile refers to
	 * 
	 * @return true if file deleted successfully
	 */
	public boolean delete() {
		boolean success = false;
		try {
			System.out.println("deleting" + this.getFileName());
			// File holds resources, can't delete while it exists without using File methods
			success = this.synchronizableFile.delete();
		} catch (SecurityException e) {
			System.out.println("Could not access file: " + this.synchronizableFile.getName() + " security violation check permissions");
			e.printStackTrace();
		}
		if (!success) {
			synchronizableFile.deleteOnExit();
		}
		return success;
	}
	
	@SafeVarargs
	public static ArrayList<String> listModNames(List<SyncFile>... modLists) {
		if (modLists != null && modLists.length > 0) {
			ArrayList<String> names = new ArrayList<String>(100);
			int len = modLists.length;
			for (int i = 0; i < len; i++) {
				for (SyncFile mod : modLists[i]) {
					names.add(mod.synchronizableFile.getName());
				}
			}
			return names;
		}
		return null;
	}
}
