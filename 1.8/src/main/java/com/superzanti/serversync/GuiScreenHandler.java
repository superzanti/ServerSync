package com.superzanti.serversync;

import java.nio.file.Paths;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class GuiScreenHandler{
	
	public static boolean doesButtonWork = true;
	
	@SubscribeEvent
	public void onActionPerformedPre (ActionPerformedEvent.Post event) {
		
		if(event.button.id > 0)
			ServerSyncRegistry.logger.info("Your BUTTON_ID is: " + event.button.id);
		
		if (event.button.id == ServerSyncRegistry.BUTTON_ID) {
			// log our absolute path so the user knows where to delete things
			ServerSyncRegistry.logger.info("Files may be downloaded to this location:");
			ServerSyncRegistry.logger.info(Paths.get(".").toAbsolutePath());
			
			if(!doesButtonWork){
				GuiErrorScreen guierrorscreen = new GuiErrorScreen("A Minecraft restart is pending...", "Please restart your client to play multiplayer.");
				Minecraft.getMinecraft().displayGuiScreen(guierrorscreen);
			} else {
				MinecraftForge.EVENT_BUS.register(ClientProxy.syncclient);
				try{
					client();
				} catch (Exception e) {
					ServerSyncRegistry.logger.error("Exception caught! - " + e);
					e.printStackTrace();
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
