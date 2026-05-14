package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public class BedrockSkinsConfig {
    //? if fabric
    private static final Path CONFIG_PATH = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("bedrockskins.json");
    //? if neoforge
    // private static final Path CONFIG_PATH = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("bedrockskins.json");

    private static volatile boolean scanResourcePacksForSkins;
    private static volatile PaperDollMode paperDollMode;
    private static volatile PackSortOrder packSortOrder;
    private static volatile boolean skinAnimations;
    private static volatile boolean adjustCameraHeight;
    private static volatile int paperDollOffsetX;
    private static volatile int paperDollOffsetY;

    public enum PaperDollMode {
        NONE, BOTH, MAIN_MENU, PAUSE_MENU;

        public String translationKey() { return "bedrockskins.option.show_paper_doll." + name().toLowerCase(Locale.ROOT); }

        public static final Codec<PaperDollMode> CODEC = Codec.STRING.xmap(
            value -> { try { return valueOf(value.toUpperCase(Locale.ROOT)); } catch (Exception e) { return BOTH; } }, Enum::name
        );
    }

    public enum PackSortOrder {
        A_TO_Z, Z_TO_A;

        public String translationKey() { return "bedrockskins.option.sort." + name().toLowerCase(Locale.ROOT); }

        public static final Codec<PackSortOrder> CODEC = Codec.STRING.xmap(
            value -> { try { return valueOf(value.toUpperCase(Locale.ROOT)); } catch (Exception e) { return A_TO_Z; } }, Enum::name
        );
    }

    private record ConfigData(boolean scanResourcePacksForSkins, PaperDollMode paperDollMode, PackSortOrder packSortOrder, boolean skinAnimations, boolean adjustCameraHeight, int paperDollOffsetX, int paperDollOffsetY) {}

    private static final ConfigData DEFAULTS = new ConfigData(true, PaperDollMode.BOTH, PackSortOrder.A_TO_Z, true, false, 0, 0);

    private static final Codec<ConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("scanResourcePacksForSkins", DEFAULTS.scanResourcePacksForSkins()).forGetter(ConfigData::scanResourcePacksForSkins),
        PaperDollMode.CODEC.optionalFieldOf("showPaperDoll", DEFAULTS.paperDollMode()).forGetter(ConfigData::paperDollMode),
        PackSortOrder.CODEC.optionalFieldOf("packSortOrder", DEFAULTS.packSortOrder()).forGetter(ConfigData::packSortOrder),
        Codec.BOOL.optionalFieldOf("skinAnimations", DEFAULTS.skinAnimations()).forGetter(ConfigData::skinAnimations),
        Codec.BOOL.optionalFieldOf("adjustCameraHeight", DEFAULTS.adjustCameraHeight()).forGetter(ConfigData::adjustCameraHeight),
        Codec.INT.optionalFieldOf("paperDollOffsetX", DEFAULTS.paperDollOffsetX()).forGetter(ConfigData::paperDollOffsetX),
        Codec.INT.optionalFieldOf("paperDollOffsetY", DEFAULTS.paperDollOffsetY()).forGetter(ConfigData::paperDollOffsetY)
    ).apply(instance, ConfigData::new));

    static { load(); }

    public static final OptionInstance<Boolean> SCAN_RESOURCE_PACKS = OptionInstance.createBoolean(
        "bedrockskins.option.scan_resourcepacks", value -> Tooltip.create(Component.translatable("bedrockskins.option.scan_resourcepacks.tooltip")),
        isScanResourcePacksForSkinsEnabled(), value -> { setScanResourcePacksForSkins(value); reloadSkinPacks(); }
    );

    public static final OptionInstance<PaperDollMode> SHOW_PAPER_DOLL = new OptionInstance<>(
        "bedrockskins.option.show_paper_doll", value -> Tooltip.create(Component.translatable("bedrockskins.option.show_paper_doll.tooltip")),
        (caption, value) -> Component.translatable(value.translationKey()), new OptionInstance.Enum<>(Arrays.asList(PaperDollMode.values()), PaperDollMode.CODEC),
        getPaperDollMode(), BedrockSkinsConfig::setPaperDollMode
    );

    public static final OptionInstance<PackSortOrder> PACK_SORT_ORDER = new OptionInstance<>(
        "bedrockskins.option.pack_sort_order", OptionInstance.noTooltip(),
        (caption, value) -> Component.translatable(value.translationKey()), new OptionInstance.Enum<>(Arrays.asList(PackSortOrder.values()), PackSortOrder.CODEC),
        getPackSortOrder(), BedrockSkinsConfig::setPackSortOrder
    );

    public static final OptionInstance<Boolean> SKIN_ANIMATIONS = OptionInstance.createBoolean(
        "bedrockskins.option.skin_animations", value -> Tooltip.create(Component.translatable("bedrockskins.option.skin_animations.tooltip")),
        isSkinAnimationsEnabled(), BedrockSkinsConfig::setSkinAnimations
    );

    public static final OptionInstance<Boolean> ADJUST_CAMERA_HEIGHT = OptionInstance.createBoolean(
        "bedrockskins.option.adjust_camera_height", value -> Tooltip.create(Component.translatable("bedrockskins.option.adjust_camera_height.tooltip")),
        isAdjustCameraHeightEnabled(), value -> {
            setAdjustCameraHeight(value);
            if (net.minecraft.client.Minecraft.getInstance().player != null) net.minecraft.client.Minecraft.getInstance().player.refreshDimensions();
        }
    );

    public static boolean isScanResourcePacksForSkinsEnabled() { return scanResourcePacksForSkins; }
    public static void setScanResourcePacksForSkins(boolean enabled) { scanResourcePacksForSkins = enabled; save(); }

    public static PaperDollMode getPaperDollMode() { return paperDollMode; }
    public static void setPaperDollMode(PaperDollMode mode) { paperDollMode = mode == null ? PaperDollMode.BOTH : mode; save(); }

    public static PackSortOrder getPackSortOrder() { return packSortOrder; }
    public static void setPackSortOrder(PackSortOrder order) { packSortOrder = order == null ? PackSortOrder.A_TO_Z : order; save(); }

    public static boolean isShowPaperDollOnMainMenu() { return paperDollMode == PaperDollMode.BOTH || paperDollMode == PaperDollMode.MAIN_MENU; }
    public static boolean isShowPaperDollOnPauseScreen() { return paperDollMode == PaperDollMode.BOTH || paperDollMode == PaperDollMode.PAUSE_MENU; }

    public static boolean isSkinAnimationsEnabled() { return skinAnimations; }
    public static void setSkinAnimations(boolean enabled) { if (skinAnimations != enabled) { skinAnimations = enabled; save(); } }

    public static boolean isAdjustCameraHeightEnabled() { return adjustCameraHeight; }
    public static void setAdjustCameraHeight(boolean enabled) { if (adjustCameraHeight != enabled) { adjustCameraHeight = enabled; save(); } }

    public static int getPaperDollOffsetX() { return paperDollOffsetX; }
    public static void setPaperDollOffsetX(int offset) { paperDollOffsetX = offset; save(); }

    public static int getPaperDollOffsetY() { return paperDollOffsetY; }
    public static void setPaperDollOffsetY(int offset) { paperDollOffsetY = offset; save(); }

    public static OptionInstance<?>[] asOptions() {
        return new OptionInstance<?>[] { PACK_SORT_ORDER, SCAN_RESOURCE_PACKS, SHOW_PAPER_DOLL, SKIN_ANIMATIONS, ADJUST_CAMERA_HEIGHT };
    }

    private static void reloadSkinPacks() {
        try { SkinPackLoader.loadPacks(); SkinPackLoader.registerTextures(); } catch (Exception ignored) {}
    }

    private static void load() {
        ConfigData data = JsonCodecFileStore.read(CONFIG_PATH, CODEC, DEFAULTS, "BedrockSkinsConfig");
        scanResourcePacksForSkins = data.scanResourcePacksForSkins();
        paperDollMode = data.paperDollMode();
        packSortOrder = data.packSortOrder();
        skinAnimations = data.skinAnimations();
        adjustCameraHeight = data.adjustCameraHeight();
        paperDollOffsetX = data.paperDollOffsetX();
        paperDollOffsetY = data.paperDollOffsetY();
    }

    private static void save() {
        JsonCodecFileStore.write(CONFIG_PATH, CODEC, new ConfigData(scanResourcePacksForSkins, paperDollMode, packSortOrder, skinAnimations, adjustCameraHeight, paperDollOffsetX, paperDollOffsetY), "BedrockSkinsConfig");
    }

    public static void resetToDefault() {
        SCAN_RESOURCE_PACKS.set(DEFAULTS.scanResourcePacksForSkins());
        SHOW_PAPER_DOLL.set(DEFAULTS.paperDollMode());
        PACK_SORT_ORDER.set(DEFAULTS.packSortOrder());
        SKIN_ANIMATIONS.set(DEFAULTS.skinAnimations());
        ADJUST_CAMERA_HEIGHT.set(DEFAULTS.adjustCameraHeight());
        setPaperDollOffsetX(DEFAULTS.paperDollOffsetX());
        setPaperDollOffsetY(DEFAULTS.paperDollOffsetY());
        save();
    }
}