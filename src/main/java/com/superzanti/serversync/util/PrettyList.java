package com.superzanti.serversync.util;

import java.util.List;
import java.util.stream.Collectors;

public class PrettyList {
    public static String get(List<?> list) {
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append("\n  ");
        String l = list.stream()
                       .map(Object::toString)
                       .collect(Collectors.joining("\n  "));
        b.append(l);
        b.append("\n");
        b.append("}");
        return b.toString();
    }
}
