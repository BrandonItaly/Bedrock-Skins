package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.Minecraft;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SkinManager {
    private SkinManager() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, SkinId> playerSkins = new HashMap<>();
    private static final Map<UUID, SkinId> previewSkins = new HashMap<>();

    public static void load() {
        previewSkins.clear();
        try {
            var selected = StateManager.readState().getSelected();
            var localUuid = getLocalPlayerUuid();
            if (localUuid != null) {
                if (selected != null && !selected.isEmpty()) {
                    playerSkins.put(localUuid, SkinId.parse(selected));
                } else {
                    playerSkins.remove(localUuid);
                }
            }
        } catch (Exception e) {
            LOGGER.error("SkinManager: load failed", e);
        }
    }

    public static void clearOtherPlayers() {
        UUID localUuid = getLocalPlayerUuid();
        playerSkins.keySet().removeIf(uuid -> !uuid.equals(localUuid));
    }

    public static SkinId getLocalSelectedKey() {
        UUID localUuid = getLocalPlayerUuid();
        if (localUuid != null) {
            return playerSkins.get(localUuid);
        }
        try {
            var selected = StateManager.readState().getSelected();
            return (selected == null || selected.isEmpty()) ? null : SkinId.parse(selected);
        } catch (Exception e) {
            LOGGER.error("SkinManager: failed to read local selected skin from state", e);
            return null;
        }
    }

    public static void setSkin(UUID uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        SkinId previous = playerSkins.put(uuid, id);
        
        if (!Objects.equals(previous, id)) {
            releaseIfUnused(previous);
        }
        
        if (uuid.equals(getLocalPlayerUuid())) {
            try {
                StateManager.saveState(FavoritesManager.getFavoriteKeys(), id.toString());
            } catch (Exception e) {
                LOGGER.error("SkinManager: failed to save selected skin", e);
            }
        }
    }

    public static void setPreviewSkin(String uuidStr, String packName, String skinName) {
        setPreviewSkin(UUID.fromString(uuidStr), packName, skinName);
    }

    public static void setPreviewSkin(UUID uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        SkinId previous = previewSkins.put(uuid, id);
        if (!Objects.equals(previous, id)) {
            releaseIfUnused(previous);
        }
    }

    public static void resetPreviewSkin(String uuidStr) {
        resetPreviewSkin(UUID.fromString(uuidStr));
    }

    public static void resetPreviewSkin(UUID uuid) {
        SkinId previous = previewSkins.remove(uuid);
        releaseIfUnused(previous);
    }

    public static SkinId getSkin(String uuidStr) {
        return uuidStr == null ? null : getSkin(UUID.fromString(uuidStr));
    }

    public static SkinId getSkin(UUID uuid) {
        if (uuid == null) return null;
        SkinId preview = previewSkins.get(uuid);
        return preview != null ? preview : playerSkins.get(uuid);
    }

    public static void resetSkin(UUID uuid) {
        SkinId previous = playerSkins.remove(uuid);
        if (previous != null) {
            if (uuid.equals(getLocalPlayerUuid())) {
                try {
                    StateManager.saveState(FavoritesManager.getFavoriteKeys(), null);
                } catch (Exception e) {
                    LOGGER.error("SkinManager: failed to clear selected skin", e);
                }
            }
            releaseIfUnused(previous);
        }
    }

    private static UUID getLocalPlayerUuid() {
        var player = Minecraft.getInstance().player;
        return player != null ? player.getUUID() : null;
    }

    private static void releaseIfUnused(SkinId id) {
        if (id == null) return;
        if (playerSkins.containsValue(id)) return;
        if (previewSkins.containsValue(id)) return;
        SkinPackLoader.releaseSkinAssets(id);
    }
}