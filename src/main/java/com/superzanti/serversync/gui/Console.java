package com.superzanti.serversync.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import runme.Main;

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
		Main.clientGUI.updateText(text);
	}
}
