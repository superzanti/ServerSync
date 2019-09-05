package com.superzanti.serversync.config;

import com.superzanti.serversync.util.enums.EFriendlyConfigCategories;
import com.superzanti.serversync.util.enums.EFriendlyConfigEntries;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfig;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfigCategory;
import com.superzanti.serversync.util.minecraft.config.FriendlyConfigElement;

import java.util.ArrayList;
import java.util.Locale;

public class ClientConfigDefault {
    private String ip = "127.0.0.1";
    private int port = 38067;
    private boolean refuseClientOnlyFiles = false;

    private FriendlyConfig config = new FriendlyConfig();

    // TODO trim down this fluff
    ClientConfigDefault() {
        // GENERAL
        FriendlyConfigCategory general = new FriendlyConfigCategory(EFriendlyConfigCategories.GENERAL.getValue());
        FriendlyConfigElement refuseClientOnlyFiles = new FriendlyConfigElement(
            EFriendlyConfigCategories.GENERAL.getValue(),
            "B",
            EFriendlyConfigEntries.REFUSE_CLIENT_MODS.getValue(),
            "false",
            new ArrayList<>(0)
        );
        general.add(refuseClientOnlyFiles);
        config.put(EFriendlyConfigCategories.GENERAL.getValue(), general);

        // CONNECTION
        FriendlyConfigCategory connection = new FriendlyConfigCategory(EFriendlyConfigCategories.CONNECTION.getValue());
        FriendlyConfigElement ip = new FriendlyConfigElement(
            EFriendlyConfigCategories.CONNECTION.getValue(),
            "S",
            EFriendlyConfigEntries.SERVER_IP.getValue(),
            "127.0.0.1",
            new ArrayList<>(0)
        );
        connection.add(ip);
        FriendlyConfigElement port = new FriendlyConfigElement(
            EFriendlyConfigCategories.CONNECTION.getValue(),
            "I",
            EFriendlyConfigEntries.SERVER_PORT.getValue(),
            "38067",
            new ArrayList<>(0)
        );
        connection.add(port);
        config.put(EFriendlyConfigCategories.CONNECTION.getValue(), connection);

        // RULES
        FriendlyConfigCategory rules = new FriendlyConfigCategory(EFriendlyConfigCategories.RULES.getValue());
        FriendlyConfigElement fileIgnoreList = new FriendlyConfigElement(
            EFriendlyConfigCategories.RULES.getValue(),
            "S",
            EFriendlyConfigEntries.FILE_IGNORE_LIST.getValue(),
            new ArrayList<>(0),
            new ArrayList<>(0)
        );
        config.put(EFriendlyConfigCategories.RULES.getValue(), rules);

        // MISC
        FriendlyConfigCategory misc = new FriendlyConfigCategory(EFriendlyConfigCategories.MISC.getValue());
        FriendlyConfigElement locale = new FriendlyConfigElement(
            EFriendlyConfigCategories.MISC.getValue(),
            "S",
            EFriendlyConfigEntries.LOCALE.getValue(),
            Locale.getDefault().toString(),
            new ArrayList<>(0)
        );
    }

    public FriendlyConfig getConfig() {
        return config;
    }
}
