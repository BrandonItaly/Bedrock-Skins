package com.brandonitaly.bedrockskins.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//? }
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class BedrockSkinsConfig {
    private static final Gson GSON = new Gson();
    //? if fabric {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bedrockskins.json");
    //? }
    //? if neoforge {
    // private static final Path CONFIG_PATH = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("bedrockskins.json");
    //? }

    private static boolean scanResourcePacksForSkins = true;
    private static boolean enableBuiltInSkinPacks = true;

    public static final OptionInstance<Boolean> SCAN_RESOURCE_PACKS = OptionInstance.createBoolean(
        "bedrockskins.option.scan_resourcepacks",
        value -> Tooltip.create(Component.translatable("bedrockskins.option.scan_resourcepacks.tooltip")),
        isScanResourcePacksForSkinsEnabled(),
        value -> {
            setScanResourcePacksForSkins(value);
            reloadSkinPacks();
        }
    );

    public static final OptionInstance<Boolean> ENABLE_BUILT_IN_PACKS = OptionInstance.createBoolean(
        "bedrockskins.option.enable_builtin_packs",
        value -> Tooltip.create(Component.translatable("bedrockskins.option.enable_builtin_packs.tooltip")),
        isEnableBuiltInSkinPacksEnabled(),
        value -> {
            setEnableBuiltInSkinPacks(value);
            reloadSkinPacks();
        }
    );

    static {
        load();
    }

    public static synchronized boolean isScanResourcePacksForSkinsEnabled() {
        return scanResourcePacksForSkins;
    }

    public static synchronized void setScanResourcePacksForSkins(boolean enabled) {
        scanResourcePacksForSkins = enabled;
        save();
    }

    public static synchronized boolean isEnableBuiltInSkinPacksEnabled() {
        return enableBuiltInSkinPacks;
    }

    public static synchronized void setEnableBuiltInSkinPacks(boolean enabled) {
        enableBuiltInSkinPacks = enabled;
        save();
    }

    public static OptionInstance<?>[] asOptions() {
        return new OptionInstance<?>[] { SCAN_RESOURCE_PACKS, ENABLE_BUILT_IN_PACKS };
    }

    private static void reloadSkinPacks() {
        try {
            com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadPacks();
            com.brandonitaly.bedrockskins.pack.SkinPackLoader.registerTextures();
        } catch (Exception ignored) {}
    }

    private static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                    JsonObject obj = GSON.fromJson(r, JsonObject.class);
                    if (obj != null) {
                        if (obj.has("scanResourcePacksForSkins")) {
                            scanResourcePacksForSkins = obj.get("scanResourcePacksForSkins").getAsBoolean();
                        }
                        if (obj.has("enableBuiltInSkinPacks")) {
                            enableBuiltInSkinPacks = obj.get("enableBuiltInSkinPacks").getAsBoolean();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("BedrockSkinsConfig: failed to load config: " + e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("scanResourcePacksForSkins", scanResourcePacksForSkins);
            obj.addProperty("enableBuiltInSkinPacks", enableBuiltInSkinPacks);
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException e) {
            System.out.println("BedrockSkinsConfig: failed to save config: " + e);
        }
    }
}
