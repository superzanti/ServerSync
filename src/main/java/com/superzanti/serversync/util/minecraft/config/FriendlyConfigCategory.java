package com.superzanti.serversync.util.minecraft.config;

import java.util.ArrayList;

public class FriendlyConfigCategory extends ArrayList<FriendlyConfigElement> {

    private static final long serialVersionUID = 2037339872073587154L;
    private String categoryName;

    public FriendlyConfigCategory(String name) {
        categoryName = name;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
