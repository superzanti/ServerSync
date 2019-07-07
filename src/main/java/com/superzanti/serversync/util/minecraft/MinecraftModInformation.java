package com.superzanti.serversync.util.minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.superzanti.serversync.util.AutoClose;
import com.superzanti.serversync.util.Logger;

public class MinecraftModInformation implements Serializable {
	private static final long serialVersionUID = 8210520496949620158L;
	public final String version;
	public final String name;

	private MinecraftModInformation(String version, String name) {
		this.version = version;
		this.name = name;
	}

	public static MinecraftModInformation fromFile(Path path) {
		JarFile packagedMod = null;
		try {
			packagedMod = new JarFile(path.toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

		JarEntry modInfoEntry = null;

		List<String> validModInfoFiles = Arrays.asList("mcmod.info", "neimod.info");
		for (String fileName : validModInfoFiles) {
			try {
				modInfoEntry = packagedMod.getJarEntry(fileName);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}

			if (modInfoEntry != null) {
				break;
			}
		}

		if (modInfoEntry != null) {
			InputStream is = null;
			try {
				is = packagedMod.getInputStream(modInfoEntry);
			} catch (IOException e) {
				e.printStackTrace();
			}

			InputStreamReader read = new InputStreamReader(is);
			JsonStreamParser parser = new JsonStreamParser(read);
			MinecraftModInformation modInformation = null;
			List<String> desiredFields = Arrays.asList("version", "name");

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

						// At the moment we only care about these two entries in the information file
						if (desiredFields.stream().allMatch(info::has)) {
							modInformation = new MinecraftModInformation(info.get("version").getAsString(),
									info.get("name").getAsString());
							break;
						}
					}
				}
			}

			AutoClose.closeResource(read, is, packagedMod);

			if (modInformation == null) {
				Logger.debug(
					String.format("%s - Could not find the desired fields in the mod information file: %s",
						path.getFileName().toString(),
						String.join(",", desiredFields)
					)
				);
				return new MinecraftModInformation("", "");
			}
			return modInformation;
		} else {
			Logger.log(
				String.format("%s - Could not find a mod information file that matches: %s",
					path.getFileName().toString(),
					String.join(",", validModInfoFiles)
				)
			);
			return null;
		}
	}
}
