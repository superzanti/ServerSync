package com.superzanti.serversync.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import runme.Main;

public class Console implements Runnable {

	String text = "";
	
	public void updateText(String text) throws InvocationTargetException, InterruptedException {
		this.text = text;
		SwingUtilities.invokeAndWait(this);
	}
	
	@Override
	public void run() {
		Main.updateText(text);
	}
}
