package io.github.brandonitaly.bedrockskins.client.appearance.skin;

import io.github.brandonitaly.bedrockskins.client.persistence.LocalSkinConfig;
import io.github.brandonitaly.bedrockskins.client.persistence.StateManager;

import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
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
    private static final Map<UUID, SkinAssignment> skinAssignments = new HashMap<>();
    private static SkinId localCapeOverride = null;
    private static Identifier localAccountCapeOverride = null;
    public static final Identifier CAPE_NONE = Identifier.fromNamespaceAndPath("bedrockskins", "none");
    public static final SkinId CAPE_NONE_SKIN_ID = SkinId.of("none", "none");
    private static final Identifier VANILLA_ELYTRA = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");

    private static final class SkinAssignment {
        private SkinId selected;
        private SkinId preview;

        private SkinId effective() {
            return preview != null ? preview : selected;
        }
    }

    private static SkinAssignment assignment(UUID uuid) {
        return skinAssignments.computeIfAbsent(uuid, ignored -> new SkinAssignment());
    }

    private static void removeIfEmpty(UUID uuid, SkinAssignment value) {
        if (value.selected == null && value.preview == null) {
            skinAssignments.remove(uuid);
        }
    }

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

        var model = loadedSkin != null
            ? (MojangSkinManager.isSkinSlim(loadedSkin)
                ? net.minecraft.world.entity.player.PlayerModelType.SLIM
                : net.minecraft.world.entity.player.PlayerModelType.WIDE)
            : original.model();
        return modified ? new PlayerSkin(body, cape, elytra, model, original.secure()) : original;
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
            SkinAssignment assignment = assignment(localUuid);
            assignment.selected = selected != null && !selected.isBlank() ? SkinId.parse(selected) : null;
            removeIfEmpty(localUuid, assignment);

            String cape = state.selectedCape();
            if (cape != null && !cape.isBlank()) localCapeOverride = SkinId.parse(cape);
            else localCapeOverride = null;
        } catch (Exception e) { LOGGER.error("SkinManager: load failed", e); }
    }

    public static void clearOtherPlayers() {
        UUID localUuid = getLocalPlayerUuid();
        java.util.List<SkinId> toRelease = new java.util.ArrayList<>();
        skinAssignments.entrySet().removeIf(entry -> {
            SkinAssignment assignment = entry.getValue();
            if (!entry.getKey().equals(localUuid) && assignment.selected != null) {
                toRelease.add(assignment.selected);
                assignment.selected = null;
                return assignment.preview == null;
            }
            return false;
        });
        for (SkinId id : toRelease) {
            releaseIfUnused(id);
        }
    }

    public static SkinId getLocalSelectedKey() {
        UUID localUuid = getLocalPlayerUuid();
        if (localUuid != null) {
            SkinAssignment assignment = skinAssignments.get(localUuid);
            if (assignment != null && assignment.selected != null) return assignment.selected;
        }
        
        try {
            String selected = StateManager.readState().selected();
            return (selected == null || selected.isBlank()) ? null : SkinId.parse(selected);
        } catch (Exception e) {
            LOGGER.error("SkinManager: failed to read local selected skin from state", e);
            return null;
        }
    }

    public static void setSkin(UUID uuid, SkinId id) {
        SkinAssignment assignment = assignment(uuid);
        SkinId previous = assignment.selected;
        assignment.selected = id;
        removeIfEmpty(uuid, assignment);
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

        SkinAssignment assignment = assignment(localUuid);
        SkinId previous = assignment.selected;
        assignment.selected = id;
        removeIfEmpty(localUuid, assignment);
        localCapeOverride = null;
        if (!Objects.equals(previous, id)) releaseIfUnused(previous);
        saveCurrentState();
    }

    public static void setPreviewSkin(UUID uuid, String packName, String skinName) {
        SkinId id = SkinId.of(packName, skinName);
        SkinAssignment assignment = assignment(uuid);
        SkinId previous = assignment.preview;
        assignment.preview = id;
        if (!Objects.equals(previous, id)) releaseIfUnused(previous);
    }

    public static void resetPreviewSkin(UUID uuid) {
        SkinAssignment assignment = skinAssignments.get(uuid);
        if (assignment == null) return;
        SkinId previous = assignment.preview;
        assignment.preview = null;
        removeIfEmpty(uuid, assignment);
        releaseIfUnused(previous);
    }

    public static SkinId getSkin(UUID uuid) {
        if (uuid == null) return null;
        SkinAssignment assignment = skinAssignments.get(uuid);
        return assignment != null ? assignment.effective() : null;
    }

    public static void resetSkin(UUID uuid) {
        SkinAssignment assignment = skinAssignments.get(uuid);
        if (assignment == null) return;
        SkinId previous = assignment.selected;
        assignment.selected = null;
        removeIfEmpty(uuid, assignment);
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
            SkinAssignment assignment = skinAssignments.get(localUuid);
            SkinId activeSkin = assignment != null ? assignment.selected : null;
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
        boolean used = skinAssignments.values().stream()
            .anyMatch(assignment -> id.equals(assignment.selected) || id.equals(assignment.preview));
        if (!used) SkinPackLoader.releaseSkinAssets(id);
    }
}
