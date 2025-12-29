package com.brandonitaly.bedrockskins;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedrockSkins implements ModInitializer {
    private final Logger logger = LoggerFactory.getLogger("bedrockskins");
    private final Map<UUID, Long> lastSkinChange = new HashMap<>();

    @Override
    public void onInitialize() {
        logger.info("Initializing Bedrock Skins Mod");

        // Register Payloads
        PayloadTypeRegistry.playS2C().register(BedrockSkinsNetworking.SkinUpdatePayload.ID, BedrockSkinsNetworking.SkinUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC);

        // Handle player joining - send them all existing skins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerSkinManager.getAllSkins().forEach((uuid, skinData) -> {
                ServerPlayNetworking.send(handler.player, new BedrockSkinsNetworking.SkinUpdatePayload(
                    uuid,
                    skinData.skinKey,
                    skinData.geometry,
                    skinData.textureData
                ));
            });
        });

        // Handle client setting their skin
        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SetSkinPayload.ID, (payload, context) -> {
            final String skinKey = payload.getSkinKey();
            final var player = context.player();
            final UUID uuid = player.getUuid();
            final var server = context.server();

            final String geometry = payload.getGeometry();
            final byte[] textureData = payload.getTextureData();

            server.execute(() -> {
                // Security: Rate Limiting / cooldown (5s)
                final long now = System.currentTimeMillis();
                final Long last = lastSkinChange.get(uuid);
                if (last != null && now - last < 5_000L) {
                    logger.warn("Player {} is changing skins too quickly.", player.getName().getString());
                    return;
                }

                // Security: Server-side Validation
                if (!"RESET".equals(skinKey)) {
                    // 1. Check Texture Size (512KB limit)
                    if (textureData.length > 512 * 1024) {
                        logger.warn("Player {} sent oversized texture ({} bytes).", player.getName().getString(), textureData.length);
                        return;
                    }

                    // 2. Check PNG Magic Bytes
                    if (textureData.length < 8 ||
                        textureData[0] != (byte)0x89 ||
                        textureData[1] != (byte)0x50 ||
                        textureData[2] != (byte)0x4E ||
                        textureData[3] != (byte)0x47
                    ) {
                        logger.warn("Player {} sent invalid texture format (not PNG).", player.getName().getString());
                        return;
                    }

                    // 3. Check Geometry Size (100KB limit)
                    if (geometry.length() > 100_000) {
                        logger.warn("Player {} sent oversized geometry ({} chars).", player.getName().getString(), geometry.length());
                        return;
                    }
                }

                logger.info("Player {} set skin to {}", player.getName().getString(), skinKey);

                if ("RESET".equals(skinKey)) {
                    ServerSkinManager.removeSkin(uuid);
                } else {
                    final PlayerSkinData data = new PlayerSkinData(skinKey, geometry, textureData);
                    ServerSkinManager.setSkin(uuid, data);
                }

                // Broadcast to all players
                final BedrockSkinsNetworking.SkinUpdatePayload updatePayload = new BedrockSkinsNetworking.SkinUpdatePayload(uuid, skinKey, geometry, textureData);

                server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, updatePayload));
            });
        });
    }
}
