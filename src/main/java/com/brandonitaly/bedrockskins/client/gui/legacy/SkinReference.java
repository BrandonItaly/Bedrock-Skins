package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Adapter class to bridge LoadedSkin structure with Legacy4J widget system.
 * Represents a reference to a specific skin within a pack.
 */
public record SkinReference(String packId, int ordinal) {
    
    public static final Codec<SkinReference> CODEC = Codec.STRING.comapFlatMap(
        key -> {
            int split = key.lastIndexOf(':'); 
            if (split <= 0 || split == key.length() - 1) {
                return DataResult.error(() -> "Invalid SkinReference key: " + key);
            }
            
            try {
                String pack = key.substring(0, split);
                int index = Integer.parseInt(key.substring(split + 1));
                return DataResult.success(new SkinReference(pack, index));
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid SkinReference ordinal: " + key);
            }
        },
        SkinReference::toKey
    );

    public String toKey() {
        return packId + ":" + ordinal;
    }
}