package io.github.brandonitaly.bedrockskins;

import io.github.brandonitaly.bedrockskins.network.BedrockSkinsNetworking;
import io.github.brandonitaly.bedrockskins.server.PlayerSkinData;
import io.github.brandonitaly.bedrockskins.server.ServerSkinManager;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaTexturePayload;
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
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;

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
        PayloadTypeRegistry.clientboundPlay().register(BedrockSkinsNetworking.CosmeticsUpdatePayload.ID, BedrockSkinsNetworking.CosmeticsUpdatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedrockSkinsNetworking.EmoteUpdatePayload.ID, BedrockSkinsNetworking.EmoteUpdatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BedrockSkinsNetworking.SetCosmeticsPayload.ID, BedrockSkinsNetworking.SetCosmeticsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BedrockSkinsNetworking.PlayEmotePayload.ID, BedrockSkinsNetworking.PlayEmotePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BedrockSkinsNetworking.RequestSkinDataPayload.ID, BedrockSkinsNetworking.RequestSkinDataPayload.CODEC);

        // Handle player joining - send them all existing skin announcements
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerSkinHandler.onPlayerJoin(payload -> ServerPlayNetworking.send(handler.player, payload));
            ServerSkinHandler.onPlayerJoinCosmetics(payload -> ServerPlayNetworking.send(handler.player, payload));
        });

        // Handle player disconnecting - clean up memory
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerSkinHandler.onPlayerDisconnect(handler.player.getUUID());
        });

        // Handle client setting their skin
        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SetSkinPayload.ID, (payload, context) -> context.server().execute(() -> {
            ServerSkinHandler.handleSetSkin(
                context.player(), payload.skinId(), payload.geometry(), payload.textureData(), payload.capeData(),
                broadcast -> context.server().getPlayerList().getPlayers().forEach(p -> ServerPlayNetworking.send(p, broadcast))
            );
        }));

        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SetCosmeticsPayload.ID, (payload, context) -> context.server().execute(() ->
            ServerSkinHandler.handleSetCosmetics(context.player(), payload.cosmetics(),
                update -> context.server().getPlayerList().getPlayers().forEach(p -> ServerPlayNetworking.send(p, update)))
        ));
        ServerPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.PlayEmotePayload.ID, (payload, context) -> context.server().execute(() -> {
            if (payload.isStop() || ServerSkinHandler.validEmote(payload)) context.server().getPlayerList().getPlayers().forEach(player -> ServerPlayNetworking.send(player,
                new BedrockSkinsNetworking.EmoteUpdatePayload(context.player().getUUID(), payload.id(), payload.name(),
                    payload.animationName(), payload.animation(), payload.duration())));
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
            io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient.init(modEventBus, modContainer);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("bedrockskins");
        
        registrar.playToClient(BedrockSkinsNetworking.SkinUpdatePayload.ID, BedrockSkinsNetworking.SkinUpdatePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient.handleSkinUpdatePacket(payload));
        });

        registrar.playToClient(BedrockSkinsNetworking.SkinAnnouncePayload.ID, BedrockSkinsNetworking.SkinAnnouncePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient.handleSkinAnnouncePacket(payload));
        });

        registrar.playToClient(BedrockSkinsNetworking.CosmeticsUpdatePayload.ID, BedrockSkinsNetworking.CosmeticsUpdatePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient.handleCosmeticsUpdatePacket(payload));
        });
        registrar.playToClient(BedrockSkinsNetworking.EmoteUpdatePayload.ID, BedrockSkinsNetworking.EmoteUpdatePayload.CODEC, (payload, context) ->
            context.enqueueWork(() -> io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient.handleEmoteUpdatePacket(payload)));

        registrar.playToServer(BedrockSkinsNetworking.SetSkinPayload.ID, BedrockSkinsNetworking.SetSkinPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerSkinHandler.handleSetSkin(
                (ServerPlayer) context.player(), payload.skinId(), payload.geometry(), payload.textureData(), payload.capeData(),
                PacketDistributor::sendToAllPlayers
            ));
        });

        registrar.playToServer(BedrockSkinsNetworking.SetCosmeticsPayload.ID, BedrockSkinsNetworking.SetCosmeticsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerSkinHandler.handleSetCosmetics(
                (ServerPlayer) context.player(), payload.cosmetics(), PacketDistributor::sendToAllPlayers
            ));
        });
        registrar.playToServer(BedrockSkinsNetworking.PlayEmotePayload.ID, BedrockSkinsNetworking.PlayEmotePayload.CODEC, (payload, context) -> {
            if (payload.isStop() || ServerSkinHandler.validEmote(payload)) context.enqueueWork(() -> PacketDistributor.sendToAllPlayers(
                new BedrockSkinsNetworking.EmoteUpdatePayload(context.player().getUUID(), payload.id(), payload.name(),
                    payload.animationName(), payload.animation(), payload.duration())));
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
            ServerSkinHandler.onPlayerJoinCosmetics(payload -> PacketDistributor.sendToPlayer(serverPlayer, payload));
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
    private static final int MAX_COSMETIC_TEXTURE_SIZE = 1024 * 1024;
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final Map<UUID, java.util.List<BedrockSkinsNetworking.CosmeticData>> playerCosmetics = new ConcurrentHashMap<>();

    static boolean isValidPngHeader(byte[] data) {
        if (data == null || data.length < PNG_HEADER.length) return false;
        for (int i = 0; i < PNG_HEADER.length; i++) {
            if (data[i] != PNG_HEADER[i]) return false;
        }
        return true;
    }

    static boolean isValidCosmeticTexture(byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_COSMETIC_TEXTURE_SIZE) return false;
        try {
            PersonaTexturePayload payload = PersonaTexturePayload.decode(data);
            if (!isValidPngHeader(payload.baseTexture())) return false;
            for (PersonaTexturePayload.AnimationRegion region : payload.animations()) {
                if (!isValidPngHeader(region.textureStrip())) return false;
            }
            return true;
        } catch (java.io.IOException exception) {
            return false;
        }
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
        playerCosmetics.remove(uuid);
    }

    static boolean validEmote(BedrockSkinsNetworking.PlayEmotePayload payload) {
        return payload != null && !payload.id().isBlank() && !payload.animation().isBlank()
            && payload.animation().length() <= 150_000 && payload.duration() > 0.0f && payload.duration() <= 30.0f;
    }

    static void onPlayerJoinCosmetics(Consumer<BedrockSkinsNetworking.CosmeticsUpdatePayload> packetSender) {
        playerCosmetics.forEach((uuid, cosmetics) ->
            packetSender.accept(new BedrockSkinsNetworking.CosmeticsUpdatePayload(uuid, cosmetics)));
    }

    static void handleSetCosmetics(ServerPlayer player, java.util.List<BedrockSkinsNetworking.CosmeticData> cosmetics,
                                   Consumer<BedrockSkinsNetworking.CosmeticsUpdatePayload> broadcaster) {
        if (cosmetics == null || cosmetics.size() > 8) return;
        for (var cosmetic : cosmetics) {
            if (cosmetic.id().isBlank() || cosmetic.type().isBlank() || cosmetic.geometry().length() > 150_000
                    || cosmetic.zones().size() > 32 || cosmetic.zones().stream().anyMatch(zone -> zone.length() > 64)
                    || !isValidCosmeticTexture(cosmetic.textureData())) {
                logger.warn("Player {} sent invalid Persona cosmetic data", player.getName().getString());
                return;
            }
        }
        java.util.List<BedrockSkinsNetworking.CosmeticData> safe = java.util.List.copyOf(cosmetics);
        if (safe.isEmpty()) playerCosmetics.remove(player.getUUID());
        else playerCosmetics.put(player.getUUID(), safe);
        broadcaster.accept(new BedrockSkinsNetworking.CosmeticsUpdatePayload(player.getUUID(), safe));
    }

    static void handleSetSkin(ServerPlayer player, SkinId skinId, String geometry, byte[] textureData, byte[] capeData, Consumer<BedrockSkinsNetworking.SkinAnnouncePayload> broadcaster) {
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
            if (capeData != null && capeData.length > 256 * 1024) {
                logger.warn("Player {} sent oversized cape texture ({} bytes).", player.getName().getString(), capeData.length);
                return;
            }
            if (capeData != null && capeData.length > 0 && !isValidPngHeader(capeData)) {
                logger.warn("Player {} sent invalid cape texture format (not PNG).", player.getName().getString());
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
                new PlayerSkinData(skinId, geometry, textureData, capeData);
            } catch (IllegalArgumentException e) {
                logger.warn("Player {} sent invalid geometry payload.", player.getName().getString());
                return;
            }
            hash = ServerSkinManager.setSkin(uuid, skinId, geometry, textureData, capeData);
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
                ownerUuid, data.skinId(), data.geometry(), data.textureData(), data.capeData()
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
