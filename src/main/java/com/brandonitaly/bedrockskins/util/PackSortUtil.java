package com.brandonitaly.bedrockskins.util;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;

import java.util.Comparator;
import java.util.function.Function;

public final class PackSortUtil {
    private PackSortUtil() {}

    public static Comparator<String> buildPackComparator(BedrockSkinsConfig.PackSortOrder sortOrder, Function<String, String> displayNameResolver) {
        return switch (sortOrder) {
            case A_TO_Z -> (k1, k2) -> {
                int overrideCompare = compareOverrideRank(k1, k2);
                if (overrideCompare != 0) return overrideCompare;
                return Comparator.comparing(displayNameResolver, String.CASE_INSENSITIVE_ORDER).compare(k1, k2);
            };
            case Z_TO_A -> (k1, k2) -> {
                int overrideCompare = compareOverrideRank(k1, k2);
                if (overrideCompare != 0) return overrideCompare;
                return Comparator.comparing(displayNameResolver, String.CASE_INSENSITIVE_ORDER).reversed().compare(k1, k2);
            };
        };
    }

    private static int compareOverrideRank(String k1, String k2) {
        int i1 = SkinPackLoader.packOrder.indexOf(k1);
        int i2 = SkinPackLoader.packOrder.indexOf(k2);

        boolean o1 = i1 != -1;
        boolean o2 = i2 != -1;

        if (o1 && o2) return Integer.compare(i1, i2);
        if (o1) return -1;
        if (o2) return 1;
        return 0;
    }
}
