package com.superzanti.serversync.util;

import java.util.Arrays;
import java.util.List;

public class ProgramArguments {
	
	public final boolean isServer;
	public final boolean syncSilent;
	public final boolean syncProgressOnly;
	public final boolean cleanup;
	
	public ProgramArguments(String[] arguments) {
		List<String> args = Arrays.asList(arguments);
		this.isServer = args.contains("server");
		this.syncSilent = args.contains("silent");
		this.syncProgressOnly = args.contains("progress-only");
		this.cleanup = args.contains("cleanup");
	}
}
