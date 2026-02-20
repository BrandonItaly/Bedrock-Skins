package com.brandonitaly.bedrockskins;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
//?} else if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;*/
//?}
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.brandonitaly.bedrockskins.pack.SkinId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//? if fabric {
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
                    skinData.getSkinId(),
                    skinData.getGeometry(),
                    skinData.getTextureData()
                ));
            });
        });

        // Handle client setting their skin
        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SetSkinPayload.ID, (payload, context) -> {
            final SkinId skinId = payload.getSkinId();
            final ServerPlayer player = context.player();
            final UUID uuid = player.getUUID();
            final MinecraftServer server = context.server();

            final String geometry = payload.getGeometry();
            final byte[] textureData = payload.getTextureData();

            server.execute(() -> {
                handleSkinSetLogic(server, player, uuid, skinId, geometry, textureData);
            });
        });
    }

    private void handleSkinSetLogic(MinecraftServer server, ServerPlayer player, UUID uuid, SkinId skinId, String geometry, byte[] textureData) {
        // Security: Rate Limiting / cooldown (5s)
        final long now = System.currentTimeMillis();
        final Long last = lastSkinChange.get(uuid);
        if (last != null && now - last < 5_000L) {
            logger.warn("Player {} is changing skins too quickly.", player.getName().getString());
            return;
        }

        // Security: Server-side Validation
        if (skinId != null) {
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

        logger.info("Player {} set skin to {}", player.getName().getString(), (skinId == null ? "RESET" : skinId.toString()));

        PlayerSkinData data = null;
        if (skinId == null) {
            ServerSkinManager.removeSkin(uuid);
        } else {
            try {
                data = new PlayerSkinData(skinId, geometry, textureData);
            } catch (IllegalArgumentException e) {
                logger.warn("Player {} sent invalid geometry payload.", player.getName().getString());
                return;
            }
            ServerSkinManager.setSkin(uuid, data);
        }

        // Broadcast to all players
        final String geometryOut = data != null ? data.getGeometry() : geometry;
        final BedrockSkinsNetworking.SkinUpdatePayload updatePayload = new BedrockSkinsNetworking.SkinUpdatePayload(uuid, skinId, geometryOut, textureData);
        server.getPlayerList().getPlayers().forEach(p -> ServerPlayNetworking.send(p, updatePayload));
        
        lastSkinChange.put(uuid, now);
    }
}
//?} else if neoforge {
/*@Mod("bedrockskins")
public class BedrockSkins {
    public static final Logger logger = LoggerFactory.getLogger("bedrockskins");
    private final Map<UUID, Long> lastSkinChange = new HashMap<>();

    public BedrockSkins(IEventBus modEventBus) {
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
        
        // Manual Client Registration to avoid Annotation issues
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            com.brandonitaly.bedrockskins.client.BedrockSkinsClient.init(modEventBus);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("bedrockskins");
        
        registrar.playToClient(
            BedrockSkinsNetworking.SkinUpdatePayload.ID,
            BedrockSkinsNetworking.SkinUpdatePayload.CODEC,
            (payload, context) -> {
                context.enqueueWork(() -> {
                     com.brandonitaly.bedrockskins.client.BedrockSkinsClient.handleSkinUpdatePacket(payload);
                });
            }
        );

        registrar.playToServer(
            BedrockSkinsNetworking.SetSkinPayload.ID,
            BedrockSkinsNetworking.SetSkinPayload.CODEC,
            (payload, context) -> {
                context.enqueueWork(() -> handleSetSkinPacket(payload, context));
            }
        );
    }

    private void handleSetSkinPacket(BedrockSkinsNetworking.SetSkinPayload payload, IPayloadContext context) {
        final net.minecraft.world.entity.player.Player player = context.player();
        final UUID uuid = player.getUUID();
        final SkinId skinId = payload.getSkinId();
        final String geometry = payload.getGeometry();
        final byte[] textureData = payload.getTextureData();
        
        final long now = System.currentTimeMillis();
        final Long last = lastSkinChange.get(uuid);
        if (last != null && now - last < 5_000L) {
            logger.warn("Player {} is changing skins too quickly.", player.getName().getString());
            return;
        }

        if (skinId != null) {
            if (textureData.length > 512 * 1024) {
                logger.warn("Player {} sent oversized texture ({} bytes).", player.getName().getString(), textureData.length);
                return;
            }
            if (textureData.length < 8 || textureData[0] != (byte)0x89 || textureData[1] != (byte)0x50 || textureData[2] != (byte)0x4E || textureData[3] != (byte)0x47) {
                logger.warn("Player {} sent invalid texture format (not PNG).", player.getName().getString());
                return;
            }
            if (geometry.length() > 100_000) {
                logger.warn("Player {} sent oversized geometry ({} chars).", player.getName().getString(), geometry.length());
                return;
            }
        }

        logger.info("Player {} set skin to {}", player.getName().getString(), (skinId == null ? "RESET" : skinId.toString()));

        PlayerSkinData data = null;
        if (skinId == null) {
            ServerSkinManager.removeSkin(uuid);
        } else {
            try {
                data = new PlayerSkinData(skinId, geometry, textureData);
            } catch (IllegalArgumentException e) {
                logger.warn("Player {} sent invalid geometry payload.", player.getName().getString());
                return;
            }
            ServerSkinManager.setSkin(uuid, data);
        }
        
        lastSkinChange.put(uuid, now);

        final String geometryOut = data != null ? data.getGeometry() : geometry;
        final BedrockSkinsNetworking.SkinUpdatePayload updatePayload = new BedrockSkinsNetworking.SkinUpdatePayload(uuid, skinId, geometryOut, textureData);
        PacketDistributor.sendToAllPlayers(updatePayload);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerSkinManager.getAllSkins().forEach((uuid, skinData) -> {
                PacketDistributor.sendToPlayer(serverPlayer, new BedrockSkinsNetworking.SkinUpdatePayload(
                    uuid,
                    skinData.getSkinId(),
                    skinData.getGeometry(),
                    skinData.getTextureData()
                ));
            });
        }
    }
}*/
//?}