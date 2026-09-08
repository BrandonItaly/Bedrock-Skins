package io.github.brandonitaly.bedrockskins.client;

import io.github.brandonitaly.bedrockskins.network.BedrockSkinsNetworking;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.ClientSkinSync;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.FavoritesManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.client.persistence.StateManager;
import io.github.brandonitaly.bedrockskins.client.persistence.BedrockSkinsConfig;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.gui.screen.SkinSelectionScreen;
import io.github.brandonitaly.bedrockskins.gui.screen.EmoteWheelScreen;
//? if legacy4j {
import io.github.brandonitaly.bedrockskins.gui.legacy.Legacy4JMenuIntegration;
//?}
import io.github.brandonitaly.bedrockskins.util.PlatformUtil;
import io.github.brandonitaly.bedrockskins.pack.model.AssetSource;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaResourceLoader;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
//?} else if neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;*/
//?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.PlayerModelPart;
//? if <=26.2 {
import org.lwjgl.glfw.GLFW;
//?}
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BedrockSkinsClient /*? if fabric {*/ implements ClientModInitializer /*?}*/ {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int pendingJoinSyncTicks = -1;
    private static boolean emoteKeyHandledByScreen;
    private static CameraType cameraBeforeEmote;
    private static boolean emoteCameraActive;

    public static KeyMapping toggleCapeKey, toggleJacketKey, toggleLeftSleeveKey, toggleRightSleeveKey, toggleLeftPantsKey, toggleRightPantsKey, toggleHatKey, toggleMainHandKey, openKey, playEmoteKey;
    private static KeyMapping[] ALL_KEYS;

    public static boolean blockUnfairSkins = false; // for dev testing

    public static void createKeybinds() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bedrockskins", "controls"));

        ALL_KEYS = new KeyMapping[]{
            openKey = new KeyMapping("key.bedrockskins.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, cat),
            playEmoteKey = new KeyMapping("key.bedrockskins.play_emote", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, cat),
            toggleCapeKey = new KeyMapping("key.bedrockskins.toggle_cape", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat),
            toggleJacketKey = new KeyMapping("key.bedrockskins.toggle_jacket", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat),
            toggleLeftSleeveKey = new KeyMapping("key.bedrockskins.toggle_left_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat),
            toggleRightSleeveKey = new KeyMapping("key.bedrockskins.toggle_right_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat),
            toggleLeftPantsKey = new KeyMapping("key.bedrockskins.toggle_left_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat),
            toggleRightPantsKey = new KeyMapping("key.bedrockskins.toggle_right_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat),
            toggleHatKey = new KeyMapping("key.bedrockskins.toggle_hat", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat),
            toggleMainHandKey = new KeyMapping("key.bedrockskins.swap_main_hand", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat)
        };
    }

    public static Screen getAppropriateSkinScreen(Screen parent) {
        //? if legacy4j {
        if (PlatformUtil.isModLoaded("legacy")) {
            try {
                return Legacy4JMenuIntegration.createScreen(parent);
            } catch (Throwable t) {
                LOGGER.warn("Failed to open Legacy4J skin screen; falling back to default screen", t);
            }
        }
        //?}
        return new SkinSelectionScreen(parent);
    }

//? if fabric {
    @Override
    public void onInitializeClient() {
        createKeybinds();
        for (KeyMapping key : ALL_KEYS) KeyMappingHelper.registerKeyMapping(key);

        //? if legacy4j {
        if (PlatformUtil.isModLoaded("legacy")) {
            try {
                Legacy4JMenuIntegration.init();
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to initialize Legacy4J hosted menu integration", throwable);
            }
        }
        //?}

        ClientTickEvents.END_CLIENT_TICK.register(BedrockSkinsClient::handleTick);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            reloadResources(client);
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Reloader());
        });
        
        ClientPlayConnectionEvents.JOIN.register((h, s, client) -> queueSavedAppearanceSync());
        ClientPlayConnectionEvents.DISCONNECT.register((h, client) -> client.execute(() -> {
            pendingJoinSyncTicks = -1;
            clearAllRemoteSkins();
        }));
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinUpdatePayload.ID, (payload, context) -> context.client().execute(() -> handleSkinUpdate(payload)));
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinAnnouncePayload.ID, (payload, context) -> context.client().execute(() -> handleSkinAnnounce(payload)));
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.CosmeticsUpdatePayload.ID, (payload, context) -> context.client().execute(() -> handleCosmeticsUpdate(payload)));
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.EmoteUpdatePayload.ID, (payload, context) -> context.client().execute(() -> handleEmoteUpdate(payload)));
    }

    private static final class Reloader implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {
        @Override public Identifier getFabricId() { return Identifier.fromNamespaceAndPath("bedrockskins", "reloader"); }
        @Override public void onResourceManagerReload(ResourceManager manager) {
            BedrockSkinsClient.reloadResources(Minecraft.getInstance());
        }
    }
//?} else if neoforge {
/*
    public static void init(IEventBus modBus, ModContainer modContainer) {
        modBus.register(BedrockSkinsClient.class);
        NeoForge.EVENT_BUS.register(GameEvents.class);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new io.github.brandonitaly.bedrockskins.gui.screen.BedrockSkinsConfigScreen(parent));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        createKeybinds();
        for (KeyMapping key : ALL_KEYS) event.register(key);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> reloadResources(Minecraft.getInstance()));
    }

    public static void handleSkinUpdatePacket(BedrockSkinsNetworking.SkinUpdatePayload payload) {
        handleSkinUpdate(payload);
    }

    public static void handleSkinAnnouncePacket(BedrockSkinsNetworking.SkinAnnouncePayload payload) {
        handleSkinAnnounce(payload);
    }

    public static void handleCosmeticsUpdatePacket(BedrockSkinsNetworking.CosmeticsUpdatePayload payload) {
        handleCosmeticsUpdate(payload);
    }

    public static void handleEmoteUpdatePacket(BedrockSkinsNetworking.EmoteUpdatePayload payload) {
        handleEmoteUpdate(payload);
    }

    public static class GameEvents {
        @SubscribeEvent public static void onClientTick(ClientTickEvent.Post event) { handleTick(Minecraft.getInstance()); }
        @SubscribeEvent public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) { queueSavedAppearanceSync(); }
        @SubscribeEvent public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
            pendingJoinSyncTicks = -1;
            Minecraft.getInstance().execute(BedrockSkinsClient::clearAllRemoteSkins);
        }
    }
*/
//?}

    // --- Shared Logic ---

    static void handleTick(Minecraft client) {
        //? if legacy4j {
        if (PlatformUtil.isModLoaded("legacy")) Legacy4JMenuIntegration.init();
        //?}
        while (openKey.consumeClick()) client.gui.setScreen(getAppropriateSkinScreen(client.gui.screen()));
        EmoteManager.tick();
        if (client.player == null) {
            restoreEmoteCamera(client);
            return;
        }

        if (pendingJoinSyncTicks >= 0 && pendingJoinSyncTicks-- == 0) {
            applySavedSkinOnJoin(client);
        }

        while (playEmoteKey.consumeClick()) {
            if (client.gui.screen() instanceof EmoteWheelScreen wheel) {
                wheel.onClose();
            } else {
                client.gui.setScreen(new EmoteWheelScreen(client.gui.screen()));
            }
        }
        UUID localPlayerId = client.player.getUUID();
        if (EmoteManager.isPlaying(localPlayerId) && isMovementInputDown(client)) {
            EmoteManager.stopLocal();
        }
        updateEmoteCamera(client, EmoteManager.isPlaying(localPlayerId));
        
        toggleModelPart(client, toggleCapeKey, PlayerModelPart.CAPE);
        toggleModelPart(client, toggleJacketKey, PlayerModelPart.JACKET);
        toggleModelPart(client, toggleLeftSleeveKey, PlayerModelPart.LEFT_SLEEVE);
        toggleModelPart(client, toggleRightSleeveKey, PlayerModelPart.RIGHT_SLEEVE);
        toggleModelPart(client, toggleLeftPantsKey, PlayerModelPart.LEFT_PANTS_LEG);
        toggleModelPart(client, toggleRightPantsKey, PlayerModelPart.RIGHT_PANTS_LEG);
        toggleModelPart(client, toggleHatKey, PlayerModelPart.HAT);

        while (toggleMainHandKey.consumeClick()) {
            var currentHand = client.options.mainHand().get();
            client.options.mainHand().set(currentHand == net.minecraft.world.entity.HumanoidArm.LEFT ? net.minecraft.world.entity.HumanoidArm.RIGHT : net.minecraft.world.entity.HumanoidArm.LEFT);
            client.options.save();
        }
    }

    private static void toggleModelPart(Minecraft client, KeyMapping key, PlayerModelPart part) {
        while (key.consumeClick()) {
            client.options.setModelPart(part, !client.options.isModelPartEnabled(part));
            client.options.save();
        }
    }

    public static void reloadResources(Minecraft client) {
        try {
            SkinPackLoader.loadPacks();
            PersonaResourceLoader.beginReload(client.getResourceManager());
            PersonaManager.reload(SkinPackLoader.vanillaGeometryJson);
            EmoteManager.reload();
            FavoritesManager.load();
            SkinManager.load();
            BedrockModelManager.clearAllModels();
            
            if (client.player != null) {
                UUID playerUuid = client.player.getUUID();
                SkinId id = SkinManager.getSkin(playerUuid);
                if (id != null) SkinManager.setSkin(playerUuid, id);
            }

            if (client.gui.screen() instanceof SkinSelectionScreen selectionScreen) {
                selectionScreen.onResourcesReloaded();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to reload Bedrock Skins resources", e);
        }
    }

    static void applySavedSkinOnJoin(Minecraft client) {
        try {
            PersonaManager.applySavedSelection();
            if (client.player != null) ClientSkinSync.syncCurrentCosmetics();
            String savedKey = StateManager.readState().selected();
            if (savedKey == null || client.player == null) return;

            SkinId savedSkinId = SkinId.parse(savedKey);
            UUID playerUuid = client.player.getUUID();
            SkinManager.setSkin(playerUuid, savedSkinId);

            LoadedSkin loadedSkin = SkinPackLoader.getLoadedSkin(savedSkinId);
            if (loadedSkin != null) {
                ClientSkinSync.syncCurrentSkin(client);
                LOGGER.debug("Synced saved skin {}", savedKey);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply saved skin on join", e);
        }
    }

    private static final java.util.Set<String> requestedHashes = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Map<UUID, SkinIdAnnounce> playerAnnouncedSkins = new java.util.concurrent.ConcurrentHashMap<>();

    private record SkinIdAnnounce(SkinId skinId, String hash) {}

    static void handleSkinAnnounce(BedrockSkinsNetworking.SkinAnnouncePayload p) {
        UUID playerUuid = p.uuid();
        SkinId skinId = p.skinId();
        String hash = p.hash();

        if (skinId == null || hash == null || hash.isEmpty()) {
            playerAnnouncedSkins.remove(playerUuid);
            SkinManager.resetSkin(playerUuid);
            return;
        }

        playerAnnouncedSkins.put(playerUuid, new SkinIdAnnounce(skinId, hash));

        // Check whether this skin is already registered with a matching hash.
        LoadedSkin existing = SkinPackLoader.getLoadedSkin(skinId);
        if (existing != null && hash.equals(existing.hash)) {
            // Apply immediately
            SkinManager.setSkin(playerUuid, skinId);
        } else {
            // Lazily request skin data if not already requested
            if (requestedHashes.add(hash)) {
                ClientSkinSync.sendRequestSkinDataPayload(hash);
            }
        }
    }

    static void handleSkinUpdate(BedrockSkinsNetworking.SkinUpdatePayload p) {
        SkinId id = p.skinId();
        UUID playerUuid = p.uuid();

        if (id == null) {
            SkinManager.resetSkin(playerUuid);
        } else {
            String hash = BedrockSkinsNetworking.computeHash(p.geometry(), p.textureData());
            SkinPackLoader.registerRemoteSkin(id.toString(), p.geometry(), p.textureData(), p.capeData(), hash);
            
            // Set for the main player in the payload
            SkinManager.setSkin(playerUuid, id);

            // Apply to any other players who have been announced with this hash
            playerAnnouncedSkins.forEach((uuid, announce) -> {
                if (hash.equals(announce.hash)) {
                    if (announce.skinId != null) {
                        SkinPackLoader.registerRemoteSkin(announce.skinId.toString(), p.geometry(), p.textureData(), p.capeData(), hash);
                        SkinManager.setSkin(uuid, announce.skinId);
                    }
                }
            });

            requestedHashes.remove(hash);
        }
    }

    private static void queueSavedAppearanceSync() {
        // The loader's login event may precede creation of Minecraft.player. Waiting
        // two client ticks also ensures the play connection can accept custom payloads.
        pendingJoinSyncTicks = 2;
    }

    public static void markEmoteKeyHandledByScreen() {
        emoteKeyHandledByScreen = true;
    }

    private static boolean isMovementInputDown(Minecraft client) {
        return client.options.keyUp.isDown()
            || client.options.keyDown.isDown()
            || client.options.keyLeft.isDown()
            || client.options.keyRight.isDown()
            || client.options.keyJump.isDown()
            || client.options.keyShift.isDown()
            || client.options.keySprint.isDown();
    }

    private static void updateEmoteCamera(Minecraft client, boolean emoting) {
        if (emoting && BedrockSkinsConfig.isThirdPersonEmotesEnabled()) {
            if (!emoteCameraActive) {
                cameraBeforeEmote = client.options.getCameraType();
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                emoteCameraActive = true;
            }
        } else {
            restoreEmoteCamera(client);
        }
    }

    private static void restoreEmoteCamera(Minecraft client) {
        if (!emoteCameraActive) return;
        if (cameraBeforeEmote != null) client.options.setCameraType(cameraBeforeEmote);
        cameraBeforeEmote = null;
        emoteCameraActive = false;
    }

    static void handleCosmeticsUpdate(BedrockSkinsNetworking.CosmeticsUpdatePayload payload) {
        PersonaManager.applyRemote(payload.uuid(), payload.cosmetics());
    }

    static void handleEmoteUpdate(BedrockSkinsNetworking.EmoteUpdatePayload payload) {
        EmoteManager.playRemote(payload.uuid(), payload.id(), payload.name(), payload.animationName(),
            payload.animation(), payload.duration());
    }

    static void clearAllRemoteSkins() {
        SkinManager.clearOtherPlayers();
        PersonaManager.clearOtherPlayers();
        EmoteManager.clear();
        BedrockModelManager.clearAllModels();
        
        int removedCount = SkinPackLoader.removeLoadedSkins(skin -> skin.texture instanceof AssetSource.Remote);
        
        requestedHashes.clear();
        playerAnnouncedSkins.clear();
        
        if (removedCount > 0) LOGGER.debug("Cleared {} remote skins from memory", removedCount);
    }
}
