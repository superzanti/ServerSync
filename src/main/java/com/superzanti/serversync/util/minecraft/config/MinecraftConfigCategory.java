package com.superzanti.serversync.util.MCConfigReader;

import java.util.ArrayList;

public class MCCCategory extends ArrayList<MCCElement> {
	
	private static final long serialVersionUID = 2037339872073587154L;
	private String categoryName;
	
	public MCCCategory(String name) {
		categoryName = name;
	}
	
	public String getCategoryName() {
		return categoryName;
	}
}
