package com.superzanti.serversync.util.MCConfigReader;

import java.util.ArrayList;
import java.util.HashMap;

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
	
	/**
	 * Gets a list of element arrays grouped by category
	 * @return HashMap with categorys as keys
	 */
	public HashMap<String, MCCArray> getElementsByCategorys() {
		HashMap<String,MCCArray> elements = new HashMap<String, MCCArray>();
		for (MCCElement e : this) {
			if (elements.containsKey(e.getCategoryName())) {
				elements.get(e.getCategoryName()).add(e);
			} else {
				elements.put(e.getCategoryName(), new MCCArray());
				elements.get(e.getCategoryName()).add(e);
			}
		}
		if (!elements.isEmpty()) {
			return elements;
		}
		return null;
	}
}
