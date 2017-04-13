package com.superzanti.serversync.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Just a wrapper for using glob pattern matching, make sure to set a pattern to match against
 * @author Rheimus
 *
 */
public class GlobPathMatcher implements PathMatcher {
	
	private String mPattern;
	
	public void setPattern(String pattern) {
		if (File.separator.equals("\\")) {			
			this.mPattern = pattern.replaceAll("[/\\\\]", "\\\\\\\\"); // Gross
		} else {
			this.mPattern = pattern.replaceAll("[/\\\\]", File.separator);
		}
	}
	
	@Override
	public boolean matches(Path path) {
		if (this.mPattern == null) {
			System.err.println("Glob pattern not set, did you mean to do this?");
			return false;
		}
		
		PathMatcher globMatcher = FileSystems.getDefault().getPathMatcher("glob:" + this.mPattern);
		return globMatcher.matches(path);
	}
	
}
