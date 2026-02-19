package com.brandonitaly.bedrockskins.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//? }
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;

public class BedrockSkinsConfig {
    //? if fabric {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bedrockskins.json");
    //? }
    //? if neoforge {
    // private static final Path CONFIG_PATH = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("bedrockskins.json");
    //? }

    private static boolean scanResourcePacksForSkins = true;
    private static boolean enableBuiltInSkinPacks = true;

    private record ConfigData(boolean scanResourcePacksForSkins, boolean enableBuiltInSkinPacks) {
    }

    private static final ConfigData DEFAULTS = new ConfigData(true, true);

    private static final Codec<ConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("scanResourcePacksForSkins", DEFAULTS.scanResourcePacksForSkins()).forGetter(ConfigData::scanResourcePacksForSkins),
        Codec.BOOL.optionalFieldOf("enableBuiltInSkinPacks", DEFAULTS.enableBuiltInSkinPacks()).forGetter(ConfigData::enableBuiltInSkinPacks)
    ).apply(instance, ConfigData::new));

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
        ConfigData data = JsonCodecFileStore.read(CONFIG_PATH, CODEC, DEFAULTS, "BedrockSkinsConfig");
        scanResourcePacksForSkins = data.scanResourcePacksForSkins();
        enableBuiltInSkinPacks = data.enableBuiltInSkinPacks();
    }

    private static void save() {
        ConfigData data = new ConfigData(scanResourcePacksForSkins, enableBuiltInSkinPacks);
        JsonCodecFileStore.write(CONFIG_PATH, CODEC, data, "BedrockSkinsConfig");
    }
}
