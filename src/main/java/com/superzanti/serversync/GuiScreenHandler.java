package com.superzanti.serversync;

import java.nio.file.Paths;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class GuiScreenHandler{
	
	@SubscribeEvent
	public void onActionPerformedPre (ActionPerformedEvent.Post event) {
		
		if(event.button.id > 0)
			ServerSyncRegistry.logger.info("Your BUTTON_ID is: " + event.button.id);
		
		if (event.button.id == ServerSyncRegistry.BUTTON_ID) {
			// log our absolute path so the user knows where to delete things
			ServerSyncRegistry.logger.info("Files may be downloaded to this location:");
			ServerSyncRegistry.logger.info(Paths.get(".").toAbsolutePath());
			
			if(SyncClient.syncclientconnecction.getFinished()){
				GuiErrorScreen guierrorscreen = new GuiErrorScreen("A Minecraft restart is pending...", "Please restart your client to play multiplayer.");
				Minecraft.getMinecraft().displayGuiScreen(guierrorscreen);
			} else {
				try{
					client();
				} catch (Exception e) {
					ServerSyncRegistry.logger.error("Exception caught! - " + e);
				}
			}
		}
		return;
	}
	
	private static void client() throws Exception {
		ClientProxy.syncclient.runClient();
		return;
	}

}
