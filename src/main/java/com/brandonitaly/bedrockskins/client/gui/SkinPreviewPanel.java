package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.function.Consumer;

public class SkinPreviewPanel {
    private final Minecraft minecraft;
    private final Font font;
    private final Runnable onFavoritesChanged;
    
    // State
    private int x, y, width, height;
    private FavoriteHeartButton favoriteButton;
    private Button selectButton, resetButton;
    private LoadedSkin selectedSkin;
    private SkinId currentSkinId;
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

    public void init(int x, int y, int w, int h, Consumer<AbstractWidget> widgetAdder) {
        selectButton = Button.builder(Component.translatable("bedrockskins.button.select"), b -> applySkin()).bounds(0, 0, 10, 20).build();
        widgetAdder.accept(selectButton);

        favoriteButton = new FavoriteHeartButton(20, BedrockSkinsSprites.HEART_CONTAINER, BedrockSkinsSprites.HEART_FULL, b -> toggleFavorite());
        widgetAdder.accept(favoriteButton.getButton());

        resetButton = Button.builder(Component.translatable("bedrockskins.button.reset"), b -> resetSkin()).bounds(0, 0, 10, 20).build();
        widgetAdder.accept(resetButton);
        
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
    }

    public void initPreviewState() {
        if (this.selectedSkin != null) {
            updatePreviewModel(this.dummyUuid, this.selectedSkin.getSkinId());
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
        this.currentSkinId = skin != null ? skin.getSkinId() : null;
        updateFavoriteButton();
        if (skin != null) updatePreviewModel(dummyUuid, skin.getSkinId());
    }

    private void updatePreviewModel(UUID uuid, SkinId skinId) {
        if (!this.dummyUuid.equals(uuid)) SkinManager.resetPreviewSkin(this.dummyUuid);
        this.dummyUuid = uuid;
        
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(uuid, name));

        if (skinId == null || GuiSkinUtils.isAutoSelectedSkin(selectedSkin)) {
            applyAutoSelectedSkinBehavior();
        } else {
            GuiSkinUtils.applyLoadedSkinPreview(dummyPlayer, uuid, selectedSkin);
        }
    }

    private void applyAutoSelectedSkinBehavior() {
        GuiSkinUtils.applyAutoSelectedPreview(minecraft, dummyPlayer, dummyUuid);
    }

    private void applySkin() {
        if (selectedSkin == null) return;
        try {
            GuiSkinUtils.applySelectedSkin(minecraft, selectedSkin);
            if (minecraft.player == null) {
                updatePreviewModel(dummyUuid, selectedSkin.getSkinId());
                updateActionButtons();
            }
        } catch (Exception e) { e.printStackTrace(); }
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
        if (favoriteButton == null) return;
        
        boolean isFav = FavoritesManager.isFavorite(selectedSkin);
        favoriteButton.setSelected(isFav);
        favoriteButton.setActive(currentSkinId != null);
        favoriteButton.setTooltip(Component.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));
        
        if (selectButton != null) selectButton.active = selectedSkin != null;
    }

    public void render(GuiGraphics gui, int mouseX) {
        GuiUtils.drawPanelChrome(gui, x, y, width, height, Component.translatable("bedrockskins.gui.preview"), font);

        int PANEL_HEADER_HEIGHT = 24;
        int BUTTONS_RESERVED_HEIGHT = 60; 
        
        int contentTop = y + PANEL_HEADER_HEIGHT;
        int contentBottom = y + height - BUTTONS_RESERVED_HEIGHT;
        int centerX = x + width / 2;

        int rotateW = Math.max(30, Math.min((int)(width * 0.3f), 90));
        int rotateH = (int)Math.ceil(rotateW * (7.0f / 45.0f));

        if (dummyPlayer != null) {
            if (currentSkinId == null && selectedSkin == null) applyAutoSelectedSkinBehavior();
            dummyPlayer.tickCount = (int)(Util.getMillis() / 50L);

            if (isDraggingPreview) {
                rotationX -= (mouseX - lastMouseX) * 0.5f;
            }
            lastMouseX = mouseX;

            String nameToRender = null;
            String descToRender = null;

            if (selectedSkin != null) {
                nameToRender = GuiSkinUtils.getSkinDisplayNameText(selectedSkin);
                descToRender = GuiSkinUtils.getSkinDescriptionText(selectedSkin).orElse(null);
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
            
            int currentTextY = textY;
            if (nameToRender != null && !nameToRender.isEmpty()) {
                gui.drawCenteredString(font, nameToRender, centerX, currentTextY, 0xFFAAAAAA);
                currentTextY += font.lineHeight + textGap;
            }
            if (descToRender != null && !descToRender.isEmpty()) {
                gui.drawCenteredString(font, descToRender, centerX, currentTextY, 0xFFAAAAAA);
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
    
    private void renderRotatableEntity(GuiGraphics gui, int centerX, int centerY, int boxWidth, int boxHeight, int scale, LivingEntity entity) {
        int halfW = boxWidth / 2, halfH = boxHeight / 2;
        GuiUtils.renderEntityInRect(gui, entity, rotationX * 3, centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH, scale);
    }
    
    public void setButtonsVisible(boolean visible) {
        if (selectButton != null) selectButton.visible = visible;
        if (resetButton != null) resetButton.visible = visible;
        if (favoriteButton != null) favoriteButton.getButton().visible = visible;
    }

    public void renderSprites(GuiGraphics gui) {
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

        public void renderSprites(GuiGraphics graphics) {
            if (button.visible && isFavorited) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, button.getX() + 4, button.getY() + 4, 12, 12);
            }
        }
    }
}