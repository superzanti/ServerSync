package com.superzanti.serversync.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class FileHash {
    public static String hashFile(Path file) {
        try {
            byte[] fileBytes = Files.readAllBytes(file);
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update(fileBytes);

            // Digest file
            byte[] digest = hash.digest();
            if (digest == null) {
                Logger.debug(String.format("Failed to digest file: %s", file));
                return "";
            }

            // Convert to string
            StringBuilder sb = new StringBuilder(64);
            for (byte aDigest : digest) {
                sb.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (Exception e) {
            Logger.debug(String.format("Failed to hash file: %s", file));
            Logger.debug(e);
        }
        return "";
    }
}
