package com.superzanti.serversync.util.minecraft.config;

import java.util.Arrays;
import java.util.List;

/**
 * Old style config, replace with json configuration.
 * @see com.superzanti.serversync.config.JsonConfig
 */
@Deprecated
public class FriendlyConfigElement {
    private final String category;
//    private String typeTag;
    private String value;
    private final String name;
    private final List<String> values;
    private final List<String> comments;
    public final boolean isArray;
    public final boolean hasComment;

    public FriendlyConfigElement(
        String category, String type, String name, String value, String[] comments
    ) {
        this.category = category;
        this.comments = Arrays.asList(comments);
        this.name = name;
        setType(type);
        this.isArray = false;
        this.hasComment = !this.comments.isEmpty();
        this.value = value;
        this.values = null;
    }

    public FriendlyConfigElement(
        String category, String type, String name, List<String> values, String[] comments
    ) {
        this.category = category;
        this.comments = Arrays.asList(comments);
        this.hasComment = !this.comments.isEmpty();
        this.name = name;
        setType(type);
        this.values = values;
        this.isArray = true;
    }

    private void setType(String type) {
//        Type type1;
//        if (type.equals("B")) {
//            type1 = Boolean.class;
//        }
//        if (type.equals("S")) {
//            type1 = String.class;
//        }
//        if (type.equals("I")) {
//            type1 = Integer.class;
//        }
//        this.typeTag = type;
    }

    public List<String> getComments() {
        return this.comments;
    }

    public List<String> getList() {
        if (isArray) {
            return values;
        }
        return null;
    }

    public String getCategoryName() {
        return category;
    }

//    /**
//     * @return String representation of the type of data this element holds
//     */
//    public String getTypeTag() {
//        return typeTag;
//    }

    public boolean getBoolean() {
        return Boolean.parseBoolean(value);
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
