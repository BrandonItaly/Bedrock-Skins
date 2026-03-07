package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?}
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public class BedrockSkinsConfig {
    //? if fabric {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bedrockskins.json");
    //?}
    //? if neoforge {
    /*private static final Path CONFIG_PATH = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("bedrockskins.json");*/
    //?}

    private static volatile boolean scanResourcePacksForSkins;
    private static volatile boolean enableBuiltInSkinPacks;
    private static volatile PaperDollMode paperDollMode;
    private static volatile boolean paperDollLeftSide;
    private static volatile boolean skinAnimations;
    private static volatile boolean adjustCameraHeight;

    public enum PaperDollMode {
        NONE("bedrockskins.option.show_paper_doll.none"),
        BOTH("bedrockskins.option.show_paper_doll.both"),
        MAIN_MENU("bedrockskins.option.show_paper_doll.main_menu"),
        PAUSE_MENU("bedrockskins.option.show_paper_doll.pause_menu");

        private final String translationKey;

        PaperDollMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        public static final Codec<PaperDollMode> CODEC = Codec.STRING.xmap(
            value -> {
                try {
                    return valueOf(value.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException | NullPointerException e) {
                    return BOTH;
                }
            },
            PaperDollMode::name
        );
    }

    private record ConfigData(boolean scanResourcePacksForSkins, boolean enableBuiltInSkinPacks, PaperDollMode paperDollMode, boolean paperDollLeftSide, boolean skinAnimations, boolean adjustCameraHeight) {}

    private static final ConfigData DEFAULTS = new ConfigData(true, true, PaperDollMode.BOTH, false, true, false);

    private static final Codec<ConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("scanResourcePacksForSkins", DEFAULTS.scanResourcePacksForSkins()).forGetter(ConfigData::scanResourcePacksForSkins),
        Codec.BOOL.optionalFieldOf("enableBuiltInSkinPacks", DEFAULTS.enableBuiltInSkinPacks()).forGetter(ConfigData::enableBuiltInSkinPacks),
        PaperDollMode.CODEC.optionalFieldOf("showPaperDoll", DEFAULTS.paperDollMode()).forGetter(ConfigData::paperDollMode),
        Codec.BOOL.optionalFieldOf("paperDollLeftSide", DEFAULTS.paperDollLeftSide()).forGetter(ConfigData::paperDollLeftSide),
        Codec.BOOL.optionalFieldOf("skinAnimations", DEFAULTS.skinAnimations()).forGetter(ConfigData::skinAnimations),
        Codec.BOOL.optionalFieldOf("adjustCameraHeight", DEFAULTS.adjustCameraHeight()).forGetter(ConfigData::adjustCameraHeight)
    ).apply(instance, ConfigData::new));

    // Ensure load happens BEFORE OptionInstances are created
    static {
        load();
    }

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
        (caption, value) -> Component.translatable(value ? "bedrockskins.option.paper_doll_side.left" : "bedrockskins.option.paper_doll_side.right"),
        OptionInstance.BOOLEAN_VALUES,
        isPaperDollLeftSideEnabled(),
        BedrockSkinsConfig::setPaperDollLeftSide
    );

    public static final OptionInstance<Boolean> SKIN_ANIMATIONS = OptionInstance.createBoolean(
        "bedrockskins.option.skin_animations",
        value -> Tooltip.create(Component.translatable("bedrockskins.option.skin_animations.tooltip")),
        isSkinAnimationsEnabled(),
        BedrockSkinsConfig::setSkinAnimations
    );

    public static final OptionInstance<Boolean> ADJUST_CAMERA_HEIGHT = OptionInstance.createBoolean(
        "bedrockskins.option.adjust_camera_height",
        value -> Tooltip.create(Component.translatable("bedrockskins.option.adjust_camera_height.tooltip")),
        isAdjustCameraHeightEnabled(),
        value -> {
            setAdjustCameraHeight(value);
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                net.minecraft.client.Minecraft.getInstance().player.refreshDimensions();
            }
        }
    );

    public static boolean isScanResourcePacksForSkinsEnabled() {
        return scanResourcePacksForSkins;
    }

    public static void setScanResourcePacksForSkins(boolean enabled) {
        scanResourcePacksForSkins = enabled;
        save();
    }

    public static boolean isEnableBuiltInSkinPacksEnabled() {
        return enableBuiltInSkinPacks;
    }

    public static void setEnableBuiltInSkinPacks(boolean enabled) {
        enableBuiltInSkinPacks = enabled;
        save();
    }

    public static PaperDollMode getPaperDollMode() {
        return paperDollMode;
    }

    public static void setPaperDollMode(PaperDollMode mode) {
        paperDollMode = mode == null ? PaperDollMode.BOTH : mode;
        save();
    }

    public static boolean isShowPaperDollOnMainMenu() {
        return paperDollMode == PaperDollMode.BOTH || paperDollMode == PaperDollMode.MAIN_MENU;
    }

    public static boolean isShowPaperDollOnPauseScreen() {
        return paperDollMode == PaperDollMode.BOTH || paperDollMode == PaperDollMode.PAUSE_MENU;
    }

    public static boolean isPaperDollLeftSideEnabled() {
        return paperDollLeftSide;
    }

    public static void setPaperDollLeftSide(boolean enabled) {
        paperDollLeftSide = enabled;
        save();
    }

    public static boolean isSkinAnimationsEnabled() {
        return skinAnimations;
    }

    public static void setSkinAnimations(boolean enabled) {
        skinAnimations = enabled;
        save();
    }

    public static boolean isAdjustCameraHeightEnabled() {
        return adjustCameraHeight;
    }

    public static void setAdjustCameraHeight(boolean enabled) {
        adjustCameraHeight = enabled;
        save();
    }

    public static OptionInstance<?>[] asOptions() {
        return new OptionInstance<?>[] { SCAN_RESOURCE_PACKS, ENABLE_BUILT_IN_PACKS, SHOW_PAPER_DOLL, PAPER_DOLL_LEFT_SIDE, SKIN_ANIMATIONS, ADJUST_CAMERA_HEIGHT };
    }

    private static void reloadSkinPacks() {
        try {
            SkinPackLoader.loadPacks();
            SkinPackLoader.registerTextures();
        } catch (Exception ignored) {}
    }

    private static void load() {
        ConfigData data = JsonCodecFileStore.read(CONFIG_PATH, CODEC, DEFAULTS, "BedrockSkinsConfig");
        scanResourcePacksForSkins = data.scanResourcePacksForSkins();
        enableBuiltInSkinPacks = data.enableBuiltInSkinPacks();
        paperDollMode = data.paperDollMode();
        paperDollLeftSide = data.paperDollLeftSide();
        skinAnimations = data.skinAnimations();
        adjustCameraHeight = data.adjustCameraHeight();
    }

    private static void save() {
        ConfigData data = new ConfigData(scanResourcePacksForSkins, enableBuiltInSkinPacks, paperDollMode, paperDollLeftSide, skinAnimations, adjustCameraHeight);
        JsonCodecFileStore.write(CONFIG_PATH, CODEC, data, "BedrockSkinsConfig");
    }
}