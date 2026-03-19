package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter to organize LoadedSkins into pack-based collections
 * compatible with the Legacy4J carousel system.
 */
public record SkinPackAdapter(String packId, List<LoadedSkin> skins, String packType) {
    
    public SkinPackAdapter {
        skins = List.copyOf(skins); 
    }

    // Legacy constructor for compatibility
    public SkinPackAdapter(String packId, List<LoadedSkin> skins) {
        this(packId, skins, null);
    }

    public int size() { return skins.size(); }
    public boolean isEmpty() { return skins.isEmpty(); }
    public int indexOf(LoadedSkin skin) { return skins.indexOf(skin); }

    public LoadedSkin getSkin(int ordinal) {
        return (ordinal >= 0 && ordinal < skins.size()) ? skins.get(ordinal) : null;
    }

    /**
     * Gets all available skin packs from the SkinPackLoader.
     */
    public static Map<String, SkinPackAdapter> getAllPacks() {
        // Automatically group all loaded skins by their packId
        Map<String, List<LoadedSkin>> packMap = SkinPackLoader.loadedSkins.values().stream()
                .collect(Collectors.groupingBy(skin -> skin.packId, LinkedHashMap::new, Collectors.toList()));

        Map<String, SkinPackAdapter> packs = new LinkedHashMap<>();
        packMap.forEach((id, skins) -> 
            packs.put(id, new SkinPackAdapter(id, skins, SkinPackLoader.packTypesByPackId.get(id)))
        );

        return packs;
    }

    /**
     * Gets a specific pack by ID.
     */
    public static SkinPackAdapter getPack(String packId) {
        List<LoadedSkin> filteredSkins = SkinPackLoader.loadedSkins.values().stream()
                .filter(skin -> skin.packId.equals(packId))
                .toList();
                
        return new SkinPackAdapter(packId, filteredSkins, SkinPackLoader.packTypesByPackId.get(packId));
    }
}