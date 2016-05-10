package com.superzanti.serversync.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.superzanti.lib.RefStrings;

import runme.Main;

public class FileProgress implements Runnable {
	
	StringBuilder sb;
	
	public void updateProgress(int progress,String fileName) {
		sb = new StringBuilder(100);
		sb.append("<");
		sb.append(progress);
		sb.append("%> ");
		sb.append("Updating: ");
		sb.append(fileName);
		try {
			SwingUtilities.invokeAndWait(this);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void resetTitle() {
		sb = new StringBuilder(18);
		sb.append("Serversync - " + RefStrings.VERSION);
		try {
			SwingUtilities.invokeAndWait(this);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		if (sb != null) {
			Main.updateFileProgress(sb.toString());
		}
	}

}
