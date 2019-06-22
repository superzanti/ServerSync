package com.superzanti.serversync.util.minecraft.config;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MinecraftConfigElement {
	private final String category;
	private Type type;
	private String typeTag;
	private String value;
	private String name;
	private ArrayList<String> values;
	private ArrayList<String> comments;
	public final boolean isArray;
	public boolean hasComment;
	
	public MinecraftConfigElement(String category,String type,String name,String value, ArrayList<String> elementComments) {
		this.category = category;
		this.comments = elementComments;
		this.name = name;
		setType(type);
		this.isArray = false;
		this.hasComment = !comments.isEmpty();
		this.value = value;
		this.values = null;
	}
	
	public MinecraftConfigElement(String category,String type,String name, ArrayList<String> values, ArrayList<String> elementComments) {
		this.category = category;
		this.comments = elementComments;
		this.hasComment = !comments.isEmpty();
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
		this.typeTag = type;
	}
	
	public void addComment(String comment) {
		this.comments.add(comment);
		this.hasComment = true;
	}
	
	public void removeComments() {
		this.comments = null;
		this.hasComment = false;
	}
	
	public ArrayList<String> getComments() {
		return this.comments;
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
	
	public Type getType() {
		return type;
	}
	
	/**
	 * 
	 * @return String representation of the type of data this element holds
	 */
	public String getTypeTag() {
		return typeTag;
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
