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
import java.util.Arrays;

public class BedrockSkinsConfig {
    //? if fabric {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bedrockskins.json");
    //? }
    //? if neoforge {
    // private static final Path CONFIG_PATH = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("bedrockskins.json");
    //? }

    private static boolean scanResourcePacksForSkins = true;
    private static boolean enableBuiltInSkinPacks = true;
    private static PaperDollMode paperDollMode = PaperDollMode.BOTH;
    private static boolean paperDollLeftSide = false;

    public enum PaperDollMode {
        NONE("bedrockskins.option.show_paper_doll.none"),
        BOTH("bedrockskins.option.show_paper_doll.both"),
        MAIN_MENU("bedrockskins.option.show_paper_doll.main_menu"),
        PAUSE_SCREEN("bedrockskins.option.show_paper_doll.pause_screen");

        private final String translationKey;

        PaperDollMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        public static final Codec<PaperDollMode> CODEC = Codec.STRING.xmap(
            value -> {
                for (PaperDollMode mode : values()) {
                    if (mode.name().equalsIgnoreCase(value)) {
                        return mode;
                    }
                }
                return BOTH;
            },
            PaperDollMode::name
        );
    }

    private record ConfigData(boolean scanResourcePacksForSkins, boolean enableBuiltInSkinPacks, PaperDollMode paperDollMode, boolean paperDollLeftSide) {
    }

    private static final ConfigData DEFAULTS = new ConfigData(true, true, PaperDollMode.BOTH, false);

    private static final Codec<ConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("scanResourcePacksForSkins", DEFAULTS.scanResourcePacksForSkins()).forGetter(ConfigData::scanResourcePacksForSkins),
        Codec.BOOL.optionalFieldOf("enableBuiltInSkinPacks", DEFAULTS.enableBuiltInSkinPacks()).forGetter(ConfigData::enableBuiltInSkinPacks),
        PaperDollMode.CODEC.optionalFieldOf("showPaperDoll", DEFAULTS.paperDollMode()).forGetter(ConfigData::paperDollMode),
        Codec.BOOL.optionalFieldOf("paperDollLeftSide", DEFAULTS.paperDollLeftSide()).forGetter(ConfigData::paperDollLeftSide)
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

    public static final OptionInstance<PaperDollMode> SHOW_PAPER_DOLL = new OptionInstance<>(
        "bedrockskins.option.show_paper_doll",
        value -> Tooltip.create(Component.translatable("bedrockskins.option.show_paper_doll.tooltip")),
        (caption, value) -> Component.translatable(value.translationKey()),
        new OptionInstance.Enum<>(Arrays.asList(PaperDollMode.values()), PaperDollMode.CODEC),
        getPaperDollMode(),
        BedrockSkinsConfig::setPaperDollMode
    );

    public static final OptionInstance<Boolean> PAPER_DOLL_LEFT_SIDE = new OptionInstance<>(
        "bedrockskins.option.paper_doll_side",
        value -> Tooltip.create(Component.translatable("bedrockskins.option.paper_doll_side.tooltip")),
        (caption, value) -> Component.translatable(value
            ? "bedrockskins.option.paper_doll_side.left"
            : "bedrockskins.option.paper_doll_side.right"),
        OptionInstance.BOOLEAN_VALUES,
        isPaperDollLeftSideEnabled(),
        BedrockSkinsConfig::setPaperDollLeftSide
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

    public static synchronized PaperDollMode getPaperDollMode() {
        return paperDollMode;
    }

    public static synchronized void setPaperDollMode(PaperDollMode mode) {
        paperDollMode = mode == null ? PaperDollMode.BOTH : mode;
        save();
    }

    public static synchronized boolean isShowPaperDollOnMainMenu() {
        return paperDollMode == PaperDollMode.BOTH || paperDollMode == PaperDollMode.MAIN_MENU;
    }

    public static synchronized boolean isShowPaperDollOnPauseScreen() {
        return paperDollMode == PaperDollMode.BOTH || paperDollMode == PaperDollMode.PAUSE_SCREEN;
    }

    public static synchronized boolean isPaperDollLeftSideEnabled() {
        return paperDollLeftSide;
    }

    public static synchronized void setPaperDollLeftSide(boolean enabled) {
        paperDollLeftSide = enabled;
        save();
    }

    public static OptionInstance<?>[] asOptions() {
        return new OptionInstance<?>[] { SCAN_RESOURCE_PACKS, ENABLE_BUILT_IN_PACKS, SHOW_PAPER_DOLL, PAPER_DOLL_LEFT_SIDE };
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
        paperDollMode = data.paperDollMode();
        paperDollLeftSide = data.paperDollLeftSide();
    }

    private static void save() {
        ConfigData data = new ConfigData(scanResourcePacksForSkins, enableBuiltInSkinPacks, paperDollMode, paperDollLeftSide);
        JsonCodecFileStore.write(CONFIG_PATH, CODEC, data, "BedrockSkinsConfig");
    }
}
