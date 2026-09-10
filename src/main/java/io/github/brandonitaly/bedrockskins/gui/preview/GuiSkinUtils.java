package io.github.brandonitaly.bedrockskins.gui.preview;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.ClientSkinSync;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.MojangSkinManager;
import io.github.brandonitaly.bedrockskins.client.persistence.StateManager;

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
        if (MinecraftAccountSkin.is(skin)) return SkinManager.getLocalSelectedKey() == null;
        if (ImportSkinAction.is(skin)) return false;
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
            StateManager.updateSelectedSkin(null);
        }
    }

    public static void applyAutoSelectedPreview(Minecraft minecraft, PreviewPlayer previewPlayer, UUID previewUuid) {
        if (previewPlayer == null) return;
        
        SkinManager.resetPreviewSkin(previewUuid);
        previewPlayer.clearForcedBody();
        previewPlayer.clearForcedModel();
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
        
        PlayerSkin sessionSkin = BedrockSessionSkin.getSessionPlayerSkin();
        if (sessionSkin != null) {
            previewPlayer.setForcedProfileSkin(sessionSkin);
            previewPlayer.setForcedModel(sessionSkin.model());
            return;
        }

        var profile = minecraft.getGameProfile();
        if (profile != null) {
            PlayerSkin skin = minecraft.getSkinManager().createLookup(profile, false).get();
            previewPlayer.setForcedProfileSkin(skin);
            if (skin != null) {
                previewPlayer.setForcedModel(skin.model());
            }
        }
    }

    public static void applyLoadedSkinPreview(PreviewPlayer previewPlayer, UUID previewUuid, LoadedSkin skin) {
        applyLoadedSkinPreview(previewPlayer, previewUuid, skin, true);
    }

    public static void applyLoadedSkinPreview(PreviewPlayer previewPlayer, UUID previewUuid, LoadedSkin skin, boolean ignoreCapeOverrides) {
        if (previewPlayer == null) return;

        if (MinecraftAccountSkin.is(skin)) {
            applyAutoSelectedPreview(Minecraft.getInstance(), previewPlayer, previewUuid);
            return;
        }

        previewPlayer.clearForcedProfileSkin();
        previewPlayer.clearForcedBody();
        previewPlayer.setUseLocalPlayerModel(false);
        if (skin != null) {
            boolean slim = MojangSkinManager.isSkinSlim(skin);
            previewPlayer.setForcedModel(slim ? PlayerModelType.SLIM : PlayerModelType.WIDE);
        } else {
            previewPlayer.clearForcedModel();
        }

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

    public static void applyCurrentEquippedSkin(Minecraft minecraft, PreviewPlayer previewPlayer, UUID previewUuid) {
        if (previewPlayer == null) return;
        SkinId currentKey = SkinManager.getLocalSelectedKey();
        if (currentKey != null) {
            LoadedSkin skin = SkinPackLoader.getLoadedSkin(currentKey);
            if (skin != null) {
                applyLoadedSkinPreview(previewPlayer, previewUuid, skin, false);
                return;
            }
        }
        applyAutoSelectedPreview(minecraft, previewPlayer, previewUuid);
    }

    public static void cleanupPreview(UUID previewUuid) {
        SkinManager.resetPreviewSkin(previewUuid);
    }
}
