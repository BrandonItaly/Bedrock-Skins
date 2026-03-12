package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.ClientSkinSync;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.StateManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.ExternalAssetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public final class GuiSkinUtils {
    public static final String AUTO_SELECTED_TRANSLATION_KEY = "bedrockskins.skin.auto_selected";
    public static final String AUTO_SELECTED_INTERNAL_NAME = "__auto_selected__";
    public static final String STANDARD_PACK_ID = "skinpack.Standard";
    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";

    private GuiSkinUtils() {
    }

    private static String translatedOrFallback(String translationKey, String fallback) {
        String translated = SkinPackLoader.getTranslation(translationKey);
        return translated != null ? translated : fallback;
    }

    private static String favoritesDisplayName() {
        return Component.translatable("bedrockskins.gui.favorites").getString();
    }

    private static String skinDisplayNameText(LoadedSkin skin) {
        return translatedOrFallback(skin.getSafeSkinName(), skin.getSkinDisplayName());
    }

    public static String getTranslatedOrFallback(String translationKey, String fallback) {
        return translatedOrFallback(translationKey, fallback);
    }

    public static boolean isAutoSelectedSkin(LoadedSkin skin) {
        return skin != null
            && "Standard".equals(skin.getSerializeName())
            && AUTO_SELECTED_INTERNAL_NAME.equals(skin.getSkinDisplayName());
    }

    public static boolean isAutoSelectedSkinId(SkinId skinId) {
        return skinId != null
            && "Standard".equals(skinId.getPack())
            && AUTO_SELECTED_INTERNAL_NAME.equals(skinId.getName());
    }

    public static LoadedSkin createAutoSelectedSkin(LoadedSkin template) {
        if (template == null) {
            return null;
        }
        return new LoadedSkin(
                "Standard",
                "Standard",
                AUTO_SELECTED_INTERNAL_NAME,
                template.getGeometryData(),
                template.getTexture(),
                null,
                false
        );
    }

    public static List<LoadedSkin> withAutoSelectedStandardFirst(List<LoadedSkin> standardSkins) {
        if (standardSkins == null || standardSkins.isEmpty()) {
            return List.of();
        }

        List<LoadedSkin> merged = new java.util.ArrayList<>();
        LoadedSkin autoSelected = createAutoSelectedSkin(standardSkins.getFirst());
        if (autoSelected != null) {
            merged.add(autoSelected);
        }

        for (LoadedSkin skin : standardSkins) {
            if (!isAutoSelectedSkin(skin)) {
                merged.add(skin);
            }
        }
        return merged;
    }

    public static LoadedSkin resolveAutoSelectedFromStandard(List<LoadedSkin> standardSkins) {
        if (standardSkins == null || standardSkins.isEmpty()) {
            return null;
        }
        for (LoadedSkin skin : standardSkins) {
            if (isAutoSelectedSkin(skin)) {
                return skin;
            }
        }
        return createAutoSelectedSkin(standardSkins.getFirst());
    }

    public static Component getSkinDisplayName(LoadedSkin skin) {
        if (isAutoSelectedSkin(skin)) {
            return Component.translatable(AUTO_SELECTED_TRANSLATION_KEY);
        }
        if (skin == null) {
            return Component.empty();
        }
        return Component.literal(skinDisplayNameText(skin));
    }

    public static String getSkinDisplayNameText(LoadedSkin skin) {
        if (skin == null) {
            return "";
        }
        if (isAutoSelectedSkin(skin)) {
            return Component.translatable(AUTO_SELECTED_TRANSLATION_KEY).getString();
        }
        return skinDisplayNameText(skin);
    }

    public static Optional<String> getSkinDescriptionText(LoadedSkin skin) {
        if (skin == null) {
            return Optional.empty();
        }
        String description = translatedOrFallback(skin.getSafeSkinName() + ".description", "");
        return (description == null || description.isEmpty()) ? Optional.empty() : Optional.of(description);
    }

    public static String getPackDisplayName(String packId, LoadedSkin firstSkin) {
        if (FAVORITES_PACK_ID.equals(packId)) {
            return favoritesDisplayName();
        }
        if (firstSkin != null) {
            return translatedOrFallback(firstSkin.getSafePackName(), firstSkin.getPackDisplayName());
        }
        return translatedOrFallback(packId, packId);
    }

    public static String getPackTranslationKey(String packId, LoadedSkin firstSkin) {
        if (FAVORITES_PACK_ID.equals(packId)) {
            return "bedrockskins.gui.favorites";
        }
        if (firstSkin != null) {
            return firstSkin.getSafePackName();
        }
        return packId;
    }

    public static String getPackFallbackName(String packId, LoadedSkin firstSkin) {
        if (FAVORITES_PACK_ID.equals(packId)) {
            return favoritesDisplayName();
        }
        if (firstSkin != null) {
            return firstSkin.getPackDisplayName();
        }
        return packId;
    }

    public static boolean isSkinCurrentlyEquipped(LoadedSkin skin) {
        SkinId currentSkinKey = SkinManager.getLocalSelectedKey();
        return isAutoSelectedSkin(skin) ? currentSkinKey == null : Objects.equals(currentSkinKey, skin != null ? skin.getSkinId() : null);
    }

    public static void applySelectedSkin(Minecraft minecraft, LoadedSkin skin) throws Exception {
        if (skin == null) {
            return;
        }
        if (isAutoSelectedSkin(skin)) {
            resetSelectedSkin(minecraft);
            return;
        }

        SkinId skinId = skin.getSkinId() != null ? skin.getSkinId() : SkinId.of(skin.getSerializeName(), skin.getSkinDisplayName());
        if (minecraft.player != null) {
            SkinManager.setSkin(minecraft.player.getUUID(), skinId.pack(), skinId.name());
            byte[] textureData = ExternalAssetUtil.loadTextureData(skin, minecraft);
            ClientSkinSync.sendSetSkinPayload(skinId, skin.getGeometryData().toString(), textureData);
            minecraft.player.refreshDimensions();
        } else {
            StateManager.saveState(FavoritesManager.getFavoriteKeys(), skinId.toString());
        }
    }

    public static void resetSelectedSkin(Minecraft minecraft) {
        if (minecraft.player != null) {
            SkinManager.resetSkin(minecraft.player.getUUID());
            ClientSkinSync.sendResetSkinPayload();
            minecraft.player.refreshDimensions();
        } else {
            StateManager.saveState(FavoritesManager.getFavoriteKeys(), null);
        }
    }

    public static void applyAutoSelectedPreview(Minecraft minecraft, PreviewPlayer previewPlayer, UUID previewUuid) {
        if (previewPlayer == null) {
            return;
        }
        SkinManager.resetPreviewSkin(previewUuid);
        previewPlayer.clearForcedBody();
        previewPlayer.clearForcedCape();
        refreshAutoSelectedProfileSkin(minecraft, previewPlayer);
        previewPlayer.setUseLocalPlayerModel(false);
    }

    public static void refreshAutoSelectedProfileSkin(Minecraft minecraft, PreviewPlayer previewPlayer) {
        if (previewPlayer == null) {
            return;
        }
        var profile = minecraft.getGameProfile();
        previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
    }

    public static void applyLoadedSkinPreview(PreviewPlayer previewPlayer, UUID previewUuid, LoadedSkin skin) {
        if (previewPlayer == null) {
            return;
        }
        if (skin == null) {
            SkinManager.resetPreviewSkin(previewUuid);
            previewPlayer.clearForcedProfileSkin();
            previewPlayer.clearForcedBody();
            previewPlayer.clearForcedCape();
            previewPlayer.setUseLocalPlayerModel(false);
            return;
        }

        SkinId skinId = skin.getSkinId();
        if (skinId != null) {
            SkinManager.setPreviewSkin(previewUuid, skinId.pack(), skinId.name());
            SkinPackLoader.registerTextureFor(skinId);
        }
        previewPlayer.clearForcedProfileSkin();
        previewPlayer.clearForcedBody();
        previewPlayer.setForcedCape(skin.capeIdentifier);
        previewPlayer.setUseLocalPlayerModel(false);
    }

    public static void cleanupPreview(UUID previewUuid) {
        SkinManager.resetPreviewSkin(previewUuid);
        PreviewPlayer.PreviewPlayerPool.remove(previewUuid);
    }
}