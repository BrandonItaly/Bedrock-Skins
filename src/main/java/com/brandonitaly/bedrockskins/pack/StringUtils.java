package com.brandonitaly.bedrockskins.pack;

public final class StringUtils {
    private StringUtils() {}

    public static String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }

    public static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : (fallback == null ? "" : fallback);
    }
}
