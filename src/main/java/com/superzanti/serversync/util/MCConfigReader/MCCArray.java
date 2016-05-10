package com.superzanti.serversync.util.MCConfigReader;

import java.util.ArrayList;

public class MCCArray extends ArrayList<MCCElement> {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8982760081192740589L;
	
	public MCCElement getElementByName(String name) {
		for (MCCElement e : this) {
			if (e.getName().equals(name)) {
				return e;
			}
		}
		return null;
	}
}
