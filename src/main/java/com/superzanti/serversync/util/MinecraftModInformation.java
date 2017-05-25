package com.superzanti.serversync.util;

import java.io.Serializable;

public class MinecraftModInformation implements Serializable {
	private static final long serialVersionUID = 8210520496949620158L;
	public final String version;
	public final String name;
	
	public MinecraftModInformation(String version, String name) {
		this.version = version;
		this.name = name;
	}
}
