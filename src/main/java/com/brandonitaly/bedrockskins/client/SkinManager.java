package com.brandonitaly.bedrockskins.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import com.brandonitaly.bedrockskins.pack.SkinId;

public final class SkinManager {
    private SkinManager() {}

    private static final Map<String, SkinId> playerSkins = new HashMap<>();

    public static void load() {
        playerSkins.clear();
        try {
            var state = StateManager.readState();
            var selected = state.getSelected();
            var client = Minecraft.getInstance();
            var player = client.player;
            if (selected != null && !selected.isEmpty() && player != null) {
                playerSkins.put(player.getUUID().toString(), SkinId.parse(selected));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SkinId getLocalSelectedKey() {
        var client = Minecraft.getInstance();
        var localUuid = client.player != null ? client.player.getUUID().toString() : null;
        if (localUuid == null) return null;
        return playerSkins.get(localUuid);
    }

    public static void setSkin(String uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        playerSkins.put(uuid, id);
        var client = Minecraft.getInstance();
        var localUuid = client.player != null ? client.player.getUUID().toString() : null;
        if (localUuid != null && localUuid.equals(uuid)) {
            try {
                var favorites = FavoritesManager.getFavoriteKeys();
                StateManager.saveState(favorites, id == null ? null : id.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setPreviewSkin(String uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        playerSkins.put(uuid, id);
    }

    public static void resetPreviewSkin(String uuid) {
        playerSkins.remove(uuid);
    }

    public static SkinId getSkin(String uuid) {
        return playerSkins.get(uuid);
    }

    public static void resetSkin(String uuid) {
        if (playerSkins.remove(uuid) != null) {
            var client = Minecraft.getInstance();
            var localUuid = client.player != null ? client.player.getUUID().toString() : null;
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
