package com.superzanti.serversync.forgeloader;

import com.superzanti.serversync.RefStrings;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import runme.Main;

@Mod(modid=RefStrings.MODID, name=RefStrings.NAME, version=RefStrings.VERSION,
        serverSideOnly=true, acceptableRemoteVersions="*")
public class ForgeLoaderNET {
	@EventHandler
	public void startServersync(FMLPreInitializationEvent _e) {
		Main.main(new String[]{"server"});
	}
}
