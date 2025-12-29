package com.brandonitaly.bedrockskins.pack;

public final class StringUtils {
    private StringUtils() {}

    public static String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }
}
