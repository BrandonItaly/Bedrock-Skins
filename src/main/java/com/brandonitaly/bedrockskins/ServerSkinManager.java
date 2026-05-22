package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerSkinManager {
    private ServerSkinManager() {}

    public record ActiveSkin(SkinId skinId, String hash) {}

    private static final Map<String, PlayerSkinData> skinRegistry = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveSkin> playerActiveSkins = new ConcurrentHashMap<>();

    public static String setSkin(UUID uuid, SkinId skinId, String geometry, byte[] textureData) {
        if (uuid == null) return null;
        if (skinId == null) {
            removeSkin(uuid);
            return null;
        }

        String hash = com.brandonitaly.bedrockskins.BedrockSkinsNetworking.computeHash(geometry, textureData);
        
        // Deduplicate: store skin data in registry if not already present
        skinRegistry.computeIfAbsent(hash, h -> new PlayerSkinData(skinId, geometry, textureData));
        
        playerActiveSkins.put(uuid, new ActiveSkin(skinId, hash));
        cleanUnusedSkins();
        return hash;
    }

    public static PlayerSkinData getSkinData(String hash) {
        return hash == null ? null : skinRegistry.get(hash);
    }

    public static void removeSkin(UUID uuid) {
        if (uuid != null) {
            playerActiveSkins.remove(uuid);
            cleanUnusedSkins();
        }
    }

    public static Map<UUID, ActiveSkin> getAllActiveSkins() {
        return Map.copyOf(playerActiveSkins);
    }

    public static UUID getAnyActivePlayerWithHash(String hash) {
        if (hash == null) return null;
        for (Map.Entry<UUID, ActiveSkin> entry : playerActiveSkins.entrySet()) {
            if (hash.equals(entry.getValue().hash())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void cleanUnusedSkins() {
        java.util.Set<String> activeHashes = new java.util.HashSet<>();
        for (ActiveSkin active : playerActiveSkins.values()) {
            activeHashes.add(active.hash());
        }
        skinRegistry.keySet().retainAll(activeHashes);
    }
}