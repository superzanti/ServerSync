package com.superzanti.serversync.util.MCConfigReader;

import java.io.IOException;
import java.util.HashMap;

public class MCCConfig extends HashMap<String,MCCCategory> {
	private static final long serialVersionUID = -8812919144959413432L;
	
	public void writeConfig(MCCWriter writer) {
		this.forEach((key,category) -> {
			
			try {
				writer.writeOpenCategory(category.getCategoryName());
				for (int i = 0; i < category.size(); i++) {
					MCCElement e = category.get(i);
					writer.writeElement(e);
					if (i == category.size() - 1) {
						// Last element
						writer.newLine();
					} else {
						writer.newLines(2);
					}
				}
				writer.writeCloseCategory();
			} catch (Exception e) {
				//TODO handle these errors
				e.printStackTrace();
			}
		});
		try {
			writer.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public void readConfig(MCCReader read) {
		MCCElement entry;
		try {
			this.clear();
			while((entry = read.readNextElement()) != null) {
				if (this.containsKey(entry.getCategoryName())) {
					this.get(entry.getCategoryName()).add(entry);
				} else {		
					MCCCategory newCat = new MCCCategory(entry.getCategoryName());
					newCat.add(entry);
					this.put(entry.getCategoryName(), newCat);
				}
			}
		} catch (IOException e) {
			// TODO handle these errors
			e.printStackTrace();
		}
		
		try {
			read.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public MCCElement getEntryByName(String name) {
		for (MCCCategory cat : this.values()) {
			for (MCCElement e : cat) {				
				if (e.getName().equals(name)) {
					return e;
				}
			}
		}
		return null;
	}
}
