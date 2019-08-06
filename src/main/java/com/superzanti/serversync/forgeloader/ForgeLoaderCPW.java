package com.superzanti.serversync.forgeloader;

import com.superzanti.serversync.RefStrings;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import com.superzanti.serversync.ServerSync;

@Mod(modid=RefStrings.MODID, name=RefStrings.NAME, version=RefStrings.VERSION, acceptableRemoteVersions="*")
public class ForgeLoaderCPW {
	@EventHandler
	public void startServersync(FMLPreInitializationEvent _e) {
		ServerSync.main(new String[]{"server"});
	}
}
