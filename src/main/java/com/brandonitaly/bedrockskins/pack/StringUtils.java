package com.brandonitaly.bedrockskins.pack;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StringUtils {
    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[^a-z0-9/._-]");

    private StringUtils() {}

    public static String sanitize(String name) {
        if (name == null) return "";
        return SANITIZE_PATTERN.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("_");
    }

    public static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    public static String stripExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(0, idx) : name;
    }
}