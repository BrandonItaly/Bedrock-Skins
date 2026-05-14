package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if legacy4j {
import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.gui.GuiSkinUtils;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wily.legacy.skins.client.screen.ChangeSkinScreenSource;
import wily.legacy.skins.skin.SkinEntry;
import wily.legacy.skins.skin.SkinPack;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.*;

public final class BedrockChangeSkinSource implements ChangeSkinScreenSource {
    private static final ChangeSkinScreenSource legacySource = ChangeSkinScreenSource.Default.INSTANCE;

    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";
    private static final String REMOTE_PACK_ID = "skinpack.Remote";
    private static final String STANDARD_PACK_ID = "skinpack.Standard";
    private static final String LEGACY_PACK_ID = "skinpack.LegacySkinPack";
    private static final String AUTO_SELECTED_SKIN_ID = SkinId.of("Standard", GuiSkinUtils.AUTO_SELECTED_INTERNAL_NAME).toString();
    private static final int MAX_PREVIEWS = 96;
    private static final Identifier DEFAULT_PACK_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "skin_packs/vanilla/pack_icon.png");

    private final Map<String, PreviewState> previews = new LinkedHashMap<>(16, 0.75F, true);
    private @Nullable String lastAutoSelectedId;
    
    // Caches to avoid rebuilding Bedrock native structures every frame
    private final LinkedHashMap<String, SkinPack> cachedBedrockPacks = new LinkedHashMap<>();
    private final Map<String, SkinEntry> cachedBedrockSkins = new HashMap<>();
    private int lastVersionHash = -1;

    @Override
    public Map<String, SkinPack> packs() {
        updateCacheIfNeeded();
        LinkedHashMap<String, SkinPack> allPacks = new LinkedHashMap<>();

        SkinPack bedrockFavorites = cachedBedrockPacks.get(FAVORITES_PACK_ID);
        Map<String, SkinPack> legacyPacks = legacySource.packs();

        SkinPack legacyFavorites = null;
        for (SkinPack pack : legacyPacks.values()) {
            if (SkinIdUtil.isFavouritesPack(pack.id())) {
                legacyFavorites = pack;
                break;
            }
        }

        SkinPack mergedFavorites = null;
        if (bedrockFavorites != null && legacyFavorites != null) {
            List<SkinEntry> combinedSkins = new ArrayList<>(legacyFavorites.skins());
            combinedSkins.addAll(bedrockFavorites.skins());
            
            mergedFavorites = new SkinPack(
                    legacyFavorites.id(),
                    legacyFavorites.name(),
                    legacyFavorites.author(),
                    legacyFavorites.type(),
                    legacyFavorites.icon(),
                    combinedSkins,
                    legacyFavorites.editable(),
                    legacyFavorites.sortIndex(),
                    legacyFavorites.hasSort(),
                    legacyFavorites.sortSubIndex()
            );
        } else if (legacyFavorites != null) {
            mergedFavorites = legacyFavorites;
        } else if (bedrockFavorites != null) {
            mergedFavorites = bedrockFavorites;
        }

        if (mergedFavorites != null) {
            allPacks.put(mergedFavorites.id(), mergedFavorites);
        }

        for (Map.Entry<String, SkinPack> entry : cachedBedrockPacks.entrySet()) {
            String packId = entry.getKey();
            if (!FAVORITES_PACK_ID.equals(packId) && !isHiddenPackId(packId)) {
                allPacks.put(packId, entry.getValue());
            }
        }

        for (Map.Entry<String, SkinPack> entry : legacyPacks.entrySet()) {
            SkinPack pack = entry.getValue();
            if (!SkinIdUtil.isFavouritesPack(pack.id()) && !isHiddenPackId(pack.id())) {
                allPacks.put(entry.getKey(), pack);
            }
        }

        return allPacks;
    }

    @Override
    public String packName(SkinPack pack) {
        return legacySource.packName(pack);
    }

    @Override
    public String skinName(SkinEntry skin) {
        return legacySource.skinName(skin);
    }

    @Override
    public @Nullable SkinEntry skin(String id) {
        updateCacheIfNeeded();
        SkinEntry bedrockEntry = cachedBedrockSkins.get(id);
        if (bedrockEntry != null) return bedrockEntry;
        
        return legacySource.skin(id);
    }

    @Override
    public @Nullable String currentAppliedSkinId() {
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) return selected.toString();
        
        String legacySelected = legacySource.currentAppliedSkinId();
        if (lastAutoSelectedId == null) {
            lastAutoSelectedId = SkinIdUtil.isBlankOrAutoSelect(legacySelected)
                    ? SkinIdUtil.AUTO_SELECT
                    : AUTO_SELECTED_SKIN_ID;
        }
        if (SkinIdUtil.isBlankOrAutoSelect(legacySelected)) return lastAutoSelectedId;
        return legacySelected;
    }

    @Override
    public boolean supportsFavorites() {
        return true;
    }

    @Override
    public boolean isFavorite(String skinId) {
        if (isBedrockSkin(skinId)) {
            return FavoritesManager.isFavorite(resolveBedrockSkin(skinId));
        }
        return legacySource.isFavorite(skinId);
    }

    @Override
    public void toggleFavorite(String skinId) {
        if (isBedrockSkin(skinId)) {
            LoadedSkin skin = resolveBedrockSkin(skinId);
            if (skin == null) return;
            if (FavoritesManager.isFavorite(skin)) FavoritesManager.removeFavorite(skin);
            else FavoritesManager.addFavorite(skin);
        } else {
            legacySource.toggleFavorite(skinId);
        }
    }

    @Override
    public void selectSkin(@Nullable String packId, String skinId) {
        if (AUTO_SELECTED_SKIN_ID.equals(skinId)) {
            lastAutoSelectedId = AUTO_SELECTED_SKIN_ID;
            GuiSkinUtils.resetSelectedSkin(Minecraft.getInstance());
            legacySource.selectSkin(null, "");
            return;
        }
        if (SkinIdUtil.isAutoSelect(skinId)) {
            lastAutoSelectedId = SkinIdUtil.AUTO_SELECT;
        }
        if (isBedrockSkin(skinId)) {
            try {
                legacySource.selectSkin(null, "");
                LoadedSkin skin = resolveBedrockSkin(skinId);
                if (skin != null) GuiSkinUtils.applySelectedSkin(Minecraft.getInstance(), skin);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else {
            GuiSkinUtils.resetSelectedSkin(Minecraft.getInstance());
            legacySource.selectSkin(packId, skinId);
        }
    }

    @Override
    public void prewarmPreview(String skinId) {
        if (isBedrockSkin(skinId)) {
            LoadedSkin skin = resolveBedrockSkin(skinId);
            if (skin != null && !GuiSkinUtils.isAutoSelectedSkin(skin)) {
                com.brandonitaly.bedrockskins.pack.SkinPackLoader.registerTextureFor(skin.skinId);
            }
        } else {
            legacySource.prewarmPreview(skinId);
        }
    }

    @Override
    public boolean renderPreview(GuiGraphicsExtractor graphics, String skinId, float yawOffset, boolean crouchPose, float attackTime, float partialTick, int left, int top, int right, int bottom) {
        if (isBedrockSkin(skinId)) {
            LoadedSkin skin = resolveBedrockSkin(skinId);
            PreviewState state = previewFor(skinId, Minecraft.getInstance());
            if (state == null) return true;

            if (!skinId.equals(state.skinId)) applyPreviewSkin(state, skin, skinId);
            
            AvatarRenderState renderState = new AvatarRenderState();

            if (renderState instanceof BedrockRenderStateAccessor accessor) {
                accessor.bedrockSkins$setBedrockSkinId(skin == null ? null : skin.skinId);
                accessor.bedrockSkins$setUniqueId(state.uuid);
            }

            float yaw = normalizeYaw(yawOffset + 180.0F);
            renderState.id = -0x5D011;
            renderState.entityType = EntityType.PLAYER;
            renderState.lightCoords = 15728880;
            renderState.boundingBoxHeight = 1.8F; 
            renderState.boundingBoxWidth = 0.6F;
            renderState.bodyRot = yaw;
            renderState.yRot = yaw;
            renderState.xRot = 0.0F;
            renderState.pose = crouchPose ? Pose.CROUCHING : Pose.STANDING;
            renderState.isBaby = false;
            renderState.scale = 1.0F;
            renderState.ageScale = 1.0F;
            renderState.showHat = true;
            renderState.showJacket = true;
            renderState.showLeftSleeve = true;
            renderState.showRightSleeve = true;
            renderState.showLeftPants = true;
            renderState.showRightPants = true;
            renderState.showCape = true; 
            renderState.attackArm = HumanoidArm.RIGHT;
            renderState.attackTime = attackTime;
            renderState.isCrouching = crouchPose;
            renderState.skin = state.player.getSkin(Minecraft.getInstance()); 

            // Calculate Legacy Scale & Translation layout
            int baseHeight = bottom - top;
            int legacySize = Math.max(20, Math.min((int) (baseHeight / 2.75F), 165));
            Vector3f translate = new Vector3f(0.0F, 1.8F / 2.0F + (crouchPose ? -0.125F : 0.0F), 0.0F);
            Quaternionf bodyRotation = new Quaternionf().rotationZ((float) Math.PI);
            Quaternionf cameraRotation = new Quaternionf();

            // Render!
            //~ if <26.0 '.entity' -> '.submitEntityRenderState' {
            graphics.entity(
                renderState,
                legacySize,
                translate,
                bodyRotation,
                cameraRotation,
                left,
                top - baseHeight,
                right,
                bottom + baseHeight
            );
            //~}

            return true;
        }

        return legacySource.renderPreview(graphics, skinId, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom);
    }

    private static float normalizeYaw(float yaw) {
        while (yaw < 0.0F) yaw += 360.0F;
        return (yaw + 180.0F) % 360.0F - 180.0F;
    }

    @Override
    public @Nullable Component packSubtitle(SkinPack pack) {
        if (cachedBedrockPacks.containsKey(pack.id())) {
            String packType = com.brandonitaly.bedrockskins.pack.SkinPackLoader.packTypesByPackId.get(pack.id());
            return packType == null || packType.isBlank() ? null : Component.translatable("bedrockskins.packType." + packType);
        }
        return legacySource.packSubtitle(pack);
    }

    @Override
    public int version() {
        ensureDataLoaded();
        int bedrockHash = Objects.hash(
                com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadedSkins.size(),
                com.brandonitaly.bedrockskins.pack.SkinPackLoader.packTypesByPackId.size(),
                com.brandonitaly.bedrockskins.pack.SkinPackLoader.packIconsByPackId.hashCode(),
                com.brandonitaly.bedrockskins.pack.SkinPackLoader.packOrder.hashCode(),
                FavoritesManager.getFavoriteKeys().hashCode(),
                SkinManager.getLocalSelectedKey()
        );
        return bedrockHash * 31 + legacySource.version();
    }

    @Override
    public @Nullable String initialPackId(@Nullable String selectedSkinId) {
        if (isBedrockSkin(selectedSkinId)) {
            LoadedSkin skin = resolveBedrockSkin(selectedSkinId);
            if (skin != null) return skin.packId;
        }
        return legacySource.initialPackId(selectedSkinId);
    }

    @Override
    public @Nullable String preferredDefaultPackId() {
        return legacySource.preferredDefaultPackId();
    }

    @Override
    public @Nullable String lastUsedPackId() {
        return legacySource.lastUsedPackId();
    }

    @Override
    public void requestFocus(@Nullable String packId, @Nullable String skinId) {
        legacySource.requestFocus(packId, skinId);
    }

    @Override
    public @Nullable String consumeRequestedFocusSkinId() {
        return legacySource.consumeRequestedFocusSkinId();
    }

    @Override
    public boolean supportsCustomPackOptions() {
        return legacySource.supportsCustomPackOptions();
    }

    @Override
    public boolean supportsAdvancedOptions() {
        return true; 
    }

    private boolean isBedrockSkin(String skinId) {
        return skinId != null && (AUTO_SELECTED_SKIN_ID.equals(skinId) || resolveBedrockSkin(skinId) != null);
    }

    private void updateCacheIfNeeded() {
        int currentVersion = version();
        if (currentVersion == lastVersionHash) return;
        lastVersionHash = currentVersion;

        cachedBedrockPacks.clear();
        cachedBedrockSkins.clear();

        Map<String, List<LoadedSkin>> skinsByPack = buildSkinCache();

        List<LoadedSkin> favorites = skinsByPack.get(FAVORITES_PACK_ID);
        int sortIndex = 0;
        if (favorites != null && !favorites.isEmpty()) {
            cachedBedrockPacks.put(FAVORITES_PACK_ID, createNativePack(FAVORITES_PACK_ID, favorites, sortIndex++));
        }

        ArrayList<String> sortedPackIds = new ArrayList<>(skinsByPack.keySet());
        sortedPackIds.remove(FAVORITES_PACK_ID);
        sortedPackIds.remove(REMOTE_PACK_ID);
        sortedPackIds.removeIf(BedrockChangeSkinSource::isHiddenPackId);
        sortedPackIds.sort(com.brandonitaly.bedrockskins.util.PackSortUtil.buildPackComparator(
                BedrockSkinsConfig.getPackSortOrder(),
                packId -> {
                    List<LoadedSkin> skins = skinsByPack.get(packId);
                    LoadedSkin firstSkin = skins == null || skins.isEmpty() ? null : skins.getFirst();
                    return GuiSkinUtils.getPackDisplayName(packId, firstSkin);
                }
        ));

        for (String packId : sortedPackIds) {
            List<LoadedSkin> skins = skinsByPack.get(packId);
            if (skins != null && !skins.isEmpty()) {
                cachedBedrockPacks.put(packId, createNativePack(packId, skins, sortIndex++));
            }
        }
    }

    private SkinPack createNativePack(String packId, List<LoadedSkin> skins, int sortIndex) {
        List<SkinEntry> entries = new ArrayList<>();
        int order = 1;
        for (LoadedSkin skin : skins) {
            if (skin == null) continue;
            String skinId = skinIdFor(skin);
            
            SkinEntry entry = new SkinEntry(
                    skinId,
                    packId,
                    GuiSkinUtils.getSkinDisplayNameText(skin),
                    null, null, null, false, order++, false
            );
            entries.add(entry);
            cachedBedrockSkins.put(skinId, entry);
        }

        LoadedSkin firstSkin = skins.isEmpty() ? null : skins.getFirst();
        return new SkinPack(
                packId,
                GuiSkinUtils.getPackDisplayName(packId, firstSkin),
                null,
                com.brandonitaly.bedrockskins.pack.SkinPackLoader.packTypesByPackId.get(packId),
                resolvePackIcon(packId),
                entries,
                false,
                sortIndex,
                true,
                0
        );
    }

    private static void ensureDataLoaded() {
        if (com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadedSkins.isEmpty()) {
            com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadPacks();
        }
    }

    private static Map<String, List<LoadedSkin>> buildSkinCache() {
        LinkedHashMap<String, List<LoadedSkin>> skinsByPack = new LinkedHashMap<>();
        synchronized (com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadedSkins) {
            for (LoadedSkin skin : com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadedSkins.values()) {
                if (skin == null || REMOTE_PACK_ID.equals(skin.packId) || isHiddenPackId(skin.packId)) continue;
                skinsByPack.computeIfAbsent(skin.packId, ignored -> new ArrayList<>()).add(skin);
            }
        }

        List<LoadedSkin> standardSkins = skinsByPack.get(GuiSkinUtils.STANDARD_PACK_ID);
        if (standardSkins != null && !standardSkins.isEmpty()) {
            skinsByPack.put(GuiSkinUtils.STANDARD_PACK_ID, GuiSkinUtils.withAutoSelectedStandardFirst(standardSkins));
        }

        ArrayList<LoadedSkin> favorites = new ArrayList<>();
        for (String favoriteKey : FavoritesManager.getFavoriteKeys()) {
            LoadedSkin favorite = resolveBedrockSkin(favoriteKey);
            if (favorite != null) favorites.add(favorite);
        }
        if (!favorites.isEmpty()) skinsByPack.put(FAVORITES_PACK_ID, favorites);
        else skinsByPack.remove(FAVORITES_PACK_ID);

        return skinsByPack;
    }

    private static Identifier resolvePackIcon(String packId) {
        Identifier icon = com.brandonitaly.bedrockskins.pack.SkinPackLoader.packIconsByPackId.get(packId);
        return icon != null ? icon : DEFAULT_PACK_ICON;
    }

    private static boolean isHiddenPackId(String packId) {
        return STANDARD_PACK_ID.equals(packId) || LEGACY_PACK_ID.equals(packId);
    }

    private PreviewState previewFor(String skinId, Minecraft minecraft) {
        if (skinId == null || skinId.isBlank()) return null;

        PreviewState state = previews.get(skinId);
        if (state != null) return state;

        UUID uuid = UUID.randomUUID();
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        PreviewPlayer player = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(uuid, name));
        state = new PreviewState(uuid, player);
        previews.put(skinId, state);
        trimPreviews();
        return state;
    }

    private static void applyPreviewSkin(PreviewState state, LoadedSkin skin, String skinId) {
        if (GuiSkinUtils.isAutoSelectedSkin(skin)) {
            GuiSkinUtils.applyAutoSelectedPreview(Minecraft.getInstance(), state.player, state.uuid);
        } else {
            GuiSkinUtils.applyLoadedSkinPreview(state.player, state.uuid, skin);
        }
        state.skinId = skinId;
    }

    private void trimPreviews() {
        while (previews.size() > MAX_PREVIEWS) {
            String key = previews.keySet().iterator().next();
            PreviewState state = previews.remove(key);
            if (state != null) GuiSkinUtils.cleanupPreview(state.uuid);
        }
    }

    private static String skinIdFor(LoadedSkin skin) {
        return GuiSkinUtils.isAutoSelectedSkin(skin) ? AUTO_SELECTED_SKIN_ID : skin.skinId.toString();
    }

    private static @Nullable LoadedSkin resolveBedrockSkin(@Nullable String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        if (AUTO_SELECTED_SKIN_ID.equals(skinId) || GuiSkinUtils.isAutoSelectedSkinId(SkinId.parse(skinId))) {
            List<LoadedSkin> standardSkins = null;
            synchronized (com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadedSkins) {
                for (LoadedSkin skin : com.brandonitaly.bedrockskins.pack.SkinPackLoader.loadedSkins.values()) {
                    if (skin != null && GuiSkinUtils.STANDARD_PACK_ID.equals(skin.packId)) {
                        if (standardSkins == null) standardSkins = new ArrayList<>();
                        standardSkins.add(skin);
                    }
                }
            }
            return GuiSkinUtils.resolveAutoSelectedFromStandard(standardSkins);
        }
        return com.brandonitaly.bedrockskins.pack.SkinPackLoader.getLoadedSkin(SkinId.parse(skinId));
    }

    private static final class PreviewState {
        private final UUID uuid;
        private final PreviewPlayer player;
        private String skinId = "";

        private PreviewState(UUID uuid, PreviewPlayer player) {
            this.uuid = uuid;
            this.player = player;
        }
    }
}
//? }