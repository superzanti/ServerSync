package com.superzanti.serversync;

import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;

@SideOnly(Side.CLIENT)
public class SyncClient implements GuiYesNoCallback{

	protected static SyncClientConnection syncclientconnecction = new SyncClientConnection();
	protected static Thread thread = new Thread(syncclientconnecction);
	
	private static GuiMainMenu guimainmenu;
	private static GuiYesNo guiyesno;
	private static GuiErrorScreen guierrorscreen;
	private static GuiScreenWorking guiscreenworking;
	
	
	protected SyncClient() {
		ServerSyncRegistry.logger.info("Client Selected! Read for client routine...");
	}
	
	protected void runClient() throws Exception {
		
		guiscreenworking = new GuiScreenWorking();
		Minecraft.getMinecraft().displayGuiScreen(guiscreenworking);
		updateScreenWorking(0,"Checking for server updates...");
		
        thread.start();
        
	}
	
	protected static void updateScreenWorking(int newPercent, String statusMessage){
		guiscreenworking.displayLoadingString(statusMessage);
		guiscreenworking.setLoadingProgress(newPercent);
		guiscreenworking.updateScreen();
	}
	
	@SubscribeEvent
	public void onEachTick(DrawScreenEvent.Pre event){
		
		if(syncclientconnecction.getFinished()){
			if(syncclientconnecction.getErrors()){
				guierrorscreen = new GuiErrorScreen("There was an error while connecting", "Is your config file is the same as the server's? Is the server on?");		
				Minecraft.getMinecraft().displayGuiScreen(guierrorscreen);
				GuiScreenHandler.doesButtonWork = false;
			} else if (syncclientconnecction.getUpdates()){
				guiyesno = new GuiYesNo((GuiYesNoCallback) this, "You will need to re-launch minecraft to apply the changes.", "Would you like to do this now?", 0);				
				Minecraft.getMinecraft().displayGuiScreen(guiyesno);
				GuiScreenHandler.doesButtonWork = false;
			} else {
	        	FMLClientHandler.instance().connectToServerAtStartup(ServerSyncRegistry.SERVER_IP, ServerSyncRegistry.MINECRAFT_PORT);
	        	GuiScreenHandler.doesButtonWork = true;
			}
			MinecraftForge.EVENT_BUS.unregister(ClientProxy.syncclient);
		}
		
	}
	
	@Override
	public void confirmClicked(boolean yesButton, int whatsThisInt) {
		if(yesButton)
			FMLCommonHandler.instance().exitJava(0, true);
		else
			Minecraft.getMinecraft().displayGuiScreen(guimainmenu);
	}

}
