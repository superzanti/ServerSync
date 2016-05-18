package com.superzanti.serversync.util.MCConfigReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class MCCReader extends BufferedReader {
	
	//TODO create separate server/client config files
	//TODO have server send handshake secure codes and remove from clients config
	
	public String category;
	
	public MCCReader(BufferedReader read) throws IOException {
		super(read);
	}
	
	public MCCElement readNextElement() throws IOException {
		String line;
		ArrayList<String> elementComments = new ArrayList<String>();
		MCCElement el;
		while ((line = this.readLine()) != null) {
			//TODO read and attach comments to elements
			if (line.contains("#")) {
				elementComments.add(line);
				continue;
			}
			if (line.contains("}")) {
				category = "unsorted";
				elementComments.clear();
				continue;
			}
			if (line.contains("{")) {
				String[] cat = line.trim().split(" ");
				// Should get category name
				category = cat[0];
				elementComments.clear();
				// Move to next line
				continue;
			}
			if (line.contains(":") && line.contains("=")) {
				String type = getType(line);
				String name = getName(line);
				String value = getValue(line);
				el = new MCCElement(category,type,name,value,elementComments);
				elementComments.clear();
				return el;
			}
			if (line.contains(":") && line.contains("<")) {
				String type = getType(line);
				String name = getName(line);
				el = new MCCElement(category,type,name,getValues(),elementComments);
				elementComments.clear();
				return el;
			}
		}
		return null;
	}
	
	private ArrayList<String> getValues() throws IOException {
		ArrayList<String> temp = new ArrayList<String>();
		String line;
		while (true) {
			line = this.readLine();
			if (line.contains(">")) {
				break;
			}
			temp.add(line.replace(",", "").trim());
		}
		return temp;
	}
	
	private String getType(String line) {
		String sub = line.substring(line.indexOf(":") - 1, line.indexOf(":")).trim();
		return sub;
	}
	
	private String getName(String line) {
		String sub;
		if (line.contains("=")) {
			sub = line.substring(line.indexOf(":")+1,line.indexOf("=")).trim();
			return sub;
		}
		sub = line.substring(line.indexOf(":")+1,line.indexOf("<")).trim();
		return sub;
	}
	
	private String getValue(String line) {
		String sub = line.substring(line.indexOf("=")+1).trim();
		return sub;
	}
}
