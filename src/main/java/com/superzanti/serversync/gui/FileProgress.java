package com.superzanti.serversync.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.superzanti.serversync.ServerSync;

public class FileProgress implements Runnable {
	
	StringBuilder sb;
	int progress = 0;
	
	public void updateProgress(int progress,String fileName) {
		sb = new StringBuilder(100);
		this.progress = progress;
		sb.append("Updating: ");
		sb.append(fileName);
		try {
			SwingUtilities.invokeAndWait(this);
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void fileFinished() {
		ServerSync.clientGUI.updateFileProgress(null, 0);
	}

	@Override
	public void run() {
		if (sb != null) {
			ServerSync.clientGUI.updateFileProgress(sb.toString(), progress);
		}
	}

}
