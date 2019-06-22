package com.superzanti.serversync.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class FileHash {

	public static String hashString(File file) {
		try {
			InputStream fin = new FileInputStream(file);
			MessageDigest hash = MessageDigest.getInstance("SHA-256");
			
			// Push file to hash
			byte[] buffer = new byte[1024];
			int read;
			do {
				read = fin.read(buffer);
				if (read > 0) {
					hash.update(buffer, 0, read);
				}
			} while (read != -1);
			fin.close();
			
			// Digest file
			byte[] digest = hash.digest();
			if (digest == null) {
				return null;
			}
			
			// Convert to string
			StringBuilder sb = new StringBuilder(64);
			for (byte aDigest : digest) {
				sb.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
			}
			
			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}

}
