package com.brandonitaly.bedrockskins.client.gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.TrackedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PreviewPlayer extends OtherClientPlayerEntity {
    public PreviewPlayer(ClientWorld world, GameProfile profile) {
        super(world, profile);

        // Attempt to set model customization bits in a version-safe way
        try {
            java.lang.reflect.Field f = PlayerEntity.class.getField("PLAYER_MODE_CUSTOMIZATION_ID");
            Object obj = f.get(null);
            if (obj instanceof TrackedData) {
                @SuppressWarnings("unchecked")
                TrackedData<Byte> td = (TrackedData<Byte>) obj;
                this.getDataTracker().set(td, (byte)127);
            }
        } catch (Exception e) {
            try {
                java.lang.reflect.Field f2 = PlayerEntity.class.getField("PLAYER_MODEL_PARTS");
                Object obj2 = f2.get(null);
                if (obj2 instanceof TrackedData) {
                    @SuppressWarnings("unchecked")
                    TrackedData<Byte> td2 = (TrackedData<Byte>) obj2;
                    this.getDataTracker().set(td2, (byte)127);
                }
            } catch (Exception ignored) {}
        }
    }

    // Pool
    public static final class PreviewPlayerPool {
        private static final Map<UUID, PreviewPlayer> pool = new ConcurrentHashMap<>();

        public static PreviewPlayer get(ClientWorld world, GameProfile profile) {
                UUID id;
            try {
                java.lang.reflect.Method m = GameProfile.class.getMethod("getId");
                id = (UUID) m.invoke(profile);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field f = GameProfile.class.getField("id");
                    id = (UUID) f.get(profile);
                } catch (Exception ex) {
                    id = UUID.randomUUID();
                }
            }
            return pool.computeIfAbsent(id, k -> new PreviewPlayer(world, profile));
        }

        public static void remove(UUID id) { pool.remove(id); }

        public static void clear() { pool.clear(); }
    }
}
