package com.superzanti.serversync.util.minecraft.config;

import java.util.ArrayList;

public class MinecraftConfigCategory extends ArrayList<MinecraftConfigElement> {
	
	private static final long serialVersionUID = 2037339872073587154L;
	private String categoryName;
	
	public MinecraftConfigCategory(String name) {
		categoryName = name;
	}
	
	public String getCategoryName() {
		return categoryName;
	}
}
