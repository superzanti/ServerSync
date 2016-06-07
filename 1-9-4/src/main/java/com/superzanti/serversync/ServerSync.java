package com.superzanti.serversync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;

import com.superzanti.lib.RefStrings;
import com.superzanti.serversync.proxy.ClientProxy;
import com.superzanti.serversync.proxy.CommonProxy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Main class, used to initialize config/logger, register proxies/events and register the mod for FML loading
 * @author superzanti
 */
@Mod(modid = RefStrings.MODID, name = RefStrings.NAME, version = RefStrings.VERSION, acceptableRemoteVersions = "*")
public class ServerSync {
	public static Logger logger;
	
	@SidedProxy(modId = RefStrings.MODID, clientSide = "com.superzanti.serversync.proxy.ClientProxy", serverSide = "com.superzanti.serversync.proxy.CommonProxy")
	private static CommonProxy proxy;

	@Mod.EventHandler
	public static void PreLoad(FMLPreInitializationEvent PreEvent) {
		// setup the minecraft logger for the server
		logger = PreEvent.getModLog();
		
		// Create clientmods directory
		if (proxy.isServer()) {
			Path clientOnlyMods = Paths.get("clientmods/");
			if (!Files.exists(clientOnlyMods)) {				
				try {
					Files.createDirectories(clientOnlyMods);
				} catch (IOException e) {
					logger.error("Could not create clientmods directory");
				}
			}
		}
		

		// Client side
		if (proxy.isClient()) {
			logger.info("I am a client");
			MinecraftForge.EVENT_BUS.register(new ClientProxy());
		}

		// Server side
		if (proxy.isServer()) {
			logger.info("I am a server");
			// Grab the configuration file and load in the values
			SyncConfig.init(PreEvent);
			ServerSetup setup = new ServerSetup();
			Thread syncthread = new Thread(setup);
			syncthread.start();
		}
	}
}
