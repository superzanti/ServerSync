package com.superzanti.serversync.forgeloader;

import java.io.IOException;

import com.superzanti.lib.RefStrings;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import runme.Main;

@Mod(modid="com.superzanti.serversync", name="ServerSync", version=RefStrings.VERSION, acceptedMinecraftVersions="*")
public class ForgeLoaderCPW {
	@EventHandler
	public void startServersync(FMLPreInitializationEvent _e) {
		try {
			Main.main(new String[]{"server"});
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
