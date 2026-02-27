package com.brandonitaly.bedrockskins;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerSkinManager {
    private ServerSkinManager() {}

    private static final Map<UUID, PlayerSkinData> playerSkins = new ConcurrentHashMap<>();

    public static void setSkin(UUID uuid, PlayerSkinData data) {
        if (uuid != null && data != null) {
            playerSkins.put(uuid, data);
        }
    }

    public static PlayerSkinData getSkin(UUID uuid) {
        return uuid == null ? null : playerSkins.get(uuid);
    }

    public static void removeSkin(UUID uuid) {
        if (uuid != null) {
            playerSkins.remove(uuid);
        }
    }

    public static Map<UUID, PlayerSkinData> getAllSkins() {
        return Map.copyOf(playerSkins);
    }
}