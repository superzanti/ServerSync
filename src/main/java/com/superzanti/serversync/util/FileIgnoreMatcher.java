package com.superzanti.serversync.util;

import java.nio.file.Path;

import com.superzanti.serversync.ServerSync;

/**
 * Shortcut to match against configs <i>ignore</i> list
 *
 * @author Rheimus
 *
 */
public class FileIgnoreMatcher extends GlobPathMatcher {
	@Override
	public boolean matches(Path path) {
		for (String pattern : ServerSync.CONFIG.FILE_IGNORE_LIST) {
			super.setPattern(pattern);
			
			if (super.matches(path)) {
				return true;
			}
		}
		return false;
	}
}
