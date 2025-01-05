package com.superzanti.serversync.util.minecraft.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class FriendlyConfigReader extends BufferedReader {
    public String category;

    public FriendlyConfigReader(BufferedReader read) {
        super(read);
    }

    public FriendlyConfigElement readNextElement() throws IOException {
        String line;
        List<String> elementComments = new ArrayList<>();
        FriendlyConfigElement el;
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
                el = new FriendlyConfigElement(category, type, name, value, elementComments.toArray(new String[0]));
                elementComments.clear();
                return el;
            }
            if (line.contains(":") && line.contains("<")) {
                String type = getType(line);
                String name = getName(line);
                el = new FriendlyConfigElement(category, type, name, getValues(), elementComments.toArray(new String[0]));
                elementComments.clear();
                return el;
            }
        }
        return null;
    }

    private ArrayList<String> getValues() throws IOException {
        ArrayList<String> temp = new ArrayList<>();
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
        return line.substring(line.indexOf(":") - 1, line.indexOf(":")).trim();
    }

    private String getName(String line) {
        String sub;
        if (line.contains("=")) {
            sub = line.substring(line.indexOf(":") + 1, line.indexOf("=")).trim();
            return sub;
        }
        sub = line.substring(line.indexOf(":") + 1, line.indexOf("<")).trim();
        return sub;
    }

    private String getValue(String line) {
        return line.substring(line.indexOf("=") + 1).trim();
    }
}
