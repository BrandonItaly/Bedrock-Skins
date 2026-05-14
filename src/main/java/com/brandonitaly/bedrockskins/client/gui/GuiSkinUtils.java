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

public final class GuiSkinUtils {
    public static final String STANDARD_PACK_ID = "skinpack.Standard";
    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";

    private GuiSkinUtils() {
    }

    public static String translatedOrFallback(String translationKey, String fallback) {
        String translated = SkinPackLoader.getTranslation(translationKey);
        return translated != null ? translated : fallback;
    }

    private static String favoritesDisplayName() {
        return Component.translatable("bedrockskins.gui.favorites").getString();
    }

    private static String skinDisplayNameText(LoadedSkin skin) {
        return translatedOrFallback(skin.safeSkinName, skin.skinDisplayName);
    }

    public static Component getSkinDisplayName(LoadedSkin skin) {
        if (skin == null) {
            return Component.empty();
        }
        return Component.literal(skinDisplayNameText(skin));
    }

    public static String getSkinDisplayNameText(LoadedSkin skin) {
        if (skin == null) {
            return "";
        }
        return skinDisplayNameText(skin);
    }

    public static Optional<String> getSkinDescriptionText(LoadedSkin skin) {
        if (skin == null) {
            return Optional.empty();
        }
        String description = translatedOrFallback(skin.safeSkinName + ".description", "");
        return (description == null || description.isEmpty()) ? Optional.empty() : Optional.of(description);
    }

    public static String getPackDisplayName(String packId, LoadedSkin firstSkin) {
        if (FAVORITES_PACK_ID.equals(packId)) {
            return favoritesDisplayName();
        }
        if (firstSkin != null) {
            return translatedOrFallback(firstSkin.safePackName, firstSkin.packDisplayName);
        }
        return translatedOrFallback(packId, packId);
    }

    public static String getPackTranslationKey(String packId, LoadedSkin firstSkin) {
        if (FAVORITES_PACK_ID.equals(packId)) {
            return "bedrockskins.gui.favorites";
        }
        if (firstSkin != null) {
            return firstSkin.safePackName;
        }
        return packId;
    }

    public static String getPackFallbackName(String packId, LoadedSkin firstSkin) {
        if (FAVORITES_PACK_ID.equals(packId)) {
            return favoritesDisplayName();
        }
        if (firstSkin != null) {
            return firstSkin.packDisplayName;
        }
        return packId;
    }

    public static boolean isSkinCurrentlyEquipped(LoadedSkin skin) {
        SkinId currentSkinKey = SkinManager.getLocalSelectedKey();
        return Objects.equals(currentSkinKey, skin != null ? skin.skinId : null);
    }

    public static void applySelectedSkin(Minecraft minecraft, LoadedSkin skin) throws Exception {
        if (skin == null) {
            return;
        }

        SkinId skinId = skin.skinId != null ? skin.skinId : SkinId.of(skin.serializeName, skin.skinDisplayName);
        if (minecraft.player != null) {
            SkinManager.setSkin(minecraft.player.getUUID(), skinId);
            byte[] textureData = ExternalAssetUtil.loadTextureData(skin, minecraft);
            ClientSkinSync.sendSetSkinPayload(skinId, skin.geometryData.toString(), textureData);
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

        SkinId skinId = skin.skinId;
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