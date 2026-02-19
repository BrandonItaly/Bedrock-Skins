package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.mojang.serialization.Codec;

/**
 * Adapter class to bridge LoadedSkin structure with Legacy4J widget system.
 * Represents a reference to a specific skin within a pack.
 */
public record SkinReference(String packId, int ordinal) {
    public static final Codec<SkinReference> CODEC = Codec.STRING.comapFlatMap(
        key -> {
            int split = key.indexOf(':');
            if (split <= 0 || split >= key.length() - 1) {
                return com.mojang.serialization.DataResult.error(() -> "Invalid SkinReference key: " + key);
            }
            String pack = key.substring(0, split);
            String indexPart = key.substring(split + 1);
            try {
                return com.mojang.serialization.DataResult.success(new SkinReference(pack, Integer.parseInt(indexPart)));
            } catch (NumberFormatException e) {
                return com.mojang.serialization.DataResult.error(() -> "Invalid SkinReference ordinal: " + key);
            }
        },
        SkinReference::toKey
    );

    public String toKey() {
        return packId + ":" + ordinal;
    }
    
    public static SkinReference fromKey(String key) {
        if (key == null) return null;
        return CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, new com.google.gson.JsonPrimitive(key))
            .result()
            .orElse(null);
    }
}
