package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen;
import com.brandonitaly.bedrockskins.util.ExternalAssetUtil;
import com.brandonitaly.bedrockskins.util.PlatformUtil;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.platform.InputConstants;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BedrockSkinsClient /*? if fabric {*/ implements ClientModInitializer /*?}*/ {
    public static KeyMapping toggleCapeKey, toggleJacketKey, toggleLeftSleeveKey, toggleRightSleeveKey, toggleLeftPantsKey, toggleRightPantsKey, toggleHatKey, toggleMainHandKey, openKey;
    private static KeyMapping[] ALL_KEYS;

    public static boolean blockUnfairSkins = false; // for dev testing

    public static void createKeybinds() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bedrockskins", "controls"));

        ALL_KEYS = new KeyMapping[]{
            openKey = new KeyMapping("key.bedrockskins.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, cat),
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
        if (PlatformUtil.isModLoaded("legacy")) {
            try {
                return (Screen) Class.forName("com.brandonitaly.bedrockskins.client.gui.legacy.Legacy4JChangeSkinScreen").getConstructor(Screen.class).newInstance(parent);
            } catch (Throwable t) {
                System.err.println("BedrockSkinsClient: Failed to open legacy screen, falling back to default screen.");
            }
        }
        return new SkinSelectionScreen(parent);
    }

//? if fabric {
    @Override
    public void onInitializeClient() {
        createKeybinds();
        for (KeyMapping key : ALL_KEYS) KeyBindingHelper.registerKeyBinding(key);

        ClientTickEvents.END_CLIENT_TICK.register(BedrockSkinsClient::handleTick);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ContentManager.reloadCategories(client.getResourceManager());
            reloadResources(client);
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Reloader());
        });
        
        ClientPlayConnectionEvents.JOIN.register((h, s, client) -> applySavedSkinOnJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((h, client) -> client.execute(BedrockSkinsClient::clearAllRemoteSkins));
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinUpdatePayload.ID, (payload, context) -> context.client().execute(() -> handleSkinUpdate(payload)));
    }

    private static final class Reloader implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {
        @Override public Identifier getFabricId() { return Identifier.fromNamespaceAndPath("bedrockskins", "reloader"); }
        @Override public void onResourceManagerReload(ResourceManager manager) {
            ContentManager.reloadCategories(manager);
            BedrockSkinsClient.reloadResources(Minecraft.getInstance());
        }
    }
//?} else if neoforge {
/*
    public static void init(IEventBus modBus, ModContainer modContainer) {
        modBus.register(BedrockSkinsClient.class);
        NeoForge.EVENT_BUS.register(GameEvents.class);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new com.brandonitaly.bedrockskins.client.gui.BedrockSkinsConfigScreen(parent));
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

    public static class GameEvents {
        @SubscribeEvent public static void onClientTick(ClientTickEvent.Post event) { handleTick(Minecraft.getInstance()); }
        @SubscribeEvent public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) { applySavedSkinOnJoin(Minecraft.getInstance()); }
        @SubscribeEvent public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) { Minecraft.getInstance().execute(BedrockSkinsClient::clearAllRemoteSkins); }
    }
*/
//?}

    // --- Shared Logic ---

    static void handleTick(Minecraft client) {
        while (openKey.consumeClick()) client.setScreen(getAppropriateSkinScreen(client.screen));
        if (client.player == null) return;
        
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

    static void reloadResources(Minecraft client) {
        try {
            SkinPackLoader.loadPacks();
            FavoritesManager.load();
            SkinManager.load();
            BedrockModelManager.clearAllModels();
            
            if (client.player != null) {
                UUID playerUuid = client.player.getUUID();
                SkinId id = SkinManager.getSkin(playerUuid);
                if (id != null) SkinManager.setSkin(playerUuid, id);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void applySavedSkinOnJoin(Minecraft client) {
        try {
            String savedKey = StateManager.readState().selected();
            if (savedKey == null || client.player == null) return;

            SkinId savedSkinId = SkinId.parse(savedKey);
            UUID playerUuid = client.player.getUUID();
            SkinManager.setSkin(playerUuid, savedSkinId);

            LoadedSkin loadedSkin = SkinPackLoader.getLoadedSkin(savedSkinId);
            if (loadedSkin != null) {
                byte[] textureData = ExternalAssetUtil.loadTextureData(loadedSkin, client);
                if (textureData.length > 0) {
                    ClientSkinSync.sendSetSkinPayload(savedSkinId, loadedSkin.geometryData.toString(), textureData);
                    System.out.println("BedrockSkinsClient: Synced skin " + savedKey);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void handleSkinUpdate(BedrockSkinsNetworking.SkinUpdatePayload p) {
        SkinId id = p.skinId();
        UUID playerUuid = p.uuid();

        if (id == null) {
            SkinManager.resetSkin(playerUuid);
        } else {
            SkinPackLoader.registerRemoteSkin(id.toString(), p.geometry(), p.textureData());
            SkinManager.setSkin(playerUuid, id);
        }
    }

    static void clearAllRemoteSkins() {
        SkinManager.clearOtherPlayers();
        BedrockModelManager.clearAllModels();
        
        List<SkinId> toRemove = new ArrayList<>();
        synchronized (SkinPackLoader.loadedSkins) {
            SkinPackLoader.loadedSkins.forEach((id, skin) -> {
                if (skin.texture instanceof AssetSource.Remote) toRemove.add(id);
            });
            
            for (SkinId id : toRemove) {
                SkinPackLoader.releaseSkinAssets(id); 
                SkinPackLoader.loadedSkins.remove(id);
            }
        }
        
        if (!toRemove.isEmpty()) System.out.println("BedrockSkinsClient: Cleared " + toRemove.size() + " remote skins from memory.");
    }
}