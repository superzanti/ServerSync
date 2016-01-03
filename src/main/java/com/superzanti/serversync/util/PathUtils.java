package com.superzanti.serversync.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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

	public static File getMinecraftDirectory(File workDir) {
		try {
			return new File(walkUp(1, workDir.getAbsolutePath()));
		} catch (Exception e) {
			// TODO get last entry of minecraft from path
			e.printStackTrace();
		}
		return workDir;
	}

	private static List<String> getPathParts(String path) {
		path = path.replace('\\', '/');
		String[] pp = path.split("/");
		List<String> ppl = new ArrayList<String>();
		for (String s : pp) {
			ppl.add(s.toLowerCase());
		}
		return ppl;
	}

	public static File[] fileList(String directory) {
		File contents = new File(directory);
		return contents.listFiles();
	}

	public static ArrayList<String> fileListDeep(Path dir) {
		File f = dir.toFile();
		File[] files = f.listFiles();
		ArrayList<String> dirList = new ArrayList<String>();
		// Loop through all the directories and only add to the list if it's a
		// file
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					dirList.addAll(fileListDeep(Paths.get(file.getPath())));
				} else {
					dirList.add(file.toString());
				}
			}
		}
		return dirList;
	}
}
