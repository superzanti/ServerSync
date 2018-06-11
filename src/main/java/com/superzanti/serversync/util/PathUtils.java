package com.superzanti.serversync.util;

import runme.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Helper for working with paths and directories
 * 
 * @author Rheimus
 *
 */
public class PathUtils {

	private static StringBuilder pathBuilder;

	/**
	 * Go to last occurrence of target in path
	 * 
	 * @param target
	 *            what directory you want to go to
	 * @param path
	 *            the absolute path of your current directory
	 * @param offset
	 *            walk further up the chain
	 * @return String rebuilt up to the target directory <br>
	 * 
	 *         <pre>
	 * e.g. 1) target = foo, path = bar/foo/ring/ding/ 
	 * returns: bar/foo/ <br>     2) target = foo, path = bar/foo/ring/ding, offset = 1
	 * returns: bar/
	 *         </pre>
	 */
	public static String walkTo(String target, String path, int offset) {
		List<String> pathParts = getPathParts(path);
		target = target.toLowerCase();

		pathBuilder = new StringBuilder();
		int locationOfTarget = pathParts.lastIndexOf(target);

		ListIterator<String> iter = pathParts.listIterator();

		while (iter.hasNext()) {
			String part = iter.next();
			int index = iter.nextIndex();
			if (part.equalsIgnoreCase(target) && index >= locationOfTarget) {
				pathBuilder.append(part);
				pathBuilder.append("/");
				System.out.println("found target");
				System.out.println(pathBuilder.toString());
				break;
			} else {
				pathBuilder.append(part);
				pathBuilder.append("/");
				System.out.println("appended: " + part);
			}
		}

		if (offset > 0) {
			try {
				return walkUp(offset, pathBuilder.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return pathBuilder.toString();
	}

	/**
	 * Go up offset number of parts in path
	 * 
	 * @param offset
	 *            how far to walk up the path
	 * @param path
	 *            the absolute path of your current directory
	 * @return String rebuilt up to the offset amount <br>
	 * 
	 *         <pre>
	 * e.g. offset = 2, path = bar/foo/ring/ding/ 
	 * returns: bar/foo/
	 *         </pre>
	 * 
	 * @throws Exception
	 *             if offset is longer than path
	 */
	public static String walkUp(int offset, String path) throws Exception {
		List<String> pathParts = getPathParts(path);
		int ppLen = pathParts.size();
		pathBuilder = new StringBuilder();

		if (offset > ppLen) {
			throw new Exception("Offset is longer than path chain");
		}

		for (int i = 0; i < ppLen - offset; i++) {
			pathBuilder.append(pathParts.get(i));
			pathBuilder.append("/");
		}
		return pathBuilder.toString();
	}

	/**
	 * Uses Java reflection magic and ServerSync's {@linkplain Main} class to get jar file as {@linkplain File} object.
	 * @return ServerSync jar file
	 */
	public static File getServerSyncFile() {
		return new java.io.File(Main.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getPath());
	}

	/**
	 * Tries to guess Minecraft directory intelligently.
	 * @return Minecraft directory location as {@link Path} object
	 * @throws IOException if guess is wrong or path for any magical reason does not exists.
	 */
	public static Path getMinecraftDirectory() throws IOException {
		// TODO: I know there must be even more intelligent ways.
		Path minecraft;
		File jarFile = getServerSyncFile();
		String jarFilePath = jarFile.getAbsolutePath();
		String jarFileName = jarFile.getName();
		int lastIndex = -1;
		String[] directories = jarFilePath.replace("\\", "/").split("/");
		int dirsLen = directories.length - 1;
		if (directories[dirsLen].equals("mods")) {
			// Length - ServerSync jar filename - "mods/"
			// this covers mods/ServerSync.jar case
			lastIndex = jarFilePath.length() - jarFileName.length() - 5;
		} else if (directories[dirsLen].contains(".") && directories[dirsLen - 1].equals("mods")) {
			// Length - ServerSync jar filename - "/" - length of directory parenting jar file - "/"
			// this covers mods/1.12.2/ServerSync.jar case
			lastIndex = jarFilePath.length() - jarFileName.length() - 2 - directories[dirsLen - 1].length();
//		} else if (directories[dirsLen].equals(".minecraft")) {
//			// Length - jar filename - "/"
//			// this covers .minecraft/ServerSync.jar case
//			lastIndex = jarFilePath.length() - jarFileName.length() - 1;
		} else {
			// According to repository wiki, ServerSync must be placed in Minecraft directory
			lastIndex = jarFilePath.length() - jarFileName.length() - 1;
		}
		minecraft = Paths.get(jarFilePath.substring(0, lastIndex));
		return minecraft.toRealPath();
	}

	private static List<String> getPathParts(String path) {
		path = path.replace('\\', '/');
		String[] pp = path.split("/");
		List<String> ppl = new ArrayList<>();
		for (String s : pp) {
			ppl.add(s.toLowerCase());
		}
		return ppl;
	}

	public static File[] fileList(String directory) {
		File contents = new File(directory);
		return contents.listFiles();
	}

	public static ArrayList<Path> fileListDeep(Path dir) {
		try {
			if (Files.exists(dir)) {
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
			} else {
				return null;
			}

		} catch (IOException e) {
			System.out.println("Could not traverse directory");
		}
		return null;
	}
}
