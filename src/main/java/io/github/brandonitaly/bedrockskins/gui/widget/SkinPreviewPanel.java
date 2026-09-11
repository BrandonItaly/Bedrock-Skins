package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.screen.*;

import io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import io.github.brandonitaly.bedrockskins.client.persistence.BedrockSkinsConfig;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.FavoritesManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.MojangSkinManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager;
import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager.MinecraftCape;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaTypeNames;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.ClientSkinSync;
import java.util.concurrent.CompletableFuture;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import io.github.brandonitaly.bedrockskins.util.ExternalAssetUtil;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;

public class SkinPreviewPanel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INFO_PANEL_HEIGHT = 58;
    private static final int FLOATING_CONTROL_GAP = 2;

    private final Minecraft minecraft;
    private final Font font;
    private final Runnable onFavoritesChanged;
    
    // State
    private int x, y, width, height;
    private FavoriteHeartButton favoriteButton;
    private Button selectButton;
    private Button replayEmoteButton;
    private SpriteIconButton previewButton;
    private SpriteIconButton uploadSkinButton;
    private boolean isUploadingSkin = false;
    private String uploadStatusMessage = null;
    private boolean uploadStatusIsError = false;
    private long uploadStatusTime = 0L;

    private LoadedSkin selectedSkin;
    private MinecraftCape selectedCape;
    private LoadedCosmetic selectedCosmetic;
    private LoadedEmote selectedEmote;
    private SkinId currentSkinId;
    private Screen parentScreen;
    private PreviewPlayer dummyPlayer;
    private UUID dummyUuid = UUID.randomUUID();
    private float rotationX = 0;
    private int lastMouseX = 0;
    private boolean isDraggingPreview = false;
    private int previewLeft, previewRight, previewTop, previewBottom;
    private int infoPanelY;

    public SkinPreviewPanel(Minecraft minecraft, Font font, Runnable onFavoritesChanged) {
        this.minecraft = minecraft;
        this.font = font;
        this.onFavoritesChanged = onFavoritesChanged;
    }
    
    public LoadedSkin getSelectedSkin() { return selectedSkin; }
    public MinecraftCape getSelectedCape() { return selectedCape; }
    public LoadedCosmetic getSelectedCosmetic() { return selectedCosmetic; }
    public LoadedEmote getSelectedEmote() { return selectedEmote; }

    public void playEmote(LoadedEmote emote) {
        if (dummyPlayer != null && emote != null) EmoteManager.play(dummyUuid, emote);
    }

    public void setSelectedEmote(LoadedEmote emote) {
        this.selectedEmote = emote;
        if (dummyPlayer != null) {
            if (emote == null) EmoteManager.stop(dummyUuid);
            else EmoteManager.play(dummyUuid, emote);
        }
        updateFavoriteButton();
    }

    public void setSelectedCosmetic(LoadedCosmetic cosmetic) {
        this.selectedCosmetic = cosmetic;
        if (dummyPlayer != null) {
            PersonaManager.setPreviewWithEquipped(dummyUuid, cosmetic);
            if (cosmetic != null) EmoteManager.playDressingRoom(dummyUuid, cosmetic.type);
        }
        updateFavoriteButton();
    }

    public void refreshSelectedCosmeticPreview() {
        if (dummyPlayer != null) PersonaManager.setPreviewWithEquipped(dummyUuid, selectedCosmetic);
        updateFavoriteButton();
    }

    public void setSelectedCape(MinecraftCape cape) {
        this.selectedCape = cape;
        if (dummyPlayer != null) {
            if (cape == null) {
                dummyPlayer.clearForcedCape();
                rotationX = 0.0F;
            } else {
                applySelectedCapePreview();
            }
        }
        updateFavoriteButton();
    }

    public void playCapeSelectionAnimation() {
        if (dummyPlayer != null) EmoteManager.playDressingRoom(dummyUuid, "persona_back");
    }

    public void init(int x, int y, int w, int h, Screen parentScreen, Consumer<AbstractWidget> widgetAdder) {
        this.parentScreen = parentScreen;
        selectButton = Button.builder(Component.translatable("bedrockskins.button.equip"), b -> applySkin()).bounds(0, 0, 10, 20).build();
        widgetAdder.accept(selectButton);

        replayEmoteButton = Button.builder(Component.translatable("bedrockskins.button.play_again"),
            b -> replaySelectedEmote()).bounds(0, 0, 10, 20).build();
        replayEmoteButton.visible = false;
        widgetAdder.accept(replayEmoteButton);

        favoriteButton = new FavoriteHeartButton(20, BedrockSkinsSprites.HEART_CONTAINER, BedrockSkinsSprites.HEART_FULL, b -> toggleFavorite());
        widgetAdder.accept(favoriteButton.getButton());

        previewButton = SpriteIconButton.builder(Component.empty(), b -> openFullScreenPreview(), true)
            .size(20, 20).sprite(BedrockSkinsSprites.PREVIEW_ICON, 16, 16).build();
        previewButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.preview")));
        widgetAdder.accept(previewButton);

        uploadSkinButton = SpriteIconButton.builder(Component.empty(), b -> uploadSkinToMojang(), true)
            .size(20, 20)
            .sprite(BedrockSkinsSprites.UPLOAD_ICON, 16, 16)
            .build();
        uploadSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.upload_skin")));
        widgetAdder.accept(uploadSkinButton);
        
        reposition(x, y, w, h);
        initPreviewState();
    }

    public void reposition(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.width = w; this.height = h;

        infoPanelY = Math.max(y, y + h - INFO_PANEL_HEIGHT);
        previewLeft = x;
        previewRight = x + w;
        previewTop = y + 4;
        previewBottom = Math.max(previewTop + 20, infoPanelY - 26);

        int btnH = 20;
        int actionX = x + 8;
        int actionW = Math.max(10, w - 16);
        int actionY = infoPanelY + INFO_PANEL_HEIGHT - btnH - 8;
        int floatingY = infoPanelY - btnH - FLOATING_CONTROL_GAP;

        if (selectButton != null) {
            selectButton.setX(actionX); selectButton.setY(actionY); selectButton.setWidth(actionW);
        }
        if (replayEmoteButton != null) {
            replayEmoteButton.setX(actionX);
            replayEmoteButton.setY(actionY);
            replayEmoteButton.setWidth(actionW);
        }
        if (favoriteButton != null) {
            favoriteButton.getButton().setX(x); favoriteButton.getButton().setY(floatingY);
        }
        if (previewButton != null) {
            previewButton.setX(x + w - 22);
            previewButton.setY(floatingY);
        }
        if (uploadSkinButton != null) {
            uploadSkinButton.setX(x + w - 44);
            uploadSkinButton.setY(floatingY);
        }
    }

    public int floatingControlX() {
        return x;
    }

    public int floatingControlY() {
        return infoPanelY - 20 - FLOATING_CONTROL_GAP;
    }

    private void openFullScreenPreview() {
        if (parentScreen == null) return;
        minecraft.gui.setScreen(new FullScreenPreviewScreen(parentScreen, selectedSkin,
            selectedCosmetic, selectedCape, selectedEmote, rotationX));
    }

    /** Rebuilds the transient preview entity after a child screen has removed this screen. */
    public void restoreAfterFullScreen(float restoredRotation) {
        initPreviewState();
        this.rotationX = restoredRotation;
    }

    public void initPreviewState() {
        if (this.selectedSkin != null) {
            updatePreviewModel(this.dummyUuid,
                MinecraftAccountSkin.is(this.selectedSkin) ? null : this.selectedSkin.skinId);
            updateFavoriteButton();
            return;
        }

        SkinId currentKey = SkinManager.getLocalSelectedKey();
        if (currentKey != null) {
            this.dummyUuid = UUID.randomUUID();
            this.currentSkinId = currentKey;
            this.selectedSkin = SkinPackLoader.getLoadedSkin(currentKey);
            updatePreviewModel(dummyUuid, currentKey);
        } else {
            this.currentSkinId = null;
            updatePreviewModel(dummyUuid, null);
        }
        updateFavoriteButton();
    }

    public void setSelectedSkin(LoadedSkin skin) {
        this.selectedSkin = skin;
        this.currentSkinId = skin != null && !MinecraftAccountSkin.is(skin) ? skin.skinId : null;
        updateFavoriteButton();
        if (skin != null) updatePreviewModel(dummyUuid,
            MinecraftAccountSkin.is(skin) ? null : skin.skinId);
    }

    private void updatePreviewModel(UUID uuid, SkinId skinId) {
        if (!this.dummyUuid.equals(uuid)) {
            GuiSkinUtils.cleanupPreview(this.dummyUuid);
            PersonaManager.clearPreview(this.dummyUuid);
            EmoteManager.stop(this.dummyUuid);
        }
        this.dummyUuid = uuid;
        
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        dummyPlayer = new PreviewPlayer(new GameProfile(uuid, name));

        if (skinId == null) {
            applyAutoSelectedSkinBehavior();
        } else {
            GuiSkinUtils.applyLoadedSkinPreview(dummyPlayer, uuid, selectedSkin, false);
        }
        restorePreviewAppearance();
    }

    private void restorePreviewAppearance() {
        if (dummyPlayer == null) return;

        if (selectedCosmetic != null) PersonaManager.setPreviewWithEquipped(dummyUuid, selectedCosmetic);
        else PersonaManager.setPreviewFromLocal(dummyUuid);
        applySelectedCapePreview();

        if (selectedEmote != null) {
            EmoteManager.play(dummyUuid, selectedEmote);
        } else if (selectedCosmetic != null
                && parentScreen instanceof SkinSelectionScreen screen
                && screen.getActiveTab() == AppearanceTab.COSMETICS) {
            EmoteManager.playDressingRoom(dummyUuid, selectedCosmetic.type);
        } else if (selectedCape != null
                && parentScreen instanceof SkinSelectionScreen screen
                && screen.getActiveTab() == AppearanceTab.CAPES) {
            EmoteManager.playDressingRoom(dummyUuid, "persona_back");
        }
    }

    private void applySelectedCapePreview() {
        if (dummyPlayer == null || selectedCape == null) return;
        if ("none".equals(selectedCape.id)) {
            if (parentScreen instanceof SkinSelectionScreen selectionScreen
                    && "skinpack".equals(selectionScreen.getSelectedCapesCategory())) {
                dummyPlayer.clearForcedCape();
            } else {
                dummyPlayer.setForcedCape(null);
            }
            rotationX = 0.0F;
        } else {
            dummyPlayer.setForcedCape(selectedCape.textureIdentifier);
            rotationX = 60.0F;
        }
    }

    private void applyAutoSelectedSkinBehavior() {
        GuiSkinUtils.applyAutoSelectedPreview(minecraft, dummyPlayer, dummyUuid);
    }

    private void applySkin() {
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == AppearanceTab.COSMETICS) {
            applyCosmetic();
            return;
        }
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == AppearanceTab.CAPES) {
            applyCape();
            return;
        }
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == AppearanceTab.EMOTES) {
            applyEmote();
            return;
        }

        if (selectedSkin == null) return;
        if (ImportSkinAction.is(selectedSkin)) {
            if (parentScreen instanceof SkinSelectionScreen selectionScreen) {
                minecraft.gui.setScreen(new ImportSkinChoiceScreen(
                    selectionScreen, MinecraftAccountSkin.PACK_ID));
            }
            return;
        }
        if (MinecraftAccountSkin.is(selectedSkin)) {
            currentSkinId = null;
            GuiSkinUtils.resetSelectedSkin(minecraft);
            updatePreviewModel(dummyUuid, null);
            updateFavoriteButton();
            return;
        }
        SkinManager.setSkin(dummyUuid, selectedSkin.skinId);
        try {
            GuiSkinUtils.applySelectedSkin(minecraft, selectedSkin);
        } catch (Exception e) {
            LOGGER.error("Failed to apply selected skin", e);
        }
        currentSkinId = selectedSkin.skinId;
        updateFavoriteButton();

    }

    private void applyCosmetic() {
        if (selectedCosmetic == null) return;
        PersonaManager.toggleLocal(selectedCosmetic);
        PersonaManager.setPreviewFromLocal(dummyUuid);
        updateFavoriteButton();
    }

    private void applyEmote() {
        if (selectedEmote == null) return;
        if (parentScreen instanceof SkinSelectionScreen selectionScreen) {
            selectionScreen.openEmoteSlotPicker(selectedEmote);
        }
    }

    private void replaySelectedEmote() {
        if (selectedEmote != null && dummyPlayer != null) {
            EmoteManager.play(dummyUuid, selectedEmote);
        }
    }

    private void applyCape() {
        if (selectedCape == null) return;

        if (parentScreen instanceof SkinSelectionScreen selectionScreen && "skinpack".equals(selectionScreen.getSelectedCapesCategory())) {
            if (selectedCape.id.equals("none")) {
                SkinManager.setLocalCapeOverride(SkinManager.CAPE_NONE_SKIN_ID);
            } else {
                String prefix = "skinpack:";
                if (selectedCape.id.startsWith(prefix)) {
                    SkinId capeSkinId = SkinId.parse(selectedCape.id.substring(prefix.length()));
                    SkinManager.setLocalCapeOverride(capeSkinId);
                }
            }
            SkinManager.setLocalAccountCapeOverride(null); // Clear account override
            BedrockSessionSkin.clearCache();
            selectionScreen.onCapeChanged(selectedCape.id);
            ClientSkinSync.syncCurrentSkin(minecraft);
            return;
        }

        // Disable skin pack cape so the account cape is used
        SkinManager.setLocalCapeOverride(SkinManager.CAPE_NONE_SKIN_ID);

        String token = minecraft.getUser().getAccessToken();
        if (token == null || token.isEmpty() || "0".equals(token) || token.length() < 10) {
            return;
        }

        if (selectButton != null) {
            selectButton.active = false;
            selectButton.setMessage(Component.literal("Equipping..."));
        }

        CompletableFuture<Void> future;
        if (selectedCape.id.equals("none")) {
            future = CapeManager.unequipCape(token);
        } else {
            future = CapeManager.equipCape(token, selectedCape.id);
        }

        future.thenRun(() -> minecraft.execute(() -> {
            if (selectButton != null) {
                selectButton.active = true;
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip"));
            }
            if (selectedCape.id.equals("none")) {
                SkinManager.setLocalAccountCapeOverride(SkinManager.CAPE_NONE);
            } else {
                SkinManager.setLocalAccountCapeOverride(selectedCape.textureIdentifier);
            }
            BedrockSessionSkin.clearCache();
            if (parentScreen instanceof SkinSelectionScreen selectionScreen) {
                selectionScreen.onCapeChanged(selectedCape.id);
            }
            ClientSkinSync.syncCurrentSkin(minecraft);
        })).exceptionally(e -> {
            minecraft.execute(() -> {
                if (selectButton != null) {
                    selectButton.active = true;
                    selectButton.setMessage(Component.translatable("bedrockskins.button.equip"));
                }
                LOGGER.error("Failed to equip cape", e);
            });
            return null;
        });
    }

    private void uploadSkinToMojang() {
        if (!BedrockSkinsConfig.isAccountSkinUploadAllowed() || selectedSkin == null || isUploadingSkin) return;

        applySkin();

        String token = minecraft.getUser().getAccessToken();
        if (token == null || token.isEmpty() || "0".equals(token) || token.length() < 10) {
            setUploadStatus(Component.translatable("bedrockskins.status.upload_skin_offline").getString(), true);
            return;
        }

        byte[] textureBytes = ExternalAssetUtil.loadTextureData(selectedSkin, minecraft);
        if (textureBytes.length == 0) {
            setUploadStatus("Failed to read skin texture data.", true);
            return;
        }

        String variant = MojangSkinManager.isSkinSlim(selectedSkin) ? "slim" : "classic";

        isUploadingSkin = true;
        if (uploadSkinButton != null) {
            uploadSkinButton.active = false;
            uploadSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.status.uploading_skin")));
        }
        setUploadStatus(Component.translatable("bedrockskins.status.uploading_skin").getString(), false);

        MojangSkinManager.uploadSkin(token, textureBytes, variant).thenRun(() -> minecraft.execute(() -> {
            isUploadingSkin = false;
            if (uploadSkinButton != null) {
                uploadSkinButton.active = true;
                uploadSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.upload_skin")));
            }
            setUploadStatus(Component.translatable("bedrockskins.status.upload_skin_success").getString(), false);

            var profile = minecraft.getGameProfile();
            if (profile != null) {
                minecraft.getSkinManager().createLookup(profile, true);
            }
        })).exceptionally(e -> {
            minecraft.execute(() -> {
                isUploadingSkin = false;
                if (uploadSkinButton != null) {
                    uploadSkinButton.active = true;
                    uploadSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.upload_skin")));
                }
                String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                setUploadStatus(Component.translatable("bedrockskins.status.upload_skin_failed", errorMsg).getString(), true);
                LOGGER.error("Failed to upload skin to Mojang", e);
            });
            return null;
        });
    }

    private void setUploadStatus(String message, boolean isError) {
        this.uploadStatusMessage = message;
        this.uploadStatusIsError = isError;
        this.uploadStatusTime = System.currentTimeMillis();
    }

    private void toggleFavorite() {
        if (selectedSkin == null || MinecraftAccountSkin.is(selectedSkin)
                || ImportSkinAction.is(selectedSkin)) return;
        if (FavoritesManager.isFavorite(selectedSkin)) FavoritesManager.removeFavorite(selectedSkin);
        else FavoritesManager.addFavorite(selectedSkin);
        
        updateFavoriteButton();
        if (onFavoritesChanged != null) onFavoritesChanged.run();
    }

    private void updateActionButtons() {
        if (uploadSkinButton != null) {
            boolean uploadable = selectedSkin != null && !MinecraftAccountSkin.is(selectedSkin)
                && !ImportSkinAction.is(selectedSkin);
            uploadSkinButton.active = !isUploadingSkin && uploadable;
            uploadSkinButton.visible = BedrockSkinsConfig.isAccountSkinUploadAllowed() && uploadable;
        }
    }

    private void updateFavoriteButton() {
        updateActionButtons();
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == AppearanceTab.COSMETICS) {
            if (selectButton != null) {
                selectButton.active = selectedCosmetic != null;
                boolean equipped = PersonaManager.isLocallyEquipped(selectedCosmetic);
                selectButton.setMessage(Component.translatable(equipped
                    ? "bedrockskins.button.unequip" : "bedrockskins.button.equip"));
            }
            if (uploadSkinButton != null) uploadSkinButton.visible = false;
            return;
        }
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == AppearanceTab.CAPES) {
            if (selectButton != null) selectButton.active = selectedCape != null;
            if (uploadSkinButton != null) uploadSkinButton.visible = false;
            return;
        }
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == AppearanceTab.EMOTES) {
            if (selectButton != null) {
                selectButton.active = selectedEmote != null;
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip"));
            }
            if (replayEmoteButton != null) replayEmoteButton.active = selectedEmote != null;
            if (uploadSkinButton != null) uploadSkinButton.visible = false;
            return;
        }

        if (favoriteButton == null) return;

        boolean virtualSkin = MinecraftAccountSkin.is(selectedSkin) || ImportSkinAction.is(selectedSkin);
        boolean isFav = !virtualSkin && FavoritesManager.isFavorite(selectedSkin);
        favoriteButton.setSelected(isFav);
        favoriteButton.setActive(!virtualSkin && currentSkinId != null);
        favoriteButton.setTooltip(Component.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));

        if (selectButton != null) {
            boolean enabled = selectedSkin != null;
            if (enabled && BedrockSkinsClient.blockUnfairSkins && selectedSkin.unfair) enabled = false;
            selectButton.active = enabled;
        }
    }

    public void renderPreview(GuiGraphicsExtractor gui, int mouseX) {
        int contentTop = previewTop;
        int contentBottom = previewBottom;
        int centerX = x + width / 2;
        AppearanceTab activeTab = (parentScreen instanceof SkinSelectionScreen selectionScreen)
            ? selectionScreen.getActiveTab() : AppearanceTab.SKINS;

        int rotateW = Math.clamp((int)(width * 0.3f), 30, 90);
        int rotateH = (int)Math.ceil(rotateW * (7.0f / 45.0f));

        if (dummyPlayer != null) {
            if (currentSkinId == null && (selectedSkin == null || MinecraftAccountSkin.is(selectedSkin))) {
                applyAutoSelectedSkinBehavior();
            }
            if (selectedCape != null) {
                if (selectedCape.id.equals("none")) {
                    if (parentScreen instanceof SkinSelectionScreen selectionScreen && "skinpack".equals(selectionScreen.getSelectedCapesCategory())) {
                        dummyPlayer.clearForcedCape();
                    } else {
                        dummyPlayer.setForcedCape(null);
                    }
                } else {
                    dummyPlayer.setForcedCape(selectedCape.textureIdentifier);
                }
            }
            if (isDraggingPreview) {
                rotationX -= (mouseX - lastMouseX) * 0.5f;
            }
            lastMouseX = mouseX;

            String nameToRender = null;
            String descToRender = null;

            if (activeTab == AppearanceTab.COSMETICS) {
                if (selectedCosmetic != null) {
                    nameToRender = selectedCosmetic.displayName;
                    descToRender = PersonaTypeNames.displayName(selectedCosmetic.type).getString();
                }
            } else if (activeTab == AppearanceTab.CAPES) {
                if (selectedCape != null) {
                    nameToRender = Component.translatable(selectedCape.alias).getString();
                    descToRender = Component.translatable("bedrockskins.capes.description").getString();
                }
            } else if (activeTab == AppearanceTab.EMOTES) {
                if (selectedEmote != null) {
                    nameToRender = selectedEmote.displayName();
                    descToRender = Component.translatable("bedrockskins.emotes.description").getString();
                }
            } else {
                if (selectedSkin != null) {
                    nameToRender = GuiSkinUtils.getSkinDisplayNameText(selectedSkin);
                    descToRender = GuiSkinUtils.getSkinDescriptionText(selectedSkin).orElse(null);
                }
            }

            int modelAreaHeight = Math.max(0, contentBottom - contentTop);
            int scale = Math.max((int)(modelAreaHeight * 0.48f), 20);
            int centerY = contentTop + (modelAreaHeight / 2) - (rotateH / 2);

            renderRotatableEntity(gui, centerX, centerY, width - 16, modelAreaHeight, scale, dummyPlayer);

            int rotateY = Math.min((int)(centerY + (scale * 0.95f)), contentBottom - rotateH - 2);
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.ROTATE_SPRITE, centerX - (rotateW / 2), rotateY, rotateW, rotateH);

            if (uploadStatusMessage != null && System.currentTimeMillis() - uploadStatusTime < 4500) {
                int statusColor = uploadStatusIsError ? 0xFFFF5555 : 0xFF55FF55;
                gui.centeredText(font, Component.literal(uploadStatusMessage), centerX, contentTop + 4, statusColor);
            }

            renderInfoPanel(gui, nameToRender, descToRender);
        }
    }

    private void renderInfoPanel(GuiGraphicsExtractor gui, String name, String description) {
        GuiUtils.drawPanelChrome(gui, x, infoPanelY, width, INFO_PANEL_HEIGHT, Component.empty(), font);

        int textY = infoPanelY
            + Math.max(0, (GuiUtils.PANEL_HEADER_HEIGHT - font.lineHeight) / 2);
        int availableWidth = Math.max(0, width - 16);
        boolean hasName = name != null && !name.isEmpty();
        boolean hasDescription = description != null && !description.isEmpty();
        int nameWidth = hasName && hasDescription ? Math.max(0, (availableWidth - 8) / 2) : availableWidth;
        int descriptionWidth = hasName && hasDescription ? Math.max(0, availableWidth - nameWidth - 8) : availableWidth;

        if (hasName) {
            String fittedName = fitText(name, nameWidth);
            gui.text(font, Component.literal(fittedName), x + 8, textY, 0xFFFFFFFF, false);
        }
        if (hasDescription) {
            String fittedDescription = fitText(description, descriptionWidth);
            gui.text(font, Component.literal(fittedDescription),
                x + width - 8 - font.width(fittedDescription), textY, 0xFFAAAAAA, false);
        }
    }

    private String fitText(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int targetWidth = Math.max(0, maxWidth - font.width(ellipsis));
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > targetWidth) end--;
        return text.substring(0, end) + ellipsis;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        AppearanceTab activeTab = (parentScreen instanceof SkinSelectionScreen selectionScreen)
            ? selectionScreen.getActiveTab() : AppearanceTab.SKINS;
        if (activeTab == AppearanceTab.COSMETICS && PersonaManager.isSideSelectable(selectedCosmetic)
                && mouseY < y + 52) return false;
        if ((button == 0 || button == InputConstants.MOUSE_BUTTON_LEFT) && mouseX >= previewLeft && mouseX <= previewRight && mouseY >= previewTop && mouseY <= previewBottom) {
            isDraggingPreview = true;
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(int button) {
        if ((button == 0 || button == InputConstants.MOUSE_BUTTON_LEFT) && isDraggingPreview) {
            isDraggingPreview = false;
            return true;
        }
        return false;
    }
    
    private void renderRotatableEntity(GuiGraphicsExtractor gui, int centerX, int centerY, int boxWidth, int boxHeight, int scale, PreviewPlayer entity) {
        int halfW = boxWidth / 2, halfH = boxHeight / 2;
        GuiUtils.renderEntityInRect(gui, entity, rotationX * 3, centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH, scale);
    }
    
    public void setButtonsVisible(boolean visible) {
        if (selectButton != null) selectButton.visible = visible;
        if (replayEmoteButton != null) {
            replayEmoteButton.visible = visible && parentScreen instanceof SkinSelectionScreen screen
                && screen.getActiveTab() == AppearanceTab.EMOTES;
        }
        if (favoriteButton != null) favoriteButton.getButton().visible = visible;
        if (previewButton != null) previewButton.visible = visible;
        if (uploadSkinButton != null) {
            uploadSkinButton.visible = visible && BedrockSkinsConfig.isAccountSkinUploadAllowed()
                && selectedSkin != null && !MinecraftAccountSkin.is(selectedSkin)
                && !ImportSkinAction.is(selectedSkin);
        }
    }

    public void updateButtonsForTab(AppearanceTab tabIndex) {
        reposition(x, y, width, height);
        if (replayEmoteButton != null) replayEmoteButton.visible = tabIndex == AppearanceTab.EMOTES;
        if (tabIndex == AppearanceTab.SKINS) {
            this.rotationX = 0.0f;
            if (selectButton != null) {
                selectButton.visible = true;
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip"));
            }
            if (favoriteButton != null) favoriteButton.getButton().visible = true;
            if (previewButton != null) previewButton.visible = true;
            if (uploadSkinButton != null) {
                uploadSkinButton.visible = BedrockSkinsConfig.isAccountSkinUploadAllowed()
                    && selectedSkin != null && !MinecraftAccountSkin.is(selectedSkin)
                    && !ImportSkinAction.is(selectedSkin);
            }
        } else if (tabIndex == AppearanceTab.COSMETICS) {
            this.rotationX = 0.0f;
            if (selectButton != null) {
                selectButton.visible = true;
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip"));
            }
            if (favoriteButton != null) favoriteButton.getButton().visible = false;
            if (previewButton != null) previewButton.visible = true;
            if (uploadSkinButton != null) uploadSkinButton.visible = false;
        } else if (tabIndex == AppearanceTab.CAPES) {
            this.rotationX = 60.0f;
            if (selectButton != null) {
                selectButton.visible = true;
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip"));
            }
            if (favoriteButton != null) favoriteButton.getButton().visible = false;
            if (previewButton != null) previewButton.visible = true;
            if (uploadSkinButton != null) uploadSkinButton.visible = false;
        } else if (tabIndex == AppearanceTab.EMOTES) {
            this.rotationX = 0.0f;
            if (selectButton != null) {
                selectButton.visible = true;
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip"));
                int gap = 4;
                int actionX = x + 8;
                int actionWidth = Math.max(10, width - 16);
                int halfWidth = Math.max(10, (actionWidth - gap) / 2);
                selectButton.setX(actionX);
                selectButton.setWidth(halfWidth);
                if (replayEmoteButton != null) {
                    replayEmoteButton.setX(actionX + halfWidth + gap);
                    replayEmoteButton.setWidth(Math.max(10, actionWidth - halfWidth - gap));
                }
            }
            if (favoriteButton != null) favoriteButton.getButton().visible = false;
            if (previewButton != null) previewButton.visible = true;
            if (uploadSkinButton != null) uploadSkinButton.visible = false;
        }
        updateFavoriteButton();
    }

    public void renderSprites(GuiGraphicsExtractor gui) {
        if (favoriteButton != null) favoriteButton.renderSprites(gui);
    }

    public void cleanup() {
        GuiSkinUtils.cleanupPreview(this.dummyUuid);
        PersonaManager.clearPreview(this.dummyUuid);
        EmoteManager.stop(this.dummyUuid);
        this.dummyPlayer = null;
        this.dummyUuid = UUID.randomUUID();
    }
    
    private static class FavoriteHeartButton {
        private final SpriteIconButton button;
        private final Identifier fullSprite;
        private boolean isFavorited = false;

        public FavoriteHeartButton(int size, Identifier containerSprite, Identifier fullSprite, Button.OnPress onPress) {
            this.fullSprite = fullSprite;
            this.button = SpriteIconButton.builder(Component.empty(), onPress, true)
                    .size(size, size).sprite(containerSprite, 12, 12).build();
        }

        public AbstractWidget getButton() { return button; }
        public void setSelected(boolean selected) { this.isFavorited = selected; }
        public void setActive(boolean active) { button.active = active; }
        public void setTooltip(Component tooltip) { button.setTooltip(Tooltip.create(tooltip)); }

        public void renderSprites(GuiGraphicsExtractor graphics) {
            if (button.visible && isFavorited) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, button.getX() + 4, button.getY() + 4, 12, 12);
            }
        }
    }
}
