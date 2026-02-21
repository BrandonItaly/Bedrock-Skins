package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.ClientSkinSync;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.StateManager;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;*/
//?}
import net.minecraft.world.entity.LivingEntity;

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.UUID;
import java.util.function.Consumer;

public class SkinPreviewPanel {
    private final Minecraft minecraft;
    private final Font font;
    private final Runnable onFavoritesChanged;
    //? if >=1.21.11 {
    private static final Identifier ROTATE_SPRITE = Identifier.fromNamespaceAndPath("bedrockskins", "container/rotate");
    //?} else {
    /*private static final ResourceLocation ROTATE_SPRITE = ResourceLocation.fromNamespaceAndPath("bedrockskins", "container/rotate");*/
    //?}
    
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
    
    public LoadedSkin getSelectedSkin() {
        return selectedSkin;
    }

    public void init(int x, int y, int w, int h, Consumer<AbstractWidget> widgetAdder) {
        selectButton = Button.builder(Component.translatable("bedrockskins.button.select"), b -> applySkin()).bounds(0, 0, 10, 20).build();
        widgetAdder.accept(selectButton);

        //? if >=1.21.11 {
        Identifier heartEmpty = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container");
        Identifier heartFull = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");
        //?} else {
        /*ResourceLocation heartEmpty = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/container");
        ResourceLocation heartFull = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full");*/
        //?}
        favoriteButton = new FavoriteHeartButton(0, 0, 20, heartEmpty, heartFull, b -> toggleFavorite());
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
        if (!this.dummyUuid.equals(uuid)) safeResetPreview(this.dummyUuid.toString());
        this.dummyUuid = uuid;
        
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(uuid, name));

        if (skinId == null) {
            applyAutoSelectedSkinBehavior();
        } else {
            SkinManager.setPreviewSkin(uuid.toString(), skinId.getPack(), skinId.getName());
            safeRegisterTexture(skinId.toString());
            dummyPlayer.clearForcedProfileSkin();
            dummyPlayer.clearForcedBody();
            dummyPlayer.setForcedCape(selectedSkin != null ? selectedSkin.capeIdentifier : null);
            dummyPlayer.setUseLocalPlayerModel(false);
        }
    }

    private void applyAutoSelectedSkinBehavior() {
        if (dummyPlayer == null) return;
        SkinManager.resetPreviewSkin(dummyUuid.toString());
        
        if (minecraft.player != null) {
            dummyPlayer.clearForcedProfileSkin();
            dummyPlayer.setForcedBody(minecraft.player.getSkin().body());
            dummyPlayer.setForcedCapeTexture(minecraft.player.getSkin().cape());
            dummyPlayer.setUseLocalPlayerModel(true);
        } else {
            dummyPlayer.clearForcedBody();
            dummyPlayer.clearForcedCape();
            var profile = minecraft.getGameProfile();
            if (profile != null) {
                dummyPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
            } else {
                dummyPlayer.clearForcedProfileSkin();
            }
            dummyPlayer.setUseLocalPlayerModel(false);
        }
    }

    private void applySkin() {
        if (selectedSkin == null) return;
        try {
            SkinId id = selectedSkin.getSkinId();
            String key = id.toString();
            String pack = id.getPack() == null || id.getPack().isEmpty() ? "Remote" : id.getPack();
            
            safeRegisterTexture(key);
            
            if (minecraft.player != null) {
                SkinManager.setSkin(minecraft.player.getUUID().toString(), pack, id.getName());
                byte[] data = loadTextureData(selectedSkin);
                if (data.length > 0) ClientSkinSync.sendSetSkinPayload(id, selectedSkin.getGeometryData().toString(), data);
            } else {
                StateManager.saveState(FavoritesManager.getFavoriteKeys(), key);
                updatePreviewModel(dummyUuid, id);
                updateActionButtons();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void resetSkin() {
        selectedSkin = null;
        currentSkinId = null;
        if (minecraft.player != null) {
            SkinManager.resetSkin(minecraft.player.getUUID().toString());
            ClientSkinSync.sendResetSkinPayload();
            safeResetPreview(this.dummyUuid.toString());
            updatePreviewModel(this.dummyUuid, null);
        } else {
            StateManager.saveState(FavoritesManager.getFavoriteKeys(), null);
            safeResetPreview(dummyUuid.toString());
        }
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
        
        boolean isFav = selectedSkin != null && FavoritesManager.isFavorite(selectedSkin);
        favoriteButton.setSelected(isFav);
        favoriteButton.setActive(currentSkinId != null);
        favoriteButton.setTooltip(Component.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));
        
        if (selectButton != null) selectButton.active = selectedSkin != null;
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY) {
        GuiUtils.drawPanelChrome(gui, x, y, width, height, Component.translatable("bedrockskins.gui.preview"), font);
        
        int PANEL_HEADER_HEIGHT = 24, buttonsHeight = 90;
        int entityH = height - PANEL_HEADER_HEIGHT - buttonsHeight;
        
        int rotateW = Math.max(30, Math.min((int)(width * 0.3f), 90));
        int rotateH = (int)Math.ceil(rotateW * (7.0f / 45.0f)); 
        
        int rotateGap = 6;
        int availableHeight = Math.max(entityH - (rotateH + rotateGap), 0);
        int centerX = x + width / 2;
        int centerY = y + PANEL_HEADER_HEIGHT + availableHeight / 2 + 15;
        int uiStartY = y + height - 4 - 40 - 8 - font.lineHeight - 4; // text Y placement

        if (dummyPlayer != null) {
            if (currentSkinId == null && selectedSkin == null) applyAutoSelectedSkinBehavior();
            dummyPlayer.tickCount = (int)(Util.getMillis() / 50L);
            
            if (isDraggingPreview) rotationX -= (mouseX - lastMouseX) * 0.5f;
            lastMouseX = mouseX;
            
            renderRotatableEntity(gui, centerX, centerY, width, availableHeight, dummyPlayer);

            int rotateY = Math.max(previewBottom + (uiStartY - previewBottom - rotateH) / 2, previewBottom + rotateGap);
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, ROTATE_SPRITE, centerX - (rotateW / 2), rotateY, rotateW, rotateH);
        } else {
            gui.drawCenteredString(font, Component.translatable("bedrockskins.preview.unavailable"), centerX, y + PANEL_HEADER_HEIGHT + (availableHeight / 2) - (font.lineHeight / 2), 0xFFAAAAAA);
        }

        if (selectedSkin != null) {
            String name = SkinPackLoader.getTranslation(selectedSkin.getSafeSkinName());
            gui.drawCenteredString(font, name != null ? name : selectedSkin.getSkinDisplayName(), centerX, uiStartY, 0xFFAAAAAA);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= previewLeft && mouseX <= previewRight && mouseY >= previewTop && mouseY <= previewBottom) {
            isDraggingPreview = true;
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingPreview) {
            isDraggingPreview = false;
            return true;
        }
        return false;
    }
    
    private void renderRotatableEntity(GuiGraphics gui, int x, int y, int width, int height, LivingEntity entity) {
        int scale = Math.max((int)(height * 0.43f), 20);
        GuiUtils.renderEntityInRect(gui, entity, rotationX * 3, x - width, y - height, x + width, y + height, scale);
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
        safeResetPreview(this.dummyUuid.toString());
        PreviewPlayer.PreviewPlayerPool.remove(this.dummyUuid);
        this.dummyPlayer = null;
    }

    private void safeResetPreview(String uuid) { GuiUtils.safeResetPreview(uuid); }
    private void safeRegisterTexture(String key) { GuiUtils.safeRegisterTexture(key); }
    
    private byte[] loadTextureData(LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource res) {
                var opt = minecraft.getResourceManager().getResource(res.getId());
                if (opt.isPresent()) {
                    try (var is = opt.get().open()) { return is.readAllBytes(); }
                }
            } else if (src instanceof AssetSource.File f) {
                return Files.readAllBytes(new File(f.getPath()).toPath());
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    ZipEntry entry = zip.getEntry(z.getInternalPath());
                    if (entry != null) {
                        try (var is = zip.getInputStream(entry)) { return is.readAllBytes(); }
                    }
                }
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }
    
    private static class FavoriteHeartButton {
        private final Button button;
        //? if >=1.21.11 {
        private final Identifier containerSprite, fullSprite;
        public FavoriteHeartButton(int x, int y, int size, Identifier containerSprite, Identifier fullSprite, Button.OnPress onPress) {
        //?} else {
        /*private final ResourceLocation containerSprite, fullSprite;
        public FavoriteHeartButton(int x, int y, int size, ResourceLocation containerSprite, ResourceLocation fullSprite, Button.OnPress onPress) {*/
        //?}
            this.containerSprite = containerSprite;
            this.fullSprite = fullSprite;
            this.button = Button.builder(Component.empty(), onPress).bounds(x, y, size, size).build();
        }
        
        public AbstractWidget getButton() { return button; }
        public void setSelected(boolean selected) { this.isFavorited = selected; }
        public void setActive(boolean active) { button.active = active; }
        public void setTooltip(Component tooltip) { button.setTooltip(Tooltip.create(tooltip)); }
        
        private boolean isFavorited = false;
        
        public void renderSprites(GuiGraphics graphics) {
            if (button.visible) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, containerSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                if (isFavorited) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                }
            }
        }
    }
}