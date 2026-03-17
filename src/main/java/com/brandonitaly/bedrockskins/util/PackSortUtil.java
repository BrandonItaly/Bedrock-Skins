package com.brandonitaly.bedrockskins.util;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;

import java.util.Comparator;
import java.util.function.Function;

public final class PackSortUtil {
    private PackSortUtil() {}

    public static Comparator<String> buildPackComparator(BedrockSkinsConfig.PackSortOrder sortOrder, Function<String, String> displayNameResolver) {
        
        // 1. Sort alphabetically (forward or reverse based on settings)
        Comparator<String> nameComparator = Comparator.comparing(displayNameResolver, String.CASE_INSENSITIVE_ORDER);
        if (sortOrder == BedrockSkinsConfig.PackSortOrder.Z_TO_A) {
            nameComparator = nameComparator.reversed();
        }

        // 2. Sort by the override list FIRST, then fall back to the name comparator
        return Comparator.<String>comparingInt(k -> {
            int index = SkinPackLoader.packOrder.indexOf(k);
            return index == -1 ? Integer.MAX_VALUE : index;
        }).thenComparing(nameComparator);
    }
}