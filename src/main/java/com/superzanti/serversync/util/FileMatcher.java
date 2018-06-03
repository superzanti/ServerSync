package com.superzanti.serversync.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.superzanti.serversync.util.enums.EFileMatchingMode;

public class FileMatcher {
	private static FileIgnoreMatcher ignore = new FileIgnoreMatcher();
	private static FileIncludeMatcher include = new FileIncludeMatcher();

	public static boolean shouldIncludeFile(Path file, EFileMatchingMode mode) {
		switch (mode) {
		case INCLUDE:
			// Only include files that match patterns in the white-list, or included
			// files list
			return include.matches(file);
		case INGORE:
			// We don't want to include the file if it matches an ignore list pattern
			// as the user has declared that said file should not be included
			return !ignore.matches(file);
		default:
			throw new Error("Failed to match a mode for file matching");
		}
	}

	public static ArrayList<Path> filter(ArrayList<Path> files, EFileMatchingMode mode) {
		switch (mode) {
		case INCLUDE:
			return files.stream().filter(file -> include.matches(file))
					.collect(Collectors.toCollection(ArrayList::new));
		case INGORE:
			return files.stream().filter(file -> !ignore.matches(file))
					.collect(Collectors.toCollection(ArrayList::new));
		default:
			throw new Error("Failed to match a mode for file matching");
		}
	}
}
