package com.superzanti.serversync.util.MCConfigReader;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MCCElement {
	private final String category;
	private Type type;
	private String value;
	private String name;
	private ArrayList<String> values;
	public final boolean isArray;
	
	public MCCElement(String category,String type,String name,String value) {
		this.category = category;
		this.name = name;
		setType(type);
		this.isArray = false;
		this.value = value;
		this.values = null;
	}
	
	public MCCElement(String category,String type,String name, ArrayList<String> values) {
		this.category = category;
		this.name = name;
		setType(type);
		this.values = values;
		this.isArray = true;
		this.value = null;
	}
	
	private void setType(String type) {
		if (type.equals("B")) {
			this.type = Boolean.class;
		}
		if (type.equals("S")) {
			this.type = String.class;
		}
		if (type.equals("I")) {
			this.type = Integer.class;
		}
	}
	
	public ArrayList<String> getList() {
		if (isArray) {
			return values;
		}
		return null;
	}
	
	public String getCategoryName() {
		return category;
	}
	
	public java.lang.reflect.Type getType() {
		return type;
	}
	
	public boolean getBoolean() {
		return Boolean.valueOf(value);
	}
	
	public String getName() {
		return name;
	}
	
	public String getString() {
		return value;
	}
	
	public int getInt() {
		return Integer.parseInt(value);
	}
}
