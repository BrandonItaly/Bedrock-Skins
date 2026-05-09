package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
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
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;
import wily.legacy.skins.api.ui.LegacySkinUi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class BedrockSkinUiAdapter implements LegacySkinUi.Adapter {
    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";
    private static final String REMOTE_PACK_ID = "skinpack.Remote";
    private static final String AUTO_SELECTED_SKIN_ID = SkinId.of("Standard", GuiSkinUtils.AUTO_SELECTED_INTERNAL_NAME).toString();
    private static final int MAX_PREVIEWS = 96;

    private final Map<String, PreviewState> previews = new LinkedHashMap<>(16, 0.75F, true);

    @Override
    public List<LegacySkinUi.Pack> packs() {
        ensureDataLoaded();

        Map<String, List<LoadedSkin>> skinsByPack = buildSkinCache();
        ArrayList<LegacySkinUi.Pack> packs = new ArrayList<>();

        List<LoadedSkin> favorites = skinsByPack.get(FAVORITES_PACK_ID);
        if (favorites != null && !favorites.isEmpty()) packs.add(createPack(FAVORITES_PACK_ID, favorites));

        ArrayList<String> sortedPackIds = new ArrayList<>(skinsByPack.keySet());
        sortedPackIds.remove(FAVORITES_PACK_ID);
        sortedPackIds.remove(REMOTE_PACK_ID);
        sortedPackIds.sort(buildPackComparator(skinsByPack));

        for (String packId : sortedPackIds) {
            List<LoadedSkin> skins = skinsByPack.get(packId);
            if (skins != null && !skins.isEmpty()) packs.add(createPack(packId, skins));
        }

        return List.copyOf(packs);
    }

    @Override
    public boolean supportsFavorites() {
        return true;
    }

    @Override
    public @Nullable String selectedSkinId() {
        SkinId selected = SkinManager.getLocalSelectedKey();
        return selected == null ? AUTO_SELECTED_SKIN_ID : selected.toString();
    }

    @Override
    public boolean isFavorite(String skinId) {
        return FavoritesManager.isFavorite(resolveSkin(skinId));
    }

    @Override
    public void selectSkin(String skinId) {
        try {
            LoadedSkin skin = resolveSkin(skinId);
            if (skin != null) GuiSkinUtils.applySelectedSkin(Minecraft.getInstance(), skin);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void toggleFavorite(String skinId) {
        LoadedSkin skin = resolveSkin(skinId);
        if (skin == null) return;
        if (FavoritesManager.isFavorite(skin)) FavoritesManager.removeFavorite(skin);
        else FavoritesManager.addFavorite(skin);
    }

    @Override
    public void prewarmPreview(String skinId) {
        LoadedSkin skin = resolveSkin(skinId);
        if (skin != null && !GuiSkinUtils.isAutoSelectedSkin(skin)) SkinPackLoader.registerTextureFor(skin.skinId);
    }

    @Override
    public void renderPreview(LegacySkinUi.PreviewContext context) {
        LoadedSkin skin = resolveSkin(context.skinId());
        PreviewState state = previewFor(context.skinId(), Minecraft.getInstance());
        if (state == null) return;

        if (!context.skinId().equals(state.skinId)) applyPreviewSkin(state, skin, context.skinId());

        PreviewPlayer player = state.player;
        boolean crouching = context.crouching();
        float attackTime = context.attackTime();

        player.setShiftKeyDown(crouching);
        player.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
        player.tickCount = (int) (Util.getMillis() / 50L);

        if (attackTime > 0.0F) {
            player.swinging = true;
            player.swingingArm = InteractionHand.MAIN_HAND;
            player.swingTime = (int) (attackTime * 6.0F);
            player.attackAnim = attackTime;
        } else {
            player.swinging = false;
            player.attackAnim = 0.0F;
        }

        GuiUtils.renderEntityInRect(
                context.graphics(),
                player,
                context.yaw(),
                crouching ? 5.0F : 0.0F,
                context.left(),
                context.top(),
                context.right(),
                context.bottom(),
                165
        );
    }

    @Override
    public int version() {
        ensureDataLoaded();
        return Objects.hash(
                SkinPackLoader.loadedSkins.size(),
                SkinPackLoader.packTypesByPackId.size(),
                SkinPackLoader.packIconsByPackId.hashCode(),
                SkinPackLoader.packOrder.hashCode(),
                FavoritesManager.getFavoriteKeys().hashCode(),
                SkinManager.getLocalSelectedKey()
        );
    }

    private static void ensureDataLoaded() {
        if (SkinPackLoader.loadedSkins.isEmpty()) SkinPackLoader.loadPacks();
    }

    private static Map<String, List<LoadedSkin>> buildSkinCache() {
        LinkedHashMap<String, List<LoadedSkin>> skinsByPack = new LinkedHashMap<>();
        synchronized (SkinPackLoader.loadedSkins) {
            for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
                if (skin == null || REMOTE_PACK_ID.equals(skin.packId)) continue;
                skinsByPack.computeIfAbsent(skin.packId, ignored -> new ArrayList<>()).add(skin);
            }
        }

        List<LoadedSkin> standardSkins = skinsByPack.get(GuiSkinUtils.STANDARD_PACK_ID);
        if (standardSkins != null && !standardSkins.isEmpty()) {
            skinsByPack.put(GuiSkinUtils.STANDARD_PACK_ID, GuiSkinUtils.withAutoSelectedStandardFirst(standardSkins));
        }

        ArrayList<LoadedSkin> favorites = new ArrayList<>();
        for (String favoriteKey : FavoritesManager.getFavoriteKeys()) {
            LoadedSkin favorite = resolveSkin(favoriteKey);
            if (favorite != null) favorites.add(favorite);
        }
        if (!favorites.isEmpty()) skinsByPack.put(FAVORITES_PACK_ID, favorites);
        else skinsByPack.remove(FAVORITES_PACK_ID);

        return skinsByPack;
    }

    private static Comparator<String> buildPackComparator(Map<String, List<LoadedSkin>> skinsByPack) {
        return PackSortUtil.buildPackComparator(BedrockSkinsConfig.getPackSortOrder(), packId -> {
            List<LoadedSkin> skins = skinsByPack.get(packId);
            LoadedSkin firstSkin = skins == null || skins.isEmpty() ? null : skins.getFirst();
            return GuiSkinUtils.getPackDisplayName(packId, firstSkin);
        });
    }

    private static LegacySkinUi.Pack createPack(String packId, List<LoadedSkin> skins) {
        ArrayList<LegacySkinUi.Skin> uiSkins = new ArrayList<>(skins.size());
        for (LoadedSkin skin : skins) {
            if (skin == null) continue;
            uiSkins.add(new LegacySkinUi.Skin(skinIdFor(skin), GuiSkinUtils.getSkinDisplayNameText(skin)));
        }

        LoadedSkin firstSkin = skins.getFirst();
        String packType = SkinPackLoader.packTypesByPackId.get(packId);
        String subtitle = packType == null || packType.isBlank()
                ? null
                : net.minecraft.network.chat.Component.translatable("bedrockskins.packType." + packType).getString();

        return new LegacySkinUi.Pack(
                packId,
                GuiSkinUtils.getPackDisplayName(packId, firstSkin),
                subtitle,
                SkinPackLoader.packIconsByPackId.get(packId),
                List.copyOf(uiSkins)
        );
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

    private static @Nullable LoadedSkin resolveSkin(@Nullable String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        if (AUTO_SELECTED_SKIN_ID.equals(skinId) || GuiSkinUtils.isAutoSelectedSkinId(SkinId.parse(skinId))) {
            List<LoadedSkin> standardSkins = null;
            synchronized (SkinPackLoader.loadedSkins) {
                for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
                    if (skin != null && GuiSkinUtils.STANDARD_PACK_ID.equals(skin.packId)) {
                        if (standardSkins == null) standardSkins = new ArrayList<>();
                        standardSkins.add(skin);
                    }
                }
            }
            return GuiSkinUtils.resolveAutoSelectedFromStandard(standardSkins);
        }
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
