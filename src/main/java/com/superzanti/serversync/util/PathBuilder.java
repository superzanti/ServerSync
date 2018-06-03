package com.superzanti.serversync.util;

import java.nio.file.FileSystems;

public class PathBuilder {
	private StringBuilder builder = new StringBuilder();
	
	public PathBuilder add(String segment) {
		if (builder.length() > 0) {			
			builder.append(FileSystems.getDefault().getSeparator());
		}
		builder.append(segment);
		return this;
	}

	@Override
	public String toString() {
		return builder.toString();
	}
	 
}
