package com.superzanti.serversync.util;

import java.nio.file.Paths;

import com.superzanti.serversync.ServerSyncConfig;
import com.superzanti.serversync.ServerSync;
import com.superzanti.serversync.proxy.ClientProxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

/**
 * Handles GUI loading
 * @author superzanti
 */
@SideOnly(Side.CLIENT)
public class GuiScreenHandler{
	
	public static boolean doesButtonWork = true;
	
	@SubscribeEvent
	public void onActionPerformedPre (ActionPerformedEvent.Post event) {
		
		if(event.button.id > 0)
			ServerSync.logger.info("Your BUTTON_ID is: " + event.button.id);
		
		if (event.button.id == ServerSyncConfig.BUTTON_ID) {
			// log our absolute path so the user knows where to delete things
			ServerSync.logger.info("Files may be downloaded to this location:");
			ServerSync.logger.info(Paths.get(".").toAbsolutePath());
			
			if(!doesButtonWork){
				GuiErrorScreen guierrorscreen = new GuiErrorScreen("A Minecraft restart is pending...", "Please restart your client to play multiplayer.");
				Minecraft.getMinecraft().displayGuiScreen(guierrorscreen);
			} else {
				MinecraftForge.EVENT_BUS.register(ClientProxy.getClient());
				try{
					client();
				} catch (Exception e) {
					ServerSync.logger.error("Exception caught! - " + e);
					e.printStackTrace();
				}
			}
		}
		return;
	}
	
	private static void client() throws Exception {
		ClientProxy.getClient().runClient();
		return;
	}

}
