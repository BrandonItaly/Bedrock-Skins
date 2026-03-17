package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FavoritesManager {
    private FavoritesManager() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<SkinId> favoriteIds = new ArrayList<>();

    public static void load() {
        favoriteIds.clear();
        try {
            LocalSkinConfig state = StateManager.readState();
            if (state.getFavorites() != null) {
                state.getFavorites().stream()
                    .map(SkinId::parse)
                    .filter(Objects::nonNull)
                    .forEach(favoriteIds::add);
            }
        } catch (Exception e) {
            LOGGER.error("FavoritesManager: failed to load favorites", e);
        }
    }

    public static void save() {
        try {
            SkinId selected = SkinManager.getLocalSelectedKey();
            StateManager.saveState(getFavoriteKeys(), selected == null ? null : selected.toString());
        } catch (Exception e) {
            LOGGER.error("FavoritesManager: failed to save favorites", e);
        }
    }

    public static boolean isFavorite(LoadedSkin skin) {
        return skin != null && favoriteIds.contains(skin.skinId);
    }

    public static void addFavorite(LoadedSkin skin) {
        if (skin == null) return;
        
        SkinId id = skin.skinId;
        if (id != null && !favoriteIds.contains(id)) {
            favoriteIds.addFirst(id); // Add to the front
            save();
        }
    }

    public static void removeFavorite(LoadedSkin skin) {
        if (skin == null) return;
        
        SkinId id = skin.skinId;
        if (id != null && favoriteIds.remove(id)) {
            save();
        }
    }

    public static List<String> getFavoriteKeys() {
        return favoriteIds.stream()
            .map(SkinId::toString)
            .toList(); // Natively returns an unmodifiable list
    }
}