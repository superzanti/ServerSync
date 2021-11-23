package com.superzanti.serversync.files;

import com.superzanti.serversync.util.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class FileHash {
    public static String hashFile(Path file) {
        try {
            InputStream stream = null;
            if( FileHash.isBinaryFile(file) ){
                stream = Files.newInputStream(file);
            }else{
                stream = new ByteArrayInputStream(String.join("", Files.readAllLines(file, StandardCharsets.UTF_8)).getBytes());
            }

            DigestInputStream in = new DigestInputStream(
                new BufferedInputStream(stream),
                MessageDigest.getInstance("SHA-256")
            );

            byte[] buffer = new byte[8192];
            while (in.read(buffer) > -1) { }
            return String.format("%064x", new BigInteger(1, in.getMessageDigest().digest()));
        } catch (Exception e) {
            Logger.debug(String.format("Failed to hash file: %s", file));
            Logger.debug(e);
        }
        return "";
    }

    private static boolean isBinaryFile(Path f) throws IOException {
        String type = Files.probeContentType(f);
        if (type == null) {
            //type couldn't be determined, assume binary
            return true;
        } else if (type.startsWith("text")) {
            return false;
        } else {
            //type isn't text
            return true;
        }
    }
}
