package com.superzanti.serversync.util;

import java.nio.file.Path;

import runme.Main;

/**
 * Shortcut to match against configs <i>incldue</i> list
 * 
 * @author Rheimus
 *
 */
public class FileIncludeMatcher extends GlobPathMatcher {
	@Override
	public boolean matches(Path path) {
		for (String pattern : Main.CONFIG.CONFIG_INCLUDE_LIST) {
			super.setPattern("config/" + pattern);

			if (super.matches(path)) {
				return true;
			}
		}
		return false;
	}
}
