package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.platform.InputConstants;
import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.io.File;

public class BedrockSkinsClient implements ClientModInitializer {
    
    // Keybinds for skin customization
    private static KeyMapping toggleCapeKey;
    private static KeyMapping toggleJacketKey;
    private static KeyMapping toggleLeftSleeveKey;
    private static KeyMapping toggleRightSleeveKey;
    private static KeyMapping toggleLeftPantsKey;
    private static KeyMapping toggleRightPantsKey;
    private static KeyMapping toggleHatKey;
    private static KeyMapping toggleMainHandKey;
    
    //? if >1.21.8 {
    private static KeyMapping.Category keybindCategory;
    //?}
    
    @Override
    public void onInitializeClient() {
        //? if >1.21.8 {
        keybindCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bedrockskins", "controls"));
        //?}
        registerKeyBinding();
        registerCustomizationKeybinds();
        registerLifecycleEvents();
        registerNetworking();
    }

    private void registerKeyBinding() {
        try {
            String keyId = "key.bedrockskins.open";
            //? if <=1.21.8 {
            /*KeyBinding key = new KeyBinding(keyId, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "bedrockskins.controls");*/
            //?} else {
            KeyMapping key = new KeyMapping(keyId, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, keybindCategory);
            //?}
            KeyMapping openKey = KeyBindingHelper.registerKeyBinding(key);

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                while (openKey.consumeClick()) {
                    client.setScreen(new SkinSelectionScreen(client.screen));
                }
            });
            System.out.println("BedrockSkinsClient: Registered keybinding (K)");
        } catch (Exception e) {
            System.out.println("BedrockSkinsClient: Keybinding error: " + e);
        }
    }
    
    private void registerCustomizationKeybinds() {
        try {
            //? if <=1.21.8 {
            /*String categoryName = "bedrockskins.controls";*/
            //?}
            
            // Register all customization keybinds (unbound by default - GLFW.GLFW_KEY_UNKNOWN)
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
            
            // Register tick handler for customization keybinds
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player == null) return;
                
                while (toggleCapeKey.consumeClick()) {
                    toggleModelPart(client, PlayerModelPart.CAPE);
                }
                while (toggleJacketKey.consumeClick()) {
                    toggleModelPart(client, PlayerModelPart.JACKET);
                }
                while (toggleLeftSleeveKey.consumeClick()) {
                    toggleModelPart(client, PlayerModelPart.LEFT_SLEEVE);
                }
                while (toggleRightSleeveKey.consumeClick()) {
                    toggleModelPart(client, PlayerModelPart.RIGHT_SLEEVE);
                }
                while (toggleLeftPantsKey.consumeClick()) {
                    toggleModelPart(client, PlayerModelPart.LEFT_PANTS_LEG);
                }
                while (toggleRightPantsKey.consumeClick()) {
                    toggleModelPart(client, PlayerModelPart.RIGHT_PANTS_LEG);
                }
                while (toggleHatKey.consumeClick()) {
                    toggleModelPart(client, PlayerModelPart.HAT);
                }
                while (toggleMainHandKey.consumeClick()) {
                    toggleMainHand(client);
                }
            });
            
            System.out.println("BedrockSkinsClient: Registered customization keybinds");
        } catch (Exception e) {
            System.out.println("BedrockSkinsClient: Customization keybinding error: " + e);
        }
    }
    
    private void toggleModelPart(Minecraft client, PlayerModelPart part) {
        if (client.player == null) return;
        
        boolean current = client.options.isModelPartEnabled(part);
        
        // Toggle the model part through options
        client.options.setModelPart(part, !current);
        
        // Save the updated customization
        client.options.save();
    }
    
    private void toggleMainHand(Minecraft client) {
        if (client.player == null) return;
        
        // Toggle main hand between left and right
        net.minecraft.world.entity.HumanoidArm currentHand = client.options.mainHand().get();
        net.minecraft.world.entity.HumanoidArm newHand = currentHand == net.minecraft.world.entity.HumanoidArm.LEFT 
            ? net.minecraft.world.entity.HumanoidArm.RIGHT 
            : net.minecraft.world.entity.HumanoidArm.LEFT;
        
        client.options.mainHand().set(newHand);
        client.options.save();
    }

    private void registerLifecycleEvents() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            reloadResources(client);

            // Register a synchronous reload listener that triggers a resource reload
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Reloader());
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> applySavedSkinOnJoin(client));
    }

    private final class Reloader implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {
        @Override
        public Identifier getFabricId() {
            return Identifier.fromNamespaceAndPath("bedrockskins", "reloader");
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            reloadResources(Minecraft.getInstance());
        }
    }

    private void reloadResources(Minecraft client) {
        try {
            SkinPackLoader.loadPacks();
            FavoritesManager.load();
            SkinManager.load();
            BedrockModelManager.clearAllModels();

            if (client.player != null) {
                String localUuid = client.player.getUUID().toString();
                String key = SkinManager.getSkin(localUuid);
                if (key != null) {
                    String[] parts = key.split(":", 2);
                    String pack = parts.length == 2 ? parts[0] : "Remote";
                    String name = parts.length == 2 ? parts[1] : key;
                    SkinManager.setSkin(localUuid, pack, name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applySavedSkinOnJoin(Minecraft client) {
        try {
            BedrockSkinsState state = StateManager.readState();
            String savedKey = state.getSelected();
            if (savedKey == null) return;
            if (client.player == null) return;

            String[] parts = savedKey.split(":", 2);
            String pack = parts.length == 2 ? parts[0] : "Remote";
            String name = parts.length == 2 ? parts[1] : savedKey;
            SkinManager.setSkin(client.player.getUUID().toString(), pack, name);

            com.brandonitaly.bedrockskins.pack.LoadedSkin loadedSkin = SkinPackLoader.loadedSkins.get(savedKey);
            if (loadedSkin != null) {
                byte[] textureData = loadTextureData(client, loadedSkin);
                if (textureData.length > 0) {
                    ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload(savedKey, loadedSkin.getGeometryData().toString(), textureData));
                    System.out.println("BedrockSkinsClient: Synced skin " + savedKey);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(BedrockSkinsNetworking.SkinUpdatePayload.ID, (payload, context) -> {
            BedrockSkinsNetworking.SkinUpdatePayload p = payload;
            context.client().execute(() -> {
                String key = p.getSkinKey();
                java.util.UUID uuid = p.getUuid();
                String geom = p.getGeometry();
                byte[] tex = p.getTextureData();

                if ("RESET".equals(key)) {
                    SkinManager.resetSkin(uuid.toString());
                } else {
                    SkinPackLoader.registerRemoteSkin(key, geom, tex);
                    String[] parts = key.split(":", 2);
                    String pack = parts.length == 2 ? parts[0] : "Remote";
                    String name = parts.length == 2 ? parts[1] : key;
                    SkinManager.setSkin(uuid.toString(), pack, name);
                }
            });
        });
    }

    // --- Helpers ---

    private byte[] loadTextureData(Minecraft client, com.brandonitaly.bedrockskins.pack.LoadedSkin skin) {
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
            } else {
                return new byte[0];
            }
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
