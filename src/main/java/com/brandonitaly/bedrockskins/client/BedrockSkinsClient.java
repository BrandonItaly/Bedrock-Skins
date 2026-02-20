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
    
    private static KeyMapping toggleCapeKey;
    private static KeyMapping toggleJacketKey;
    private static KeyMapping toggleLeftSleeveKey;
    private static KeyMapping toggleRightSleeveKey;
    private static KeyMapping toggleLeftPantsKey;
    private static KeyMapping toggleRightPantsKey;
    private static KeyMapping toggleHatKey;
    private static KeyMapping toggleMainHandKey;
    private static KeyMapping openKey;
    
    //? if >1.21.8 {
    private static KeyMapping.Category keybindCategory;
    //?}
    
    @Override
    public void onInitializeClient() {
        //? if >1.21.8 {
        //? if >=1.21.11 {
        keybindCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bedrockskins", "controls"));
        //?} else {
        /*keybindCategory = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("bedrockskins", "controls"));*/
        //?}
        //?}
        registerKeyBinding();
        registerCustomizationKeybinds();
        registerLifecycleEvents();
        registerNetworking();
    }

    public static net.minecraft.client.gui.screens.Screen getAppropriateSkinScreen(net.minecraft.client.gui.screens.Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("legacy")) {
            try {
                Class<?> screenClass = Class.forName("com.brandonitaly.bedrockskins.client.gui.legacy.Legacy4JChangeSkinScreen");
                var constructor = screenClass.getConstructor(net.minecraft.client.gui.screens.Screen.class);
                return (net.minecraft.client.gui.screens.Screen) constructor.newInstance(parent);
            } catch (Exception e) {
                return new SkinSelectionScreen(parent);
            }
        } else {
            return new SkinSelectionScreen(parent);
        }
    }

    private void registerKeyBinding() {
        try {
            String keyId = "key.bedrockskins.open";
            //? if <=1.21.8 {
            /*KeyBinding key = new KeyBinding(keyId, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "bedrockskins.controls");*/
            //?} else {
            KeyMapping key = new KeyMapping(keyId, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, keybindCategory);
            //?}
            openKey = KeyBindingHelper.registerKeyBinding(key);

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                while (openKey.consumeClick()) {
                    client.setScreen(getAppropriateSkinScreen(client.screen));
                }
            });
        } catch (Exception e) {
            System.out.println("BedrockSkinsClient: Keybinding error: " + e);
        }
    }
    
    private void registerCustomizationKeybinds() {
        try {
            //? if <=1.21.8 {
            /*String categoryName = "bedrockskins.controls";*/
            //?}
            
            //? if <=1.21.8 {
            /*toggleCapeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.toggle_cape", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));
            toggleJacketKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.toggle_jacket", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));
            toggleLeftSleeveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.toggle_left_sleeve", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));
            toggleRightSleeveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.toggle_right_sleeve", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));
            toggleLeftPantsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.toggle_left_pants", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));
            toggleRightPantsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.toggle_right_pants", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));
            toggleHatKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.toggle_hat", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));
            toggleMainHandKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bedrockskins.swap_main_hand", InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), categoryName));*/
            //?} else {
            toggleCapeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.toggle_cape", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            toggleJacketKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.toggle_jacket", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            toggleLeftSleeveKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.toggle_left_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            toggleRightSleeveKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.toggle_right_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            toggleLeftPantsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.toggle_left_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            toggleRightPantsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.toggle_right_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            toggleHatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.toggle_hat", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            toggleMainHandKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bedrockskins.swap_main_hand", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), keybindCategory));
            //?}
            
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player == null) return;
                while (toggleCapeKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.CAPE);
                while (toggleJacketKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.JACKET);
                while (toggleLeftSleeveKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.LEFT_SLEEVE);
                while (toggleRightSleeveKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.RIGHT_SLEEVE);
                while (toggleLeftPantsKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.LEFT_PANTS_LEG);
                while (toggleRightPantsKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.RIGHT_PANTS_LEG);
                while (toggleHatKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.HAT);
                while (toggleMainHandKey.consumeClick()) CommonLogic.toggleMainHand(client);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerLifecycleEvents() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            CommonLogic.reloadResources(client);
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Reloader());
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> CommonLogic.applySavedSkinOnJoin(client));
    }

    private void registerNetworking() {
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
/*public class BedrockSkinsClient {
    private static KeyMapping toggleCapeKey;
    private static KeyMapping toggleJacketKey;
    private static KeyMapping toggleLeftSleeveKey;
    private static KeyMapping toggleRightSleeveKey;
    private static KeyMapping toggleLeftPantsKey;
    private static KeyMapping toggleRightPantsKey;
    private static KeyMapping toggleHatKey;
    private static KeyMapping toggleMainHandKey;
    private static KeyMapping openKey;

    public static void init(IEventBus modBus) {
        // Register Mod Bus Events
        modBus.register(BedrockSkinsClient.class);
        
        // Register Game Bus Events
        NeoForge.EVENT_BUS.register(GameEvents.class);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        //? if >=1.21.11 {
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("bedrockskins", "controls"));
        //?} else {
        KeyMapping.Category category = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath("bedrockskins", "controls"));
        //?}
        
        openKey = new KeyMapping("key.bedrockskins.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category);
        
        toggleCapeKey = new KeyMapping("key.bedrockskins.toggle_cape", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
        toggleJacketKey = new KeyMapping("key.bedrockskins.toggle_jacket", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
        toggleLeftSleeveKey = new KeyMapping("key.bedrockskins.toggle_left_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
        toggleRightSleeveKey = new KeyMapping("key.bedrockskins.toggle_right_sleeve", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
        toggleLeftPantsKey = new KeyMapping("key.bedrockskins.toggle_left_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
        toggleRightPantsKey = new KeyMapping("key.bedrockskins.toggle_right_pants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
        toggleHatKey = new KeyMapping("key.bedrockskins.toggle_hat", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);
        toggleMainHandKey = new KeyMapping("key.bedrockskins.swap_main_hand", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category);

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
        event.enqueueWork(() -> {
            CommonLogic.reloadResources(Minecraft.getInstance());
        });
    }

    // Networking handler called from main class
    public static void handleSkinUpdatePacket(BedrockSkinsNetworking.SkinUpdatePayload payload) {
        CommonLogic.handleSkinUpdate(payload);
    }
    
    // Runtime events
    public static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            
            while (openKey.consumeClick()) {
                client.setScreen(getAppropriateSkinScreen(client.screen));
            }

            while (toggleCapeKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.CAPE);
            while (toggleJacketKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.JACKET);
            while (toggleLeftSleeveKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.LEFT_SLEEVE);
            while (toggleRightSleeveKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.RIGHT_SLEEVE);
            while (toggleLeftPantsKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.LEFT_PANTS_LEG);
            while (toggleRightPantsKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.RIGHT_PANTS_LEG);
            while (toggleHatKey.consumeClick()) CommonLogic.toggleModelPart(client, PlayerModelPart.HAT);
            while (toggleMainHandKey.consumeClick()) CommonLogic.toggleMainHand(client);
        }

        @SubscribeEvent
        public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            CommonLogic.applySavedSkinOnJoin(Minecraft.getInstance());
        }
    }

    public static net.minecraft.client.gui.screens.Screen getAppropriateSkinScreen(net.minecraft.client.gui.screens.Screen parent) {
        // For NeoForge, use Legacy4J screen if loaded, else fallback
        if (net.neoforged.fml.ModList.get().isLoaded("legacy")) {
            try {
                Class<?> screenClass = Class.forName("com.brandonitaly.bedrockskins.client.gui.legacy.Legacy4JChangeSkinScreen");
                var constructor = screenClass.getConstructor(net.minecraft.client.gui.screens.Screen.class);
                return (net.minecraft.client.gui.screens.Screen) constructor.newInstance(parent);
            } catch (Exception e) {
                // Fallback if reflection fails
                return new com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen(parent);
            }
        } else {
            return new com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen(parent);
        }
    }
}*/
//?}

// Shared Logic Container
class CommonLogic {
    static void toggleModelPart(Minecraft client, PlayerModelPart part) {
        if (client.player == null) return;
        boolean current = client.options.isModelPartEnabled(part);
        client.options.setModelPart(part, !current);
        client.options.save();
    }
    
    static void toggleMainHand(Minecraft client) {
        if (client.player == null) return;
        net.minecraft.world.entity.HumanoidArm currentHand = client.options.mainHand().get();
        net.minecraft.world.entity.HumanoidArm newHand = currentHand == net.minecraft.world.entity.HumanoidArm.LEFT 
            ? net.minecraft.world.entity.HumanoidArm.RIGHT 
            : net.minecraft.world.entity.HumanoidArm.LEFT;
        client.options.mainHand().set(newHand);
        client.options.save();
    }

    static void reloadResources(Minecraft client) {
        try {
            SkinPackLoader.loadPacks();
            FavoritesManager.load();
            SkinManager.load();
            BedrockModelManager.clearAllModels();
            if (client.player != null) {
                String localUuid = client.player.getUUID().toString();
                SkinId id = SkinManager.getSkin(client.player.getUUID());
                if (id != null) {
                    String pack = id.getPack() == null || id.getPack().isEmpty() ? "Remote" : id.getPack();
                    String name = id.getName() == null || id.getName().isEmpty() ? id.toString() : id.getName();
                    SkinManager.setSkin(localUuid, pack, name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void applySavedSkinOnJoin(Minecraft client) {
        try {
            BedrockSkinsState state = StateManager.readState();
            String savedKey = state.getSelected();
            if (savedKey == null || client.player == null) return;

            SkinId savedSkinId = SkinId.parse(savedKey);
            String pack = savedSkinId == null || savedSkinId.getPack().isEmpty() ? "Remote" : savedSkinId.getPack();
            String name = savedSkinId == null || savedSkinId.getName().isEmpty() ? savedKey : savedSkinId.getName();
            SkinManager.setSkin(client.player.getUUID().toString(), pack, name);

            LoadedSkin loadedSkin = SkinPackLoader.getLoadedSkin(savedSkinId);
            if (loadedSkin != null) {
                byte[] textureData = loadTextureData(client, loadedSkin);
                if (textureData.length > 0) {
                    ClientSkinSync.sendSetSkinPayload(savedSkinId, loadedSkin.getGeometryData().toString(), textureData);
                    
                    System.out.println("BedrockSkinsClient: Synced skin " + savedKey);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void handleSkinUpdate(BedrockSkinsNetworking.SkinUpdatePayload p) {
        SkinId id = p.getSkinId();
        java.util.UUID uuid = p.getUuid();
        String geom = p.getGeometry();
        byte[] tex = p.getTextureData();

        if (id == null) {
            SkinManager.resetSkin(uuid.toString());
        } else {
            String key = id.toString();
            SkinPackLoader.registerRemoteSkin(key, geom, tex);
            String pack = id.getPack() == null || id.getPack().isEmpty() ? "Remote" : id.getPack();
            String name = id.getName() == null || id.getName().isEmpty() ? id.toString() : id.getName();
            SkinManager.setSkin(uuid.toString(), pack, name);
        }
    }

    static byte[] loadTextureData(Minecraft client, LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource) {
                var resOpt = client.getResourceManager().getResource(((AssetSource.Resource) src).getId());
                if (resOpt.isPresent()) {
                    return resOpt.get().open().readAllBytes();
                }
                return new byte[0];
            } else if (src instanceof AssetSource.File) {
                File f = new File(((AssetSource.File) src).getPath());
                return java.nio.file.Files.readAllBytes(f.toPath());
            } else if (src instanceof AssetSource.Zip) {
                AssetSource.Zip z = (AssetSource.Zip) src;
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    ZipEntry entry = zip.getEntry(z.getInternalPath());
                    if (entry != null) {
                        try (var is = zip.getInputStream(entry)) {
                            return is.readAllBytes();
                        }
                    }
                }
                return new byte[0];
            } else {
                return new byte[0];
            }
        } catch (Exception e) {
            return new byte[0];
        }
    }
}