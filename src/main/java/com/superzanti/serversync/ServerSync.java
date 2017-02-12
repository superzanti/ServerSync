package com.superzanti.serversync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerSync {
	
	//TODO Implement a logger & ditch this middleman class
	
	public static void PreLoad() {
		
		// Create clientmods directory if it does not exist
		Path clientOnlyMods = Paths.get("clientmods/");
		if (!Files.exists(clientOnlyMods)) {				
			try {
				Files.createDirectories(clientOnlyMods);
			} catch (IOException e) {
				System.out.println("Could not create clientmods directory");
			}
		}

		ServerSetup setup = new ServerSetup();
		Thread syncthread = new Thread(setup);
		syncthread.start();
	}
}
