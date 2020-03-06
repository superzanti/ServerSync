package com.superzanti.serversync.util.minecraft.config;

import java.io.IOException;
import java.util.HashMap;

public class FriendlyConfig extends HashMap<String, FriendlyConfigCategory> {
    private static final long serialVersionUID = -8812919144959413432L;

    public void writeConfig(FriendlyConfigWriter writer) {
        this.forEach((key, category) -> {

            try {
                writer.writeOpenCategory(category.getCategoryName());
                for (int i = 0; i < category.size(); i++) {
                    FriendlyConfigElement e = category.get(i);
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

    public void readConfig(FriendlyConfigReader read) {
        FriendlyConfigElement entry;
        try {
            this.clear();
            while ((entry = read.readNextElement()) != null) {
                if (this.containsKey(entry.getCategoryName())) {
                    this.get(entry.getCategoryName()).add(entry);
                } else {
                    FriendlyConfigCategory newCat = new FriendlyConfigCategory(entry.getCategoryName());
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

    public FriendlyConfigElement getEntryByName(String name) {
        for (FriendlyConfigCategory cat : this.values()) {
            for (FriendlyConfigElement e : cat) {
                if (e.getName().equals(name)) {
                    return e;
                }
            }
        }
        return null;
    }
}
