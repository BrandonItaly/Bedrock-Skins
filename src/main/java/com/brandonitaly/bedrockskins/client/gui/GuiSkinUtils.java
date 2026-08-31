package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.ClientSkinSync;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GuiSkinUtils {

    private GuiSkinUtils() {}

    public static String translatedOrFallback(String translationKey, String fallback) {
        String translated = SkinPackLoader.getTranslation(translationKey);
        return translated != null ? translated : fallback;
    }

    public static Component getSkinDisplayName(LoadedSkin skin) {
        return skin == null ? Component.empty() : Component.literal(getSkinDisplayNameText(skin));
    }

    public static String getSkinDisplayNameText(LoadedSkin skin) {
        return skin == null ? "" : translatedOrFallback(skin.safeSkinName, skin.skinDisplayName);
    }

    public static Optional<String> getSkinDescriptionText(LoadedSkin skin) {
        if (skin == null) return Optional.empty();
        String description = translatedOrFallback(skin.safeSkinName + ".description", "");
        return description.isEmpty() ? Optional.empty() : Optional.of(description);
    }

    public static String getPackDisplayName(String packId) {
        return translatedOrFallback(packId, packId);
    }

    public static boolean isSkinCurrentlyEquipped(LoadedSkin skin) {
        return Objects.equals(SkinManager.getLocalSelectedKey(), skin != null ? skin.skinId : null);
    }

    public static void applySelectedSkin(Minecraft minecraft, LoadedSkin skin) throws Exception {
        if (skin == null) return;

        SkinId skinId = skin.skinId != null ? skin.skinId : SkinId.of(skin.serializeName, skin.skinDisplayName);
        
        if (minecraft.player != null) {
            SkinManager.setLocalSkin(skinId);
            ClientSkinSync.syncCurrentSkin(minecraft);
            minecraft.player.refreshDimensions();
        } else {
            SkinManager.setLocalSkin(skinId);
        }
    }

    public static void resetSelectedSkin(Minecraft minecraft) {
        if (minecraft.player != null) {
            SkinManager.resetSkin(minecraft.player.getUUID());
            ClientSkinSync.sendResetSkinPayload();
            minecraft.player.refreshDimensions();
        } else {
            com.brandonitaly.bedrockskins.client.StateManager.updateSelectedSkin(null);
        }
    }

    public static void applyAutoSelectedPreview(Minecraft minecraft, PreviewPlayer previewPlayer, UUID previewUuid) {
        if (previewPlayer == null) return;
        
        SkinManager.resetPreviewSkin(previewUuid);
        previewPlayer.clearForcedBody();
        SkinId capeOverrideId = SkinManager.getLocalCapeOverride();
        Identifier capeId = null;
        if (capeOverrideId != null) {
            var capeSkin = SkinPackLoader.getLoadedSkin(capeOverrideId);
            if (capeSkin != null) {
                capeId = capeSkin.capeIdentifier;
            }
        }
        if (capeId != null) {
            previewPlayer.setForcedCape(capeId);
        } else {
            previewPlayer.clearForcedCape();
        }
        refreshAutoSelectedProfileSkin(minecraft, previewPlayer);
        previewPlayer.setUseLocalPlayerModel(false);
    }

    public static void refreshAutoSelectedProfileSkin(Minecraft minecraft, PreviewPlayer previewPlayer) {
        if (previewPlayer == null) return;
        
        var profile = minecraft.getGameProfile();
        previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
    }

    public static void applyLoadedSkinPreview(PreviewPlayer previewPlayer, UUID previewUuid, LoadedSkin skin) {
        applyLoadedSkinPreview(previewPlayer, previewUuid, skin, true);
    }

    public static void applyLoadedSkinPreview(PreviewPlayer previewPlayer, UUID previewUuid, LoadedSkin skin, boolean ignoreCapeOverrides) {
        if (previewPlayer == null) return;

        previewPlayer.clearForcedProfileSkin();
        previewPlayer.clearForcedBody();
        previewPlayer.setUseLocalPlayerModel(false);

        if (skin == null) {
            SkinManager.resetPreviewSkin(previewUuid);
            previewPlayer.setForcedCape(null);
            return;
        }

        if (skin.skinId != null) {
            SkinManager.setPreviewSkin(previewUuid, skin.skinId.pack(), skin.skinId.name());
            SkinPackLoader.registerTextureFor(skin.skinId);
            previewPlayer.setForcedBody(skin.identifier);
        }
        
        Identifier resolved = SkinManager.resolveCape(skin, !ignoreCapeOverrides);
        if (resolved != null) {
            if (resolved.equals(SkinManager.CAPE_NONE)) {
                previewPlayer.setForcedCape(null);
            } else {
                previewPlayer.setForcedCape(resolved);
            }
        } else {
            if (ignoreCapeOverrides) {
                previewPlayer.setForcedCape(null);
            } else {
                previewPlayer.clearForcedCape();
            }
        }
    }

    public static void cleanupPreview(UUID previewUuid) {
        SkinManager.resetPreviewSkin(previewUuid);
    }
}
