package com.superzanti.serversync;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.superzanti.lib.RefStrings;
import com.superzanti.serversync.proxy.ClientProxy;
import com.superzanti.serversync.util.GuiScreenHandler;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.common.MinecraftForge;

@SideOnly(Side.CLIENT)
public class SyncClient implements GuiYesNoCallback{

	protected static ClientWorker clientConnection = new ClientWorker();
	protected static Thread thread = new Thread(clientConnection);
	
	private static GuiMainMenu guimainmenu;
	private static GuiYesNo guiyesno;
	private static GuiErrorScreen guierrorscreen;
	private static GuiScreenWorking guiscreenworking;
	
	public static Path absoluteModsDirectory = null;
	
	
	public SyncClient() {
		ServerSync.logger.info("Client Selected! Read for client routine...");
	}
	
	/**
	 * Sets up GUI and starts off the synchronization process
	 * @throws Exception
	 */
	public void runClient() throws Exception {
		
		guiscreenworking = new GuiScreenWorking();
		Minecraft.getMinecraft().displayGuiScreen(guiscreenworking);
		updateScreenWorking(0,"Checking for server updates...");
		
        thread.start();
        
	}
	
	/**
	 * Updates GUI with new progress and message
	 * @param newPercent Percentage completed
	 * @param statusMessage Displayed message on GUI
	 */
	protected static void updateScreenWorking(int newPercent, String statusMessage){
		guiscreenworking.displayProgressMessage(statusMessage);
		guiscreenworking.setLoadingProgress(newPercent);
		guiscreenworking.updateScreen();
	}
	
	@SubscribeEvent
	public void onEachTick(DrawScreenEvent.Pre event){
		
		if(ClientWorker.getFinished()){
			if(ClientWorker.getErrors()){
				guierrorscreen = new GuiErrorScreen("There was an error while connecting", "Is your config file is the same as the server's? Is the server on?");		
				Minecraft.getMinecraft().displayGuiScreen(guierrorscreen);
				GuiScreenHandler.doesButtonWork = false;
			} else if (ClientWorker.getUpdates()){
				
				guiyesno = new GuiYesNo((GuiYesNoCallback) this, "You will need to re-launch minecraft to apply the changes.", "Would you like to do this now?", 0);				
				Minecraft.getMinecraft().displayGuiScreen(guiyesno);
				GuiScreenHandler.doesButtonWork = false;
			} else {
	        	FMLClientHandler.instance().connectToServerAtStartup(ServerSyncConfig.SERVER_IP, ServerSyncConfig.MINECRAFT_PORT);
	        	GuiScreenHandler.doesButtonWork = true;
			}
			MinecraftForge.EVENT_BUS.unregister(ClientProxy.getClient());
		}
		
	}
	
	/**
	 * Adds shutdown hook to run mod deletion code and closes/returns to main menu
	 */
	@Override
	public void confirmClicked(boolean yesButton, int whatsThisInt) {
		final class Shutdown extends Thread {
			String modsDir = Paths.get("mods/").toAbsolutePath().toString();

			@Override
			public void run() {
				try {
					//new File(modsDir + "/imHere").createNewFile();// Creates in base mods foler
					// Not running ? needs more sleep time perhaps in runme.Delete
					Runtime.getRuntime().exec("java -cp "+RefStrings.MODID+"-"+RefStrings.VERSION+".jar runme.Main", null, new File(modsDir));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		
		if(yesButton) {
			Runtime.getRuntime().addShutdownHook(new Shutdown());
			FMLCommonHandler.instance().exitJava(0, false);
		} else {
			Runtime.getRuntime().addShutdownHook(new Shutdown());
			Minecraft.getMinecraft().displayGuiScreen(guimainmenu);
		}
	}

}
