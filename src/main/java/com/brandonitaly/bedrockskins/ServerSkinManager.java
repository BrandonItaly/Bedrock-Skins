package com.brandonitaly.bedrockskins;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerSkinManager {
    private ServerSkinManager() {}

    private static final Map<UUID, PlayerSkinData> playerSkins = new HashMap<>();

    public static void setSkin(UUID uuid, PlayerSkinData data) {
        playerSkins.put(uuid, data);
    }

    public static PlayerSkinData getSkin(UUID uuid) {
        return playerSkins.get(uuid);
    }

    public static void removeSkin(UUID uuid) {
        playerSkins.remove(uuid);
    }

    public static Map<UUID, PlayerSkinData> getAllSkins() {
        return Collections.unmodifiableMap(new HashMap<>(playerSkins));
    }
}
