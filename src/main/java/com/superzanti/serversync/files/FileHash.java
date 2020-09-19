package com.superzanti.serversync.files;

import com.superzanti.serversync.util.Logger;

import java.io.BufferedInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class FileHash {
    public static String hashFile(Path file) {
        try (
            DigestInputStream in = new DigestInputStream(
                new BufferedInputStream(Files.newInputStream(file)),
                MessageDigest.getInstance("SHA-256")
            )
        ) {
            byte[] buffer = new byte[8192];
            while (in.read(buffer) > -1) { }
            return String.format("%064x", new BigInteger(1, in.getMessageDigest().digest()));
        } catch (Exception e) {
            Logger.debug(String.format("Failed to hash file: %s", file));
            Logger.debug(e);
        }
        return "";
    }
}
