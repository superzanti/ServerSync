package com.superzanti.serversync.util.minecraft.config;

import com.sun.istack.internal.Nullable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class FriendlyConfigElement {
    private final String category;
    private Type type;
    private String typeTag;
    private String value;
    private String name;
    private List<String> values;
    private List<String> comments;
    public final boolean isArray;
    public boolean hasComment;

    public FriendlyConfigElement(
        String category, String type, String name, @Nullable String value, String[] comments
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
        String category, String name, String value, String[] comments
    ) {
        this(category, "S", name, value, comments);
    }

    public FriendlyConfigElement(
        String category, String name, int value, String[] comments
    ) {
        this(category, "I", name, String.valueOf(value), comments);
    }

    public FriendlyConfigElement(
        String category, String name, boolean value, String[] comments
    ) {
        this(category, "B", name, String.valueOf(value), comments);
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

    /**
     * @return String representation of the type of data this element holds
     */
    public String getTypeTag() {
        return typeTag;
    }

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
