package com.superzanti.serversync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;

import com.superzanti.lib.RefStrings;
import com.superzanti.serversync.proxy.ClientProxy;
import com.superzanti.serversync.proxy.CommonProxy;
import com.superzanti.serversync.util.GuiScreenHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Main class, used to initialize config/logger, register proxies/events and register the mod for FML loading
 * @author superzanti
 */
@Mod(modid = RefStrings.MODID, name = RefStrings.NAME, version = RefStrings.VERSION)
public class ServerSync {
	public static Logger logger;
	
	@SidedProxy(modId = RefStrings.MODID, clientSide = "com.superzanti.serversync.proxy.ClientProxy", serverSide = "com.superzanti.serversync.proxy.CommonProxy")
	private static CommonProxy proxy;

	@EventHandler
	public static void PreLoad(FMLPreInitializationEvent PreEvent) {
		// MinecraftForge.EVENT_BUS.register(new SyncClient());

		// setup the logger for the mod
		logger = PreEvent.getModLog();

		// Grab the configuration file and load in the values
		if (proxy.isServer()) {
			try {
				Files.createDirectories(Paths.get("clientmods/"));
			} catch (IOException e) {
				logger.error("Could not create clientmods directory");
			}
		}
		ServerSyncConfig.init(PreEvent);
		

		// Client side
		if (proxy.isClient()) {
			logger.info("I am a client");
			ClientProxy.newClient();
			MinecraftForge.EVENT_BUS.register(ClientProxy.getClient());
			MinecraftForge.EVENT_BUS.register(new GuiScreenHandler());
		}

		// Server side
		if (proxy.isServer()) {
			logger.info("I am a server");
			SyncServer syncserver = new SyncServer();
			Thread syncthread = new Thread(syncserver);
			syncthread.start();
		}
		return;
	}
}
