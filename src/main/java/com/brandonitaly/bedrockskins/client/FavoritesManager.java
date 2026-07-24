package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FavoritesManager {
    private FavoritesManager() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<SkinId> favoriteIds = new LinkedHashSet<>();

    public static void load() {
        favoriteIds.clear();
        try {
            LocalSkinConfig state = StateManager.readState();
            if (state.favorites() != null) {
                state.favorites().stream()
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
        return skin != null && skin.skinId != null && favoriteIds.contains(skin.skinId);
    }

    public static void addFavorite(LoadedSkin skin) {
        if (skin == null || skin.skinId == null) return;
        
        SkinId id = skin.skinId;
        if (!favoriteIds.contains(id)) {
            Set<SkinId> newSet = new LinkedHashSet<>();
            newSet.add(id);
            newSet.addAll(favoriteIds);
            favoriteIds.clear();
            favoriteIds.addAll(newSet);
            save();
        }
    }

    public static void removeFavorite(LoadedSkin skin) {
        if (skin == null || skin.skinId == null) return;
        
        SkinId id = skin.skinId;
        if (favoriteIds.remove(id)) {
            save();
        }
    }

    public static List<String> getFavoriteKeys() {
        return favoriteIds.stream()
            .map(SkinId::toString)
            .toList();
    }
}