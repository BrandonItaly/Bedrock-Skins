package com.brandonitaly.bedrockskins;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//?} else if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.ModContainer;*/
//?}
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.brandonitaly.bedrockskins.pack.SkinId;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

//? if fabric {
public class BedrockSkins implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerSkinHandler.logger.info("Initializing Bedrock Skins Mod");

        // Register Payloads
        PayloadTypeRegistry.playS2C().register(BedrockSkinsNetworking.SkinUpdatePayload.ID, BedrockSkinsNetworking.SkinUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC);

        // Handle player joining - send them all existing skins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerSkinHandler.onPlayerJoin(payload -> ServerPlayNetworking.send(handler.player, payload)));

        // Handle player disconnecting - clean up memory
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerSkinHandler.onPlayerDisconnect(handler.player.getUUID());
        });

        // Handle client setting their skin
        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SetSkinPayload.ID, (payload, context) -> context.server().execute(() -> {
            ServerSkinHandler.handleSetSkin(
                context.player(), payload.skinId(), payload.geometry(), payload.textureData(),
                broadcast -> context.server().getPlayerList().getPlayers().forEach(p -> ServerPlayNetworking.send(p, broadcast))
            );
        }));
    }
}
//?} else if neoforge {
/*@Mod("bedrockskins")
public class BedrockSkins {
    public BedrockSkins(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
        
        // Manual Client Registration to avoid Annotation issues
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            com.brandonitaly.bedrockskins.client.BedrockSkinsClient.init(modEventBus, modContainer);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("bedrockskins");
        
        registrar.playToClient(BedrockSkinsNetworking.SkinUpdatePayload.ID, BedrockSkinsNetworking.SkinUpdatePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> com.brandonitaly.bedrockskins.client.BedrockSkinsClient.handleSkinUpdatePacket(payload));
        });

        registrar.playToServer(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerSkinHandler.handleSetSkin(
                (ServerPlayer) context.player(), payload.skinId(), payload.geometry(), payload.textureData(),
                PacketDistributor::sendToAllPlayers
            ));
        });
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerSkinHandler.onPlayerJoin(payload -> PacketDistributor.sendToPlayer(serverPlayer, payload));
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerSkinHandler.onPlayerDisconnect(serverPlayer.getUUID());
        }
    }
}*/
//?}

// Shared logic for both Fabric and NeoForge
class ServerSkinHandler {
    static final Logger logger = LoggerFactory.getLogger("bedrockskins");
    private static final Map<UUID, Long> lastSkinChange = new ConcurrentHashMap<>();

    static void onPlayerJoin(Consumer<BedrockSkinsNetworking.SkinUpdatePayload> packetSender) {
        ServerSkinManager.getAllSkins().forEach((uuid, skinData) -> {
            packetSender.accept(new BedrockSkinsNetworking.SkinUpdatePayload(
                uuid, skinData.skinId(), skinData.geometry(), skinData.textureData()
            ));
        });
    }

    static void onPlayerDisconnect(UUID uuid) {
        lastSkinChange.remove(uuid);
        ServerSkinManager.removeSkin(uuid);
    }

    static void handleSetSkin(ServerPlayer player, SkinId skinId, String geometry, byte[] textureData, Consumer<BedrockSkinsNetworking.SkinUpdatePayload> broadcaster) {
        final UUID uuid = player.getUUID();
        final long now = System.currentTimeMillis();
        final Long last = lastSkinChange.get(uuid);

        // Security: Rate Limiting / cooldown (5s)
        if (last != null && now - last < 5_000L) {
            logger.warn("Player {} is changing skins too quickly.", player.getName().getString());
            return;
        }

        // Security: Server-side Validation
        if (skinId != null) {
            if (textureData.length > 512 * 1024) {
                logger.warn("Player {} sent oversized texture ({} bytes).", player.getName().getString(), textureData.length);
                return;
            }
            if (textureData.length < 8 || textureData[0] != (byte)0x89 || textureData[1] != (byte)0x50 || textureData[2] != (byte)0x4E || textureData[3] != (byte)0x47) {
                logger.warn("Player {} sent invalid texture format (not PNG).", player.getName().getString());
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

        // Broadcast to all players
        final String geometryOut = data != null ? data.geometry() : geometry;
        broadcaster.accept(new BedrockSkinsNetworking.SkinUpdatePayload(uuid, skinId, geometryOut, textureData));
    }
}