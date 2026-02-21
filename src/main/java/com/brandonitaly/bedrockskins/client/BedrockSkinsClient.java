package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.mojang.blaze3d.platform.InputConstants;
import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
//?} else if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;*/
//?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//? if fabric {
public class BedrockSkinsClient implements ClientModInitializer {
//?} else if neoforge {
/*public class BedrockSkinsClient {*/
//?}
    public static KeyMapping toggleCapeKey, toggleJacketKey, toggleLeftSleeveKey, toggleRightSleeveKey,
                           toggleLeftPantsKey, toggleRightPantsKey, toggleHatKey, toggleMainHandKey, openKey;
    
    //? if >1.21.8 {
    private static KeyMapping.Category keybindCategory;
    //?}

    public static void createKeybinds() {
        //? if >1.21.8 {
        //? if >=1.21.11 {
        keybindCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bedrockskins", "controls"));
        //?} else {
        /*keybindCategory = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("bedrockskins", "controls"));*/
        //?}
        //?}
        
        //? if <=1.21.8 {
        /*String cat = "bedrockskins.controls";*/
        //?} else {
        KeyMapping.Category cat = keybindCategory;
        //?}

        //? if <=1.21.8 {
        /*openKey = new KeyBinding("key.bedrockskins.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, cat);
        toggleCapeKey = new KeyBinding("key.bedrockskins.toggle_cape", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);
        toggleJacketKey = new KeyBinding("key.bedrockskins.toggle_jacket", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);
        toggleLeftSleeveKey = new KeyBinding("key.bedrockskins.toggle_left_sleeve", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);
        toggleRightSleeveKey = new KeyBinding("key.bedrockskins.toggle_right_sleeve", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);
        toggleLeftPantsKey = new KeyBinding("key.bedrockskins.toggle_left_pants", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);
        toggleRightPantsKey = new KeyBinding("key.bedrockskins.toggle_right_pants", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);
        toggleHatKey = new KeyBinding("key.bedrockskins.toggle_hat", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);
        toggleMainHandKey = new KeyBinding("key.bedrockskins.swap_main_hand", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), cat);*/
        //?} else {
        openKey = new KeyMapping("key.bedrockskins.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, cat);
        toggleCapeKey = new KeyMapping("key.bedrockskins.toggle_cape", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        toggleJacketKey = new KeyMapping("key.bedrockskins.toggle_jacket", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        toggleLeftSleeveKey = new KeyMapping("key.bedrockskins.toggle_left_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        toggleRightSleeveKey = new KeyMapping("key.bedrockskins.toggle_right_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        toggleLeftPantsKey = new KeyMapping("key.bedrockskins.toggle_left_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        toggleRightPantsKey = new KeyMapping("key.bedrockskins.toggle_right_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        toggleHatKey = new KeyMapping("key.bedrockskins.toggle_hat", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        toggleMainHandKey = new KeyMapping("key.bedrockskins.swap_main_hand", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat);
        //?}
    }

    public static Screen getAppropriateSkinScreen(Screen parent) {
        boolean legacyLoaded = false;
        //? if fabric {
        legacyLoaded = FabricLoader.getInstance().isModLoaded("legacy");
        //?} else if neoforge {
        /*legacyLoaded = net.neoforged.fml.ModList.get().isLoaded("legacy");*/
        //?}
        
        if (legacyLoaded) {
            try {
                var constructor = Class.forName("com.brandonitaly.bedrockskins.client.gui.legacy.Legacy4JChangeSkinScreen")
                                       .getConstructor(Screen.class);
                return (Screen) constructor.newInstance(parent);
            } catch (Exception ignored) {}
        }
        return new SkinSelectionScreen(parent);
    }

//? if fabric {
    @Override
    public void onInitializeClient() {
        createKeybinds();
        KeyBindingHelper.registerKeyBinding(openKey);
        KeyBindingHelper.registerKeyBinding(toggleCapeKey);
        KeyBindingHelper.registerKeyBinding(toggleJacketKey);
        KeyBindingHelper.registerKeyBinding(toggleLeftSleeveKey);
        KeyBindingHelper.registerKeyBinding(toggleRightSleeveKey);
        KeyBindingHelper.registerKeyBinding(toggleLeftPantsKey);
        KeyBindingHelper.registerKeyBinding(toggleRightPantsKey);
        KeyBindingHelper.registerKeyBinding(toggleHatKey);
        KeyBindingHelper.registerKeyBinding(toggleMainHandKey);

        ClientTickEvents.END_CLIENT_TICK.register(CommonLogic::handleTick);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            CommonLogic.reloadResources(client);
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Reloader());
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> CommonLogic.applySavedSkinOnJoin(client));
        
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinUpdatePayload.ID, (payload, context) -> {
            BedrockSkinsNetworking.SkinUpdatePayload p = payload;
            context.client().execute(() -> CommonLogic.handleSkinUpdate(p));
        });
    }

    private final class Reloader implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {
        @Override
        //? if >=1.21.11 {
        public Identifier getFabricId() { return Identifier.fromNamespaceAndPath("bedrockskins", "reloader"); }
        //?} else {
        /*public ResourceLocation getFabricId() { return ResourceLocation.fromNamespaceAndPath("bedrockskins", "reloader"); }*/
        //?}
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            CommonLogic.reloadResources(Minecraft.getInstance());
        }
    }
}
//?} else if neoforge {
/*
    public static void init(IEventBus modBus) {
        modBus.register(BedrockSkinsClient.class);
        NeoForge.EVENT_BUS.register(GameEvents.class);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        createKeybinds();
        event.register(openKey);
        event.register(toggleCapeKey);
        event.register(toggleJacketKey);
        event.register(toggleLeftSleeveKey);
        event.register(toggleRightSleeveKey);
        event.register(toggleLeftPantsKey);
        event.register(toggleRightPantsKey);
        event.register(toggleHatKey);
        event.register(toggleMainHandKey);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CommonLogic.reloadResources(Minecraft.getInstance()));
    }

    public static void handleSkinUpdatePacket(BedrockSkinsNetworking.SkinUpdatePayload payload) {
        CommonLogic.handleSkinUpdate(payload);
    }

    public static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            CommonLogic.handleTick(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            CommonLogic.applySavedSkinOnJoin(Minecraft.getInstance());
        }
    }
}
*/
//?}

// Shared Logic Container
class CommonLogic {
    static void handleTick(Minecraft client) {
        while (BedrockSkinsClient.openKey.consumeClick()) {
            client.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(client.screen));
        }
        if (client.player == null) return;
        
        while (BedrockSkinsClient.toggleCapeKey.consumeClick()) toggleModelPart(client, PlayerModelPart.CAPE);
        while (BedrockSkinsClient.toggleJacketKey.consumeClick()) toggleModelPart(client, PlayerModelPart.JACKET);
        while (BedrockSkinsClient.toggleLeftSleeveKey.consumeClick()) toggleModelPart(client, PlayerModelPart.LEFT_SLEEVE);
        while (BedrockSkinsClient.toggleRightSleeveKey.consumeClick()) toggleModelPart(client, PlayerModelPart.RIGHT_SLEEVE);
        while (BedrockSkinsClient.toggleLeftPantsKey.consumeClick()) toggleModelPart(client, PlayerModelPart.LEFT_PANTS_LEG);
        while (BedrockSkinsClient.toggleRightPantsKey.consumeClick()) toggleModelPart(client, PlayerModelPart.RIGHT_PANTS_LEG);
        while (BedrockSkinsClient.toggleHatKey.consumeClick()) toggleModelPart(client, PlayerModelPart.HAT);
        while (BedrockSkinsClient.toggleMainHandKey.consumeClick()) toggleMainHand(client);
    }

    static void toggleModelPart(Minecraft client, PlayerModelPart part) {
        client.options.setModelPart(part, !client.options.isModelPartEnabled(part));
        client.options.save();
    }
    
    static void toggleMainHand(Minecraft client) {
        var currentHand = client.options.mainHand().get();
        client.options.mainHand().set(currentHand == net.minecraft.world.entity.HumanoidArm.LEFT 
            ? net.minecraft.world.entity.HumanoidArm.RIGHT : net.minecraft.world.entity.HumanoidArm.LEFT);
        client.options.save();
    }

    static void reloadResources(Minecraft client) {
        try {
            SkinPackLoader.loadPacks();
            FavoritesManager.load();
            SkinManager.load();
            BedrockModelManager.clearAllModels();
            
            if (client.player != null) {
                SkinId id = SkinManager.getSkin(client.player.getUUID());
                if (id != null) setSafeSkin(client.player.getUUID().toString(), id, id.toString());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void applySavedSkinOnJoin(Minecraft client) {
        try {
            String savedKey = StateManager.readState().getSelected();
            if (savedKey == null || client.player == null) return;

            SkinId savedSkinId = SkinId.parse(savedKey);
            setSafeSkin(client.player.getUUID().toString(), savedSkinId, savedKey);

            LoadedSkin loadedSkin = SkinPackLoader.getLoadedSkin(savedSkinId);
            if (loadedSkin != null) {
                byte[] textureData = loadTextureData(client, loadedSkin);
                if (textureData.length > 0) {
                    ClientSkinSync.sendSetSkinPayload(savedSkinId, loadedSkin.getGeometryData().toString(), textureData);
                    System.out.println("BedrockSkinsClient: Synced skin " + savedKey);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void handleSkinUpdate(BedrockSkinsNetworking.SkinUpdatePayload p) {
        SkinId id = p.getSkinId();
        String uuidStr = p.getUuid().toString();

        if (id == null) {
            SkinManager.resetSkin(uuidStr);
        } else {
            SkinPackLoader.registerRemoteSkin(id.toString(), p.getGeometry(), p.getTextureData());
            setSafeSkin(uuidStr, id, id.toString());
        }
    }
    
    private static void setSafeSkin(String uuid, SkinId id, String fallbackName) {
        String pack = id == null || id.getPack() == null || id.getPack().isEmpty() ? "Remote" : id.getPack();
        String name = id == null || id.getName() == null || id.getName().isEmpty() ? fallbackName : id.getName();
        SkinManager.setSkin(uuid, pack, name);
    }

    static byte[] loadTextureData(Minecraft client, LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource res) {
                var opt = client.getResourceManager().getResource(res.getId());
                if (opt.isPresent()) {
                    try (var is = opt.get().open()) { return is.readAllBytes(); }
                }
            } else if (src instanceof AssetSource.File f) {
                return java.nio.file.Files.readAllBytes(new File(f.getPath()).toPath());
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    ZipEntry entry = zip.getEntry(z.getInternalPath());
                    if (entry != null) {
                        try (var is = zip.getInputStream(entry)) { return is.readAllBytes(); }
                    }
                }
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }
}