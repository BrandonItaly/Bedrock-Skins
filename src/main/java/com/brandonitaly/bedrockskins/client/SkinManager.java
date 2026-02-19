package com.brandonitaly.bedrockskins.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;

public final class SkinManager {
    private SkinManager() {}

    private static final Map<String, SkinId> playerSkins = new HashMap<>();
    private static final Map<String, SkinId> previewSkins = new HashMap<>();

    public static void load() {
        playerSkins.clear();
        previewSkins.clear();
        try {
            var state = StateManager.readState();
            var selected = state.getSelected();
            var client = Minecraft.getInstance();
            var player = client.player;
            if (selected != null && !selected.isEmpty() && player != null) {
                playerSkins.put(player.getUUID().toString(), SkinId.parse(selected));
            }
        } catch (Exception e) {
            BedrockSkinsLog.error("SkinManager: load failed", e);
        }
    }

    public static SkinId getLocalSelectedKey() {
        var localUuid = getLocalPlayerUuid();
        if (localUuid != null) {
            return playerSkins.get(localUuid);
        }
        try {
            var selected = StateManager.readState().getSelected();
            return (selected == null || selected.isEmpty()) ? null : SkinId.parse(selected);
        } catch (Exception e) {
            BedrockSkinsLog.error("SkinManager: failed to read local selected skin from state", e);
            return null;
        }
    }

    public static void setSkin(String uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        SkinId previous = playerSkins.put(uuid, id);
        if (!Objects.equals(previous, id)) {
            releaseIfUnused(previous);
        }
        var localUuid = getLocalPlayerUuid();
        if (localUuid != null && localUuid.equals(uuid)) {
            try {
                var favorites = FavoritesManager.getFavoriteKeys();
                StateManager.saveState(favorites, id == null ? null : id.toString());
            } catch (Exception e) {
                BedrockSkinsLog.error("SkinManager: failed to save selected skin", e);
            }
        }
    }

    public static void setPreviewSkin(String uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        SkinId previous = previewSkins.put(uuid, id);
        if (!Objects.equals(previous, id)) {
            releaseIfUnused(previous);
        }
    }

    public static void resetPreviewSkin(String uuid) {
        SkinId previous = previewSkins.remove(uuid);
        releaseIfUnused(previous);
    }

    public static SkinId getSkin(String uuid) {
        SkinId preview = previewSkins.get(uuid);
        return preview != null ? preview : playerSkins.get(uuid);
    }

    public static SkinId getSkin(java.util.UUID uuid) {
        return uuid == null ? null : getSkin(uuid.toString());
    }

    public static void resetSkin(String uuid) {
        SkinId previous = playerSkins.remove(uuid);
        if (previous != null) {
            var localUuid = getLocalPlayerUuid();
            if (localUuid != null && localUuid.equals(uuid)) {
                try {
                    var favorites = FavoritesManager.getFavoriteKeys();
                    StateManager.saveState(favorites, null);
                } catch (Exception e) {
                    BedrockSkinsLog.error("SkinManager: failed to clear selected skin", e);
                }
            }
            releaseIfUnused(previous);
        }
    }

    private static String getLocalPlayerUuid() {
        var client = Minecraft.getInstance();
        return client.player != null ? client.player.getUUID().toString() : null;
    }

    private static void releaseIfUnused(SkinId id) {
        if (id == null) return;
        if (playerSkins.containsValue(id)) return;
        if (previewSkins.containsValue(id)) return;
        SkinPackLoader.releaseSkinAssets(id);
    }
}
