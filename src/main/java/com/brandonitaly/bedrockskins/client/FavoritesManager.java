package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FavoritesManager {
    private FavoritesManager() {}

    private static final List<SkinId> favoriteIds = new ArrayList<>();

    public static void load() {
        favoriteIds.clear();
        try {
            BedrockSkinsState state = StateManager.readState();
            if (state.getFavorites() != null) {
                for (String k : state.getFavorites()) {
                    var id = SkinId.parse(k);
                    if (id != null) favoriteIds.add(id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            SkinId selected = SkinManager.getLocalSelectedKey();
            List<String> keys = new ArrayList<>();
            for (SkinId id : favoriteIds) keys.add(id.toString());
            StateManager.saveState(keys, selected == null ? null : selected.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isFavorite(LoadedSkin skin) {
        return favoriteIds.contains(skin.getSkinId());
    }

    public static void addFavorite(LoadedSkin skin) {
        var id = skin.getSkinId();
        if (id != null && !favoriteIds.contains(id)) {
            favoriteIds.add(0, id);
            save();
        }
    }

    public static void removeFavorite(LoadedSkin skin) {
        var id = skin.getSkinId();
        if (id != null && favoriteIds.remove(id)) save();
    }

    public static List<String> getFavoriteKeys() {
        List<String> out = new ArrayList<>();
        for (var id : favoriteIds) out.add(id.toString());
        return Collections.unmodifiableList(out);
    }

    public static List<SkinId> getFavoriteIds() {
        return Collections.unmodifiableList(new ArrayList<>(favoriteIds));
    }
}
