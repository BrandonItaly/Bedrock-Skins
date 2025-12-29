package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FavoritesManager {
    private FavoritesManager() {}

    private static final List<String> favoriteKeys = new ArrayList<>();

    public static void load() {
        favoriteKeys.clear();
        try {
            BedrockSkinsState state = StateManager.readState();
            if (state.getFavorites() != null) favoriteKeys.addAll(state.getFavorites());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            String selected = SkinManager.getLocalSelectedKey();
            StateManager.saveState(new ArrayList<>(favoriteKeys), selected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isFavorite(LoadedSkin skin) {
        return favoriteKeys.contains(skin.getKey());
    }

    public static void addFavorite(LoadedSkin skin) {
        if (!isFavorite(skin)) {
            favoriteKeys.add(0, skin.getKey());
            save();
        }
    }

    public static void removeFavorite(LoadedSkin skin) {
        if (favoriteKeys.remove(skin.getKey())) save();
    }

    public static List<String> getFavoriteKeys() {
        return Collections.unmodifiableList(new ArrayList<>(favoriteKeys));
    }
}
