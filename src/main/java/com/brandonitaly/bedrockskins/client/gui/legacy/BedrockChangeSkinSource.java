package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if legacy4j {
/*import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.gui.GuiSkinUtils;
import com.brandonitaly.bedrockskins.client.gui.GuiUtils;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.PackSortUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import wily.legacy.skins.client.gui.screen().ChangeSkinScreenSource;
import wily.legacy.skins.skin.SkinEntry;
import wily.legacy.skins.skin.SkinPack;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.*;

public final class BedrockChangeSkinSource implements ChangeSkinScreenSource {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ChangeSkinScreenSource legacySource = ChangeSkinScreenSource.Default.INSTANCE;

    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";
    private static final String REMOTE_PACK_ID = "skinpack.Remote";
    private static final String STANDARD_PACK_ID = "skinpack.Standard";
    private static final String LEGACY_PACK_ID = "skinpack.LegacySkinPack";
    private static final int MAX_PREVIEWS = 96;
    private static final Identifier DEFAULT_PACK_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "skin_packs/vanilla/pack_icon.png");

    private final Map<String, PreviewState> previews = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PreviewState> eldest) {
            if (size() > MAX_PREVIEWS) {
                GuiSkinUtils.cleanupPreview(eldest.getValue().uuid);
                return true;
            }
            return false;
        }
    };

    private final LinkedHashMap<String, SkinPack> cachedBedrockPacks = new LinkedHashMap<>();
    private final Map<String, SkinEntry> cachedBedrockSkins = new HashMap<>();
    private int lastVersionHash = -1;

    @Override
    public Map<String, SkinPack> packs() {
        updateCacheIfNeeded();
        LinkedHashMap<String, SkinPack> allPacks = new LinkedHashMap<>();

        SkinPack bedrockFavorites = cachedBedrockPacks.get(FAVORITES_PACK_ID);
        Map<String, SkinPack> legacyPacks = legacySource.packs();

        SkinPack legacyFavorites = legacyPacks.values().stream()
                .filter(pack -> SkinIdUtil.isFavouritesPack(pack.id()))
                .findFirst().orElse(null);

        SkinPack mergedFavorites = null;
        if (bedrockFavorites != null && legacyFavorites != null) {
            List<SkinEntry> combinedSkins = new ArrayList<>(legacyFavorites.skins());
            combinedSkins.addAll(bedrockFavorites.skins());

            mergedFavorites = new SkinPack(
                    legacyFavorites.id(), legacyFavorites.name(), legacyFavorites.author(),
                    legacyFavorites.type(), legacyFavorites.icon(), combinedSkins,
                    legacyFavorites.editable(), legacyFavorites.sortIndex(),
                    legacyFavorites.hasSort(), legacyFavorites.sortSubIndex()
            );
        } else if (legacyFavorites != null) {
            mergedFavorites = legacyFavorites;
        } else if (bedrockFavorites != null) {
            mergedFavorites = bedrockFavorites;
        }

        if (mergedFavorites != null) {
            allPacks.put(mergedFavorites.id(), mergedFavorites);
        }

        cachedBedrockPacks.forEach((id, pack) -> {
            if (!FAVORITES_PACK_ID.equals(id) && !isHiddenPackId(id)) allPacks.put(id, pack);
        });

        legacyPacks.forEach((id, pack) -> {
            if (!SkinIdUtil.isFavouritesPack(id) && !isHiddenPackId(id)) allPacks.put(id, pack);
        });

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

    // @Override
    // public @Nullable String skinTheme(SkinEntry skin) {
    //     if (skin == null) return null;
    //     String id = skin.id();
    //     if (isBedrockSkin(id)) {
    //         LoadedSkin loaded = resolveBedrockSkin(id);
    //         if (loaded == null) return null;
    //         return GuiSkinUtils.getSkinDescriptionText(loaded).orElse(null);
    //     }
    //     return legacySource.skinTheme(skin);
    // }

    @Override
    public @Nullable SkinEntry skin(String id) {
        updateCacheIfNeeded();
        SkinEntry bedrockEntry = cachedBedrockSkins.get(id);
        return bedrockEntry != null ? bedrockEntry : legacySource.skin(id);
    }

    @Override
    public @Nullable String currentAppliedSkinId() {
        SkinId selected = SkinManager.getLocalSelectedKey();
        return selected != null ? selected.toString() : legacySource.currentAppliedSkinId();
    }

    @Override
    public boolean supportsFavorites() {
        return true;
    }

    @Override
    public boolean isFavorite(String skinId) {
        LoadedSkin skin = resolveBedrockSkin(skinId);
        return skin != null ? FavoritesManager.isFavorite(skin) : legacySource.isFavorite(skinId);
    }

    @Override
    public void toggleFavorite(String skinId) {
        LoadedSkin skin = resolveBedrockSkin(skinId);
        if (skin != null) {
            if (FavoritesManager.isFavorite(skin)) FavoritesManager.removeFavorite(skin);
            else FavoritesManager.addFavorite(skin);
        } else {
            legacySource.toggleFavorite(skinId);
        }
    }

    @Override
    public void selectSkin(@Nullable String packId, String skinId) {
        LoadedSkin skin = resolveBedrockSkin(skinId);
        if (skin != null) {
            try {
                legacySource.selectSkin(null, "");
                GuiSkinUtils.applySelectedSkin(Minecraft.getInstance(), skin);
            } catch (Exception exception) {
                LOGGER.error("Failed to select Bedrock skin through Legacy4J source {}", skin.skinId, exception);
            }
        } else {
            GuiSkinUtils.resetSelectedSkin(Minecraft.getInstance());
            legacySource.selectSkin(packId, skinId);
        }
    }

    @Override
    public void prewarmPreview(String skinId) {
        LoadedSkin skin = resolveBedrockSkin(skinId);
        if (skin != null) {
            SkinPackLoader.registerTextureFor(skin.skinId);
        } else {
            legacySource.prewarmPreview(skinId);
        }
    }

    @Override
    public boolean renderPreview(GuiGraphicsExtractor graphics, String skinId, float yawOffset, boolean crouchPose, float attackTime, float partialTick, int left, int top, int right, int bottom) {
        LoadedSkin skin = resolveBedrockSkin(skinId);
        if (skin != null) {
            PreviewState state = previewFor(skinId, Minecraft.getInstance());
            if (state == null) return true;

            if (!skinId.equals(state.skinId)) applyPreviewSkin(state, skin, skinId);

            AvatarRenderState renderState = new AvatarRenderState();
            float yaw = Mth.wrapDegrees(yawOffset + 180.0F);

            GuiUtils.setupAvatarRenderState(renderState, state.player, skin.skinId, yaw, crouchPose, attackTime);
            
            renderState.yRot = yaw;
            renderState.xRot = 0.0F;

            int baseHeight = bottom - top;
            int legacySize = Math.max(20, Math.min((int) (baseHeight / 2.75F), 165));
            Vector3f translate = new Vector3f(0.0F, 1.8F / 2.0F + (crouchPose ? -0.125F : 0.0F), 0.0F);
            Quaternionf bodyRotation = new Quaternionf().rotationZ((float) Math.PI);
            Quaternionf cameraRotation = new Quaternionf();

            //~ if <26.1 '.entity' -> '.submitEntityRenderState' {
            graphics.entity(
                    renderState, legacySize, translate, bodyRotation, cameraRotation,
                    left, top - baseHeight, right, bottom + baseHeight
            );
            //~}

            return true;
        }

        return legacySource.renderPreview(graphics, skinId, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom);
    }

    @Override
    public @Nullable Component packSubtitle(SkinPack pack) {
        if (cachedBedrockPacks.containsKey(pack.id())) {
            String packType = SkinPackLoader.packTypesByPackId.get(pack.id());
            return packType == null || packType.isBlank() ? null : Component.translatable("bedrockskins.packType." + packType);
        }
        return legacySource.packSubtitle(pack);
    }

    @Override
    public int version() {
        if (SkinPackLoader.loadedSkins.isEmpty()) SkinPackLoader.loadPacks();
        
        int bedrockHash = Objects.hash(
                SkinPackLoader.loadedSkins.size(),
                SkinPackLoader.packTypesByPackId.size(),
                SkinPackLoader.packIconsByPackId.hashCode(),
                SkinPackLoader.packOrder.hashCode(),
                FavoritesManager.getFavoriteKeys().hashCode(),
                SkinManager.getLocalSelectedKey()
        );
        return bedrockHash * 31 + legacySource.version();
    }

    @Override
    public @Nullable String initialPackId(@Nullable String selectedSkinId) {
        LoadedSkin skin = resolveBedrockSkin(selectedSkinId);
        return skin != null ? skin.packId : legacySource.initialPackId(selectedSkinId);
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

    private void updateCacheIfNeeded() {
        int currentVersion = version();
        if (currentVersion == lastVersionHash) return;
        lastVersionHash = currentVersion;

        cachedBedrockPacks.clear();
        cachedBedrockSkins.clear();

        Map<String, List<LoadedSkin>> skinsByPack = buildSkinCache();

        int sortIndex = 0;
        List<LoadedSkin> favorites = skinsByPack.remove(FAVORITES_PACK_ID);
        if (favorites != null && !favorites.isEmpty()) {
            cachedBedrockPacks.put(FAVORITES_PACK_ID, createNativePack(FAVORITES_PACK_ID, favorites, sortIndex++));
        }

        List<String> sortedPackIds = skinsByPack.keySet().stream()
                .filter(id -> !isHiddenPackId(id))
                .sorted(PackSortUtil.buildPackComparator(
                        BedrockSkinsConfig.getPackSortOrder(),
                        packId -> GuiSkinUtils.getPackDisplayName(packId, skinsByPack.get(packId).getFirst())
                )).toList();

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
            String skinId = skin.skinId.toString();

            SkinEntry entry = new SkinEntry(
                    skinId, packId, GuiSkinUtils.getSkinDisplayNameText(skin),
                    null, null, null, false, order++, false
            );
            entries.add(entry);
            cachedBedrockSkins.put(skinId, entry);
        }

        LoadedSkin firstSkin = skins.isEmpty() ? null : skins.getFirst();
        Identifier icon = SkinPackLoader.packIconsByPackId.getOrDefault(packId, DEFAULT_PACK_ICON);
        
        return new SkinPack(
                packId, GuiSkinUtils.getPackDisplayName(packId, firstSkin), null,
                SkinPackLoader.packTypesByPackId.get(packId), icon, entries,
                false, sortIndex, true, 0
        );
    }

    private static Map<String, List<LoadedSkin>> buildSkinCache() {
        Map<String, List<LoadedSkin>> skinsByPack = new HashMap<>();
        
        synchronized (SkinPackLoader.loadedSkins) {
            SkinPackLoader.loadedSkins.values().forEach(skin -> {
                if (skin != null && !REMOTE_PACK_ID.equals(skin.packId) && !isHiddenPackId(skin.packId)) {
                    skinsByPack.computeIfAbsent(skin.packId, k -> new ArrayList<>()).add(skin);
                }
            });
        }

        List<LoadedSkin> favorites = FavoritesManager.getFavoriteKeys().stream()
                .map(BedrockChangeSkinSource::resolveBedrockSkin)
                .filter(Objects::nonNull)
                .toList();

        if (!favorites.isEmpty()) skinsByPack.put(FAVORITES_PACK_ID, favorites);

        return skinsByPack;
    }

    private static boolean isHiddenPackId(String packId) {
        return STANDARD_PACK_ID.equals(packId) || LEGACY_PACK_ID.equals(packId);
    }

    private PreviewState previewFor(String skinId, Minecraft minecraft) {
        if (skinId == null || skinId.isBlank()) return null;

        return previews.computeIfAbsent(skinId, id -> {
            UUID uuid = UUID.randomUUID();
            String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
            return new PreviewState(uuid, PreviewPlayer.PreviewPlayerPool.get(new GameProfile(uuid, name)));
        });
    }

    private static void applyPreviewSkin(PreviewState state, LoadedSkin skin, String skinId) {
        GuiSkinUtils.applyLoadedSkinPreview(state.player, state.uuid, skin);
        state.skinId = skinId;
    }

    private static @Nullable LoadedSkin resolveBedrockSkin(@Nullable String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        return SkinPackLoader.getLoadedSkin(SkinId.parse(skinId));
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
*///? }