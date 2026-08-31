package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SkinManager {
    private SkinManager() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, SkinId> playerSkins = new HashMap<>();
    private static final Map<UUID, SkinId> previewSkins = new HashMap<>();
    private static SkinId localCapeOverride = null;
    private static Identifier localAccountCapeOverride = null;
    public static final Identifier CAPE_NONE = Identifier.fromNamespaceAndPath("bedrockskins", "none");
    public static final SkinId CAPE_NONE_SKIN_ID = SkinId.of("none", "none");
    private static final Identifier VANILLA_ELYTRA = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");

    public static Identifier resolveCape(LoadedSkin loadedSkin, boolean isLocalPlayer) {
        return resolveCape(loadedSkin, isLocalPlayer, localCapeOverride, localAccountCapeOverride);
    }

    static Identifier resolveCape(LoadedSkin loadedSkin, boolean isLocalPlayer, SkinId capeOverrideId, Identifier accountCapeOverride) {
        Identifier customCapeId = null;

        // 1. Explicit custom skin pack cape override (local player only)
        if (isLocalPlayer) {
            if (capeOverrideId != null && !capeOverrideId.equals(CAPE_NONE_SKIN_ID)) {
                var capeSkin = SkinPackLoader.getLoadedSkin(capeOverrideId);
                if (capeSkin != null && capeSkin.capeIdentifier != null) {
                    customCapeId = capeSkin.capeIdentifier;
                }
            }
        }

        // 2. Bedrock skin's default built-in cape
        boolean ignoreBuiltIn = isLocalPlayer && CAPE_NONE_SKIN_ID.equals(capeOverrideId);
        if (!ignoreBuiltIn && customCapeId == null && loadedSkin != null && loadedSkin.capeIdentifier != null) {
            customCapeId = loadedSkin.capeIdentifier;
        }

        // 3. Local Mojang account cape override (local player only)
        if (customCapeId == null && isLocalPlayer) {
            if (accountCapeOverride != null) {
                if (accountCapeOverride.equals(CAPE_NONE)) {
                    return CAPE_NONE;
                } else {
                    customCapeId = accountCapeOverride;
                }
            }
        }

        if (customCapeId != null) {
            return customCapeId;
        }

        return null;
    }

    public static PlayerSkin applySkinOverrides(UUID playerId, PlayerSkin original) {
        if (playerId == null || original == null) return original;

        SkinId skinId = getSkin(playerId);
        LoadedSkin loadedSkin = SkinPackLoader.getLoadedSkin(skinId);
        ClientAsset.Texture body = original.body();
        ClientAsset.Texture cape = original.cape();
        ClientAsset.Texture elytra = original.elytra();
        boolean modified = false;

        if (loadedSkin != null && loadedSkin.identifier != null) {
            body = new ClientAsset.ResourceTexture(loadedSkin.identifier, loadedSkin.identifier);
            modified = true;
        }

        UUID localPlayerId = getLocalPlayerUuid();
        Identifier resolvedCape = resolveCape(loadedSkin, playerId.equals(localPlayerId));
        if (resolvedCape != null) {
            if (resolvedCape.equals(CAPE_NONE)) {
                cape = null;
                elytra = null;
            } else {
                cape = new ClientAsset.ResourceTexture(resolvedCape, resolvedCape);
                boolean isMojangCape = resolvedCape.getNamespace().equals("bedrockskins")
                    && resolvedCape.getPath().startsWith("capes/mojang/");
                elytra = isMojangCape
                    ? cape
                    : (original.elytra() != null
                        ? original.elytra()
                        : new ClientAsset.ResourceTexture(VANILLA_ELYTRA, VANILLA_ELYTRA));
            }
            modified = true;
        }

        return modified ? new PlayerSkin(body, cape, elytra, original.model(), original.secure()) : original;
    }

    public static SkinId getLocalCapeOverride() {
        return localCapeOverride;
    }

    public static Identifier getLocalAccountCapeOverride() {
        return localAccountCapeOverride;
    }

    public static void setLocalAccountCapeOverride(Identifier id) {
        localAccountCapeOverride = id;
    }

    public static void setLocalCapeOverride(SkinId id) {
        localCapeOverride = id;
        saveCurrentState();
    }

    public static void load() {
        UUID localUuid = getLocalPlayerUuid();
        if (localUuid == null) return;

        try {
            LocalSkinConfig state = StateManager.readState();
            String selected = state.selected();
            if (selected != null && !selected.isBlank()) playerSkins.put(localUuid, SkinId.parse(selected));
            else playerSkins.remove(localUuid);

            String cape = state.selectedCape();
            if (cape != null && !cape.isBlank()) localCapeOverride = SkinId.parse(cape);
            else localCapeOverride = null;
        } catch (Exception e) { LOGGER.error("SkinManager: load failed", e); }
    }

    public static void clearOtherPlayers() {
        UUID localUuid = getLocalPlayerUuid();
        java.util.List<SkinId> toRelease = new java.util.ArrayList<>();
        playerSkins.entrySet().removeIf(entry -> {
            if (!entry.getKey().equals(localUuid)) {
                toRelease.add(entry.getValue());
                return true;
            }
            return false;
        });
        for (SkinId id : toRelease) {
            releaseIfUnused(id);
        }
    }

    public static SkinId getLocalSelectedKey() {
        UUID localUuid = getLocalPlayerUuid();
        if (localUuid != null && playerSkins.containsKey(localUuid)) return playerSkins.get(localUuid);
        
        try {
            String selected = StateManager.readState().selected();
            return (selected == null || selected.isBlank()) ? null : SkinId.parse(selected);
        } catch (Exception e) {
            LOGGER.error("SkinManager: failed to read local selected skin from state", e);
            return null;
        }
    }

    public static void setSkin(UUID uuid, SkinId id) {
        SkinId previous = playerSkins.put(uuid, id);
        if (!Objects.equals(previous, id)) releaseIfUnused(previous);
        
        if (uuid.equals(getLocalPlayerUuid())) {
            saveCurrentState();
        }
    }

    public static void setLocalSkin(SkinId id) {
        UUID localUuid = getLocalPlayerUuid();
        if (localUuid == null) {
            StateManager.updateSelection(id != null ? id.toString() : null, null);
            return;
        }

        SkinId previous = playerSkins.put(localUuid, id);
        localCapeOverride = null;
        if (!Objects.equals(previous, id)) releaseIfUnused(previous);
        saveCurrentState();
    }

    public static void setPreviewSkin(UUID uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        SkinId previous = previewSkins.put(uuid, id);
        if (!Objects.equals(previous, id)) releaseIfUnused(previous);
    }

    public static void resetPreviewSkin(UUID uuid) {
        releaseIfUnused(previewSkins.remove(uuid));
    }

    public static SkinId getSkin(UUID uuid) {
        if (uuid == null) return null;
        SkinId preview = previewSkins.get(uuid);
        return preview != null ? preview : playerSkins.get(uuid);
    }

    public static void resetSkin(UUID uuid) {
        SkinId previous = playerSkins.remove(uuid);
        if (previous != null) {
            if (uuid.equals(getLocalPlayerUuid())) {
                saveCurrentState();
            }
            releaseIfUnused(previous);
        }
    }

    private static void saveCurrentState() {
        UUID localUuid = getLocalPlayerUuid();
        if (localUuid == null) return;
        try {
            SkinId activeSkin = playerSkins.get(localUuid);
            StateManager.updateSelection(
                activeSkin != null ? activeSkin.toString() : null,
                localCapeOverride != null ? localCapeOverride.toString() : null
            );
        } catch (Exception e) {
            LOGGER.error("SkinManager: failed to save state", e);
        }
    }

    private static UUID getLocalPlayerUuid() {
        var player = Minecraft.getInstance().player;
        return player != null ? player.getUUID() : null;
    }

    private static void releaseIfUnused(SkinId id) {
        if (id == null) return;
        if (!playerSkins.containsValue(id) && !previewSkins.containsValue(id)) SkinPackLoader.releaseSkinAssets(id);
    }
}
