package com.superzanti.serversync.util;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathBuilder {
	private StringBuilder builder = new StringBuilder();

	public PathBuilder() { }

	/**
	 * Initializes PathBuilder with some initial segment. It just calls add(initialSegment).
	 * @param initialSegment segment to add
	 */
	public PathBuilder(String initialSegment) {
		add(initialSegment);
	}

	/**
	 * Adds (appends) a segment to path
	 * @param segment segment to add
	 */
	public PathBuilder add(String segment) {
		if (builder.length() > 0) {			
			builder.append(FileSystems.getDefault().getSeparator());
		}
		builder.append(segment);
		return this;
	}

	/**
	 * Removes last segment.
	 */
	public PathBuilder removeLast() {
		int lastElementIndex = builder.lastIndexOf(FileSystems.getDefault().getSeparator());
		builder.delete(lastElementIndex, builder.length());
		return this;
	}

	@Override
	public String toString() {
		return builder.toString();
	}

	public Path buildPath() { return Paths.get(builder.toString()); }
}
