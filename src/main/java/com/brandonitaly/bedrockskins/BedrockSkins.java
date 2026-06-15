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
        PayloadTypeRegistry.clientboundPlay().register(BedrockSkinsNetworking.SkinUpdatePayload.ID, BedrockSkinsNetworking.SkinUpdatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedrockSkinsNetworking.SkinAnnouncePayload.ID, BedrockSkinsNetworking.SkinAnnouncePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BedrockSkinsNetworking.RequestSkinDataPayload.ID, BedrockSkinsNetworking.RequestSkinDataPayload.CODEC);

        // Handle player joining - send them all existing skin announcements
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

        // Handle client requesting a specific skin by hash
        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.RequestSkinDataPayload.ID, (payload, context) -> context.server().execute(() -> {
            ServerSkinHandler.handleRequestSkinData(
                context.player(), payload.hash()
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

        registrar.playToClient(BedrockSkinsNetworking.SkinAnnouncePayload.ID, BedrockSkinsNetworking.SkinAnnouncePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> com.brandonitaly.bedrockskins.client.BedrockSkinsClient.handleSkinAnnouncePacket(payload));
        });

        registrar.playToServer(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerSkinHandler.handleSetSkin(
                (ServerPlayer) context.player(), payload.skinId(), payload.geometry(), payload.textureData(),
                PacketDistributor::sendToAllPlayers
            ));
        });

        registrar.playToServer(BedrockSkinsNetworking.RequestSkinDataPayload.ID, BedrockSkinsNetworking.RequestSkinDataPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerSkinHandler.handleRequestSkinData(
                (ServerPlayer) context.player(), payload.hash()
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
    private static final long RATE_LIMIT_NANOS = 5_000_000_000L; // 5 seconds
    private static final int MAX_TEXTURE_SIZE = 512 * 1024;
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47};

    static boolean isValidPngHeader(byte[] data) {
        if (data == null || data.length < PNG_HEADER.length) return false;
        for (int i = 0; i < PNG_HEADER.length; i++) {
            if (data[i] != PNG_HEADER[i]) return false;
        }
        return true;
    }

    static void onPlayerJoin(Consumer<BedrockSkinsNetworking.SkinAnnouncePayload> packetSender) {
        ServerSkinManager.getAllActiveSkins().forEach((uuid, active) -> {
            packetSender.accept(new BedrockSkinsNetworking.SkinAnnouncePayload(
                uuid, active.skinId(), active.hash()
            ));
        });
    }

    static void onPlayerDisconnect(UUID uuid) {
        lastSkinChange.remove(uuid);
        ServerSkinManager.removeSkin(uuid);
    }

    static void handleSetSkin(ServerPlayer player, SkinId skinId, String geometry, byte[] textureData, Consumer<BedrockSkinsNetworking.SkinAnnouncePayload> broadcaster) {
        final UUID uuid = player.getUUID();
        final long now = System.nanoTime();
        final Long last = lastSkinChange.get(uuid);

        // Security: Rate Limiting / cooldown (5s)
        if (last != null && now - last < RATE_LIMIT_NANOS) {
            logger.warn("Player {} is changing skins too quickly.", player.getName().getString());
            return;
        }

        // Security: Server-side Validation
        if (skinId != null) {
            if (textureData.length > MAX_TEXTURE_SIZE) {
                logger.warn("Player {} sent oversized texture ({} bytes).", player.getName().getString(), textureData.length);
                return;
            }
            if (!isValidPngHeader(textureData)) {
                logger.warn("Player {} sent invalid texture format (not PNG).", player.getName().getString());
                return;
            }
        }

        logger.info("Player {} set skin to {}", player.getName().getString(), (skinId == null ? "RESET" : skinId.toString()));

        String hash = null;
        if (skinId == null) {
            ServerSkinManager.removeSkin(uuid);
        } else {
            try {
                // Validate by creating a temporary PlayerSkinData object
                new PlayerSkinData(skinId, geometry, textureData);
            } catch (IllegalArgumentException e) {
                logger.warn("Player {} sent invalid geometry payload.", player.getName().getString());
                return;
            }
            hash = ServerSkinManager.setSkin(uuid, skinId, geometry, textureData);
        }

        lastSkinChange.put(uuid, now);

        // Broadcast to all players
        broadcaster.accept(new BedrockSkinsNetworking.SkinAnnouncePayload(uuid, skinId, hash));
    }

    static void handleRequestSkinData(ServerPlayer player, String hash) {
        PlayerSkinData data = ServerSkinManager.getSkinData(hash);
        if (data != null) {
            UUID ownerUuid = ServerSkinManager.getAnyActivePlayerWithHash(hash);
            if (ownerUuid == null) {
                ownerUuid = player.getUUID();
            }
            var payload = new BedrockSkinsNetworking.SkinUpdatePayload(
                ownerUuid, data.skinId(), data.geometry(), data.textureData()
            );
            //? if fabric {
            ServerPlayNetworking.send(player, payload);
            //?} else if neoforge {
            /*PacketDistributor.sendToPlayer(player, payload);*/
            //?}
        } else {
            logger.warn("Player {} requested unknown skin hash: {}", player.getName().getString(), hash);
        }
    }
}