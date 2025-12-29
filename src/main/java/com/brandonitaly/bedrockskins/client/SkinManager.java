package com.brandonitaly.bedrockskins.client;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

public final class SkinManager {
    private SkinManager() {}

    private static final Map<String, String> playerSkins = new HashMap<>();

    public static void load() {
        playerSkins.clear();
        try {
            var state = StateManager.readState();
            var selected = state.getSelected();
            var client = MinecraftClient.getInstance();
            var player = client.player;
            if (selected != null && !selected.isEmpty() && player != null) {
                playerSkins.put(player.getUuid().toString(), selected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLocalSelectedKey() {
        var client = MinecraftClient.getInstance();
        var localUuid = client.player != null ? client.player.getUuid().toString() : null;
        if (localUuid == null) return null;
        return playerSkins.get(localUuid);
    }

    public static void setSkin(String uuid, String packName, String skinName) {
        String key = packName + ":" + skinName;
        playerSkins.put(uuid, key);
        var client = MinecraftClient.getInstance();
        var localUuid = client.player != null ? client.player.getUuid().toString() : null;
        if (localUuid != null && localUuid.equals(uuid)) {
            try {
                var favorites = FavoritesManager.getFavoriteKeys();
                StateManager.saveState(favorites, key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setPreviewSkin(String uuid, String packName, String skinName) {
        String key = packName + ":" + skinName;
        playerSkins.put(uuid, key);
    }

    public static void resetPreviewSkin(String uuid) {
        playerSkins.remove(uuid);
    }

    public static String getSkin(String uuid) {
        return playerSkins.get(uuid);
    }

    public static void resetSkin(String uuid) {
        if (playerSkins.remove(uuid) != null) {
            var client = MinecraftClient.getInstance();
            var localUuid = client.player != null ? client.player.getUuid().toString() : null;
            if (localUuid != null && localUuid.equals(uuid)) {
                try {
                    var favorites = FavoritesManager.getFavoriteKeys();
                    StateManager.saveState(favorites, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
