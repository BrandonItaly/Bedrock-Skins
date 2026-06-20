package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.CapeManager;
import com.brandonitaly.bedrockskins.client.CapeManager.MinecraftCape;
import com.brandonitaly.bedrockskins.client.ClientSkinSync;
import java.util.concurrent.CompletableFuture;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;

public class SkinPreviewPanel {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Minecraft minecraft;
    private final Font font;
    private final Runnable onFavoritesChanged;
    
    // State
    private int x, y, width, height;
    private FavoriteHeartButton favoriteButton;
    private Button selectButton, resetButton;
    private SpriteIconButton customizationButton;
    private LoadedSkin selectedSkin;
    private MinecraftCape selectedCape;
    private SkinId currentSkinId;
    private Screen parentScreen;
    private PreviewPlayer dummyPlayer;
    private UUID dummyUuid = UUID.randomUUID();
    private float rotationX = 0;
    private int lastMouseX = 0;
    private boolean isDraggingPreview = false;
    private int previewLeft, previewRight, previewTop, previewBottom;

    public SkinPreviewPanel(Minecraft minecraft, Font font, Runnable onFavoritesChanged) {
        this.minecraft = minecraft;
        this.font = font;
        this.onFavoritesChanged = onFavoritesChanged;
    }
    
    public LoadedSkin getSelectedSkin() { return selectedSkin; }
    public MinecraftCape getSelectedCape() { return selectedCape; }

    public void setSelectedCape(MinecraftCape cape) {
        this.selectedCape = cape;
        if (dummyPlayer != null) {
            if (cape == null) {
                dummyPlayer.clearForcedCape();
                this.rotationX = 0.0f;
            } else if ("none".equals(cape.id)) {
                if (parentScreen instanceof SkinSelectionScreen selectionScreen && "skinpack".equals(selectionScreen.getSelectedCapesCategory())) {
                    dummyPlayer.clearForcedCape();
                } else {
                    dummyPlayer.setForcedCape(null);
                }
                this.rotationX = 0.0f;
            } else {
                dummyPlayer.setForcedCape(cape.textureIdentifier);
                this.rotationX = 60.0f;
            }
        }
        updateFavoriteButton();
    }

    public void init(int x, int y, int w, int h, Screen parentScreen, Consumer<AbstractWidget> widgetAdder) {
        this.parentScreen = parentScreen;
        selectButton = Button.builder(Component.translatable("bedrockskins.button.select"), b -> applySkin()).bounds(0, 0, 10, 20).build();
        widgetAdder.accept(selectButton);

        favoriteButton = new FavoriteHeartButton(20, BedrockSkinsSprites.HEART_CONTAINER, BedrockSkinsSprites.HEART_FULL, b -> toggleFavorite());
        widgetAdder.accept(favoriteButton.getButton());

        resetButton = Button.builder(Component.translatable("bedrockskins.button.reset"), b -> resetSkin()).bounds(0, 0, 10, 20).build();
        widgetAdder.accept(resetButton);

        customizationButton = SpriteIconButton.builder(Component.empty(), b -> {
            minecraft.setScreen(new SkinCustomizationScreen(parentScreen, minecraft.options));
        }, true).size(20, 20).sprite(BedrockSkinsSprites.CHARACTER_CREATOR_ICON, 16, 16).build();
        customizationButton.setTooltip(Tooltip.create(Component.translatable("options.skinCustomisation.title")));
        widgetAdder.accept(customizationButton);
        
        reposition(x, y, w, h);
        initPreviewState();
    }

    public void reposition(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.width = w; this.height = h;

        int PANEL_HEADER_HEIGHT = 24, buttonsHeight = 90;
        previewLeft = x;
        previewRight = x + w;
        previewTop = y + PANEL_HEADER_HEIGHT;
        previewBottom = previewTop + Math.max(h - PANEL_HEADER_HEIGHT - buttonsHeight, 50);

        int btnW = Math.min(w - 16, 140), btnH = 20;
        int btnX = x + (w / 2) - (btnW / 2);
        int bottomY = y + h - 8 - btnH;
        int middleY = bottomY - btnH - 4;

        if (selectButton != null) {
            selectButton.setX(btnX); selectButton.setY(bottomY); selectButton.setWidth(btnW);
        }
        if (favoriteButton != null) {
            favoriteButton.getButton().setX(btnX); favoriteButton.getButton().setY(middleY);
        }
        if (resetButton != null) {
            resetButton.setX(btnX + 22); resetButton.setY(middleY); resetButton.setWidth(btnW - 22);
        }
        if (customizationButton != null) {
            customizationButton.setX(x + w - 22);
            customizationButton.setY(y + 2);
        }
    }

    public void initPreviewState() {
        if (this.selectedSkin != null) {
            updatePreviewModel(this.dummyUuid, this.selectedSkin.skinId);
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
        this.currentSkinId = skin != null ? skin.skinId : null;
        updateFavoriteButton();
        if (skin != null) updatePreviewModel(dummyUuid, skin.skinId);
    }

    private void updatePreviewModel(UUID uuid, SkinId skinId) {
        if (!this.dummyUuid.equals(uuid)) SkinManager.resetPreviewSkin(this.dummyUuid);
        this.dummyUuid = uuid;
        
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(uuid, name));

        if (skinId == null) {
            applyAutoSelectedSkinBehavior();
        } else {
            GuiSkinUtils.applyLoadedSkinPreview(dummyPlayer, uuid, selectedSkin, false);
        }
    }

    private void applyAutoSelectedSkinBehavior() {
        GuiSkinUtils.applyAutoSelectedPreview(minecraft, dummyPlayer, dummyUuid);
    }

    private void applySkin() {
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == 1) {
            applyCape();
            return;
        }
        if (selectedSkin == null) return;
        if (BedrockSkinsClient.blockUnfairSkins && selectedSkin.unfair) return;
        try {
            GuiSkinUtils.applySelectedSkin(minecraft, selectedSkin);
            if (minecraft.player == null) {
                updatePreviewModel(dummyUuid, selectedSkin.skinId);
                updateActionButtons();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply selected skin {}", selectedSkin.skinId, e);
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
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip_cape"));
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
                    selectButton.setMessage(Component.translatable("bedrockskins.button.equip_cape"));
                }
                LOGGER.error("Failed to equip cape", e);
            });
            return null;
        });
    }

    private void resetSkin() {
        selectedSkin = null;
        currentSkinId = null;
        GuiSkinUtils.resetSelectedSkin(minecraft);
        updatePreviewModel(dummyUuid, null);
        updateFavoriteButton();
    }

    private void toggleFavorite() {
        if (selectedSkin == null) return;
        if (FavoritesManager.isFavorite(selectedSkin)) FavoritesManager.removeFavorite(selectedSkin);
        else FavoritesManager.addFavorite(selectedSkin);
        
        updateFavoriteButton();
        if (onFavoritesChanged != null) onFavoritesChanged.run();
    }

    private void updateActionButtons() {
        if (resetButton != null) {
            resetButton.active = selectedSkin != null || 
                (minecraft.player != null ? SkinManager.getLocalSelectedKey() != null : currentSkinId != null);
        }
    }

    private void updateFavoriteButton() {
        updateActionButtons();
        if (parentScreen instanceof SkinSelectionScreen selectionScreen && selectionScreen.getActiveTab() == 1) {
            if (selectButton != null) {
                selectButton.active = selectedCape != null;
            }
            return;
        }

        if (favoriteButton == null) return;

        boolean isFav = FavoritesManager.isFavorite(selectedSkin);
        favoriteButton.setSelected(isFav);
        favoriteButton.setActive(currentSkinId != null);
        favoriteButton.setTooltip(Component.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));

        if (selectButton != null) {
            boolean enabled = selectedSkin != null;
            if (enabled && BedrockSkinsClient.blockUnfairSkins && selectedSkin.unfair) enabled = false;
            selectButton.active = enabled;
        }
    }

    public void renderPreview(GuiGraphicsExtractor gui, int mouseX) {
        GuiUtils.drawPanelChrome(gui, x, y, width, height, Component.translatable("bedrockskins.gui.preview"), font);

        int PANEL_HEADER_HEIGHT = 24;
        int BUTTONS_RESERVED_HEIGHT = 60; 
        
        int contentTop = y + PANEL_HEADER_HEIGHT;
        int contentBottom = y + height - BUTTONS_RESERVED_HEIGHT;
        int centerX = x + width / 2;

        int rotateW = Math.clamp((int)(width * 0.3f), 30, 90);
        int rotateH = (int)Math.ceil(rotateW * (7.0f / 45.0f));

        if (dummyPlayer != null) {
            if (currentSkinId == null && selectedSkin == null) applyAutoSelectedSkinBehavior();
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

            int activeTab = (parentScreen instanceof SkinSelectionScreen selectionScreen) ? selectionScreen.getActiveTab() : 0;
            if (activeTab == 1) {
                if (selectedCape != null) {
                    nameToRender = Component.translatable(selectedCape.alias).getString();
                    if (selectedCape.id.equals("none")) {
                        descToRender = Component.translatable("bedrockskins.capes.description.none").getString();
                    } else if (selectedCape.id.startsWith("skinpack:")) {
                        descToRender = Component.translatable("bedrockskins.capes.description.skinpack").getString();
                    } else {
                        descToRender = Component.translatable("bedrockskins.capes.description.account").getString();
                    }
                }
            } else {
                if (selectedSkin != null) {
                    nameToRender = GuiSkinUtils.getSkinDisplayNameText(selectedSkin);
                    descToRender = GuiSkinUtils.getSkinDescriptionText(selectedSkin).orElse(null);
                }
            }

            int textGap = 4;
            int maxTextHeight = (font.lineHeight * 2) + textGap; 
            int textY = contentBottom - maxTextHeight;

            int modelAreaHeight = Math.max(0, textY - contentTop);
            int scale = Math.max((int)(modelAreaHeight * 0.40f), 20); 
            int centerY = contentTop + (modelAreaHeight / 2) - (rotateH / 2);

            renderRotatableEntity(gui, centerX, centerY, width - 16, modelAreaHeight, scale, dummyPlayer);

            int rotateY = Math.min((int)(centerY + (scale * 0.95f)), textY - rotateH - 4);
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.ROTATE_SPRITE, centerX - (rotateW / 2), rotateY, rotateW, rotateH);

            boolean hasName = nameToRender != null && !nameToRender.isEmpty();
            boolean hasDesc = descToRender != null && !descToRender.isEmpty();

            if (hasName || hasDesc) {
                int lineGap = 2;
                
                int textWidth = Math.max(hasName ? font.width(nameToRender) : 0, hasDesc ? font.width(descToRender) : 0);
                int textHeight = font.lineHeight + (hasName && hasDesc ? font.lineHeight + lineGap : 0) - 1;

                int minX = x + 8;
                int maxX = Math.max(minX, x + width - textWidth - 8);
                int tooltipX = Math.clamp(centerX - (textWidth / 2), minX, maxX);
                int tooltipCenterX = tooltipX + (textWidth / 2);

                TooltipRenderUtil.extractTooltipBackground(gui, tooltipX, textY, textWidth, textHeight, null);

                int textYCursor = textY;
                if (hasName) {
                    gui.centeredText(font, nameToRender, tooltipCenterX, textYCursor, 0xFFFFFFFF);
                    textYCursor += font.lineHeight + lineGap;
                }
                if (hasDesc) {
                    gui.centeredText(font, descToRender, tooltipCenterX, textYCursor, 0xFFAAAAAA);
                }
            }
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= previewLeft && mouseX <= previewRight && mouseY >= previewTop && mouseY <= previewBottom) {
            isDraggingPreview = true;
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(int button) {
        if (button == 0 && isDraggingPreview) {
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
        if (resetButton != null) resetButton.visible = visible;
        if (favoriteButton != null) favoriteButton.getButton().visible = visible;
        if (customizationButton != null) customizationButton.visible = visible;
    }

    public void updateButtonsForTab(int tabIndex) {
        reposition(x, y, width, height);
        if (tabIndex == 0) { // Skins
            this.rotationX = 0.0f;
            if (selectButton != null) {
                selectButton.visible = true;
                selectButton.setMessage(Component.translatable("bedrockskins.button.select"));
            }
            if (favoriteButton != null) favoriteButton.getButton().visible = true;
            if (resetButton != null) resetButton.visible = true;
            if (customizationButton != null) customizationButton.visible = true;
        } else if (tabIndex == 1) { // Capes
            this.rotationX = 60.0f;
            if (selectButton != null) {
                selectButton.visible = true;
                selectButton.setMessage(Component.translatable("bedrockskins.button.equip_cape"));
            }
            if (favoriteButton != null) favoriteButton.getButton().visible = false;
            if (resetButton != null) resetButton.visible = false;
            if (customizationButton != null) customizationButton.visible = true;
        } else { // Store
            setButtonsVisible(false);
        }
        updateFavoriteButton();
    }

    public void renderSprites(GuiGraphicsExtractor gui) {
        if (favoriteButton != null) favoriteButton.renderSprites(gui);
    }

    public void cleanup() {
        GuiSkinUtils.cleanupPreview(this.dummyUuid);
        this.dummyPlayer = null;
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