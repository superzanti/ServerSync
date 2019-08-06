package com.superzanti.serversync.forgeloader;

import com.superzanti.serversync.RefStrings;

import com.superzanti.serversync.ServerSync;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid=RefStrings.MODID, name=RefStrings.NAME, version=RefStrings.VERSION,
        serverSideOnly=true, acceptableRemoteVersions="*")
public class ForgeLoaderNET {
	@EventHandler
	public void startServersync(FMLPreInitializationEvent _e) {
		ServerSync.main(new String[]{"server"});
	}
}
