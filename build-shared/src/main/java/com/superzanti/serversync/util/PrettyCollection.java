package com.superzanti.serversync.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrettyCollection {
    public static String get(Map<?, ?> map) {
        String body = map.entrySet().stream()
                      .map(entry -> String.format("%s -> %s", entry.getKey(), entry.getValue()))
                      .collect(Collectors.joining("\n  "));
        return build(body);
    }

    public static String get(List<?> list) {
        String body = list.stream()
                       .map(Object::toString)
                       .collect(Collectors.joining("\n  "));
        return build(body);
    }

    private static void appendHeader(StringBuilder builder) {
        builder.append("{");
        builder.append("\n  ");
    }

    private static void appendFooter(StringBuilder builder) {
        builder.append("\n");
        builder.append("}");
    }

    private static String build(String body) {
        StringBuilder builder = new StringBuilder();
        appendHeader(builder);
        builder.append(body);
        appendFooter(builder);
        return builder.toString();
    }
}
