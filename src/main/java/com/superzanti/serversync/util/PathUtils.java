package com.superzanti.serversync.util;

import runme.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper for working with paths and directories
 *
 * @author Rheimus
 */
public class PathUtils {
    /**
     * Uses Java reflection magic and ServerSync's {@linkplain Main} class to get
     * jar file as {@linkplain File} object.
     *
     * @return ServerSync jar file
     */
    public static File getServerSyncFile() {
        return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    /**
     * Tries to guess Minecraft directory intelligently.
     *
     * @return Minecraft directory location as {@link Path} object
     */
    public static String getMinecraftDirectory() {
        File jarFile = getServerSyncFile();
        String jarFilePath = jarFile.getAbsolutePath();

        List<String> parts = getPathParts(jarFilePath);

        if (parts.contains("file:")) {
            // Shift past the file declaration when loaded in a forge environment
            parts = parts.subList(parts.indexOf("file:") + 1, parts.size() - 1);
        }

        if (parts.contains("mods")) {
            // ASSUMPTION: We are most likely in the mods directory of a minecraft directory
            List<String> root = parts.subList(0, parts.indexOf("mods"));
            PathBuilder builder = new PathBuilder();
            root.forEach(builder::add);

            return builder.toString();
        }

        // ASSUMPTION: As users are instructed to put ServerSync in the Minecraft
        // directory we can assume that the current directory is where serversync is
        // supposed to be, as we are asking for the Minecraft directory it should be
        // handled elsewhere when the directory can not be found
        return null;
    }

    private static List<String> getPathParts(String path) {
        return Arrays.asList(path.split("[\\\\/]"));
    }

    public static File[] fileList(String directory) {
        File contents = new File(directory);
        return contents.listFiles();
    }

    public static ArrayList<Path> fileListDeep(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            throw new IOException("Attempted to list files of a directory that does not exist");
        }

        Stream<Path> ds = Files.walk(dir);

        ArrayList<Path> dirList = new ArrayList<>();

        Iterator<Path> it = ds.iterator();
        while (it.hasNext()) {
            Path tp = it.next();
            // discard directories
            if (!Files.isDirectory(tp)) {
                dirList.add(tp);
            }
        }
        ds.close();
        return dirList;
    }
}
