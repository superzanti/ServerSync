package com.superzanti.serversync.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.superzanti.serversync.ServerSync;

public class Console implements Runnable {

	String text = "";
	
	public void updateText(String text) {
		this.text = text;
		try {
			SwingUtilities.invokeAndWait(this);
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		ServerSync.clientGUI.updateText(text);
	}
}
