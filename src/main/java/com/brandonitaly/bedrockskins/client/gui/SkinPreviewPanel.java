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
    private Button selectButton;
    private Button resetButton;
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

    /**
     * @param widgetAdder A consumer to register buttons with the parent screen (e.g. this::addRenderableWidget)
     */
    public void init(int x, int y, int w, int h, Consumer<AbstractWidget> widgetAdder) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        
        // Initialize preview bounds
        int PANEL_HEADER_HEIGHT = 24;
        int buttonsHeight = 90;
        int entityY = y + PANEL_HEADER_HEIGHT;
        int entityH = h - PANEL_HEADER_HEIGHT - buttonsHeight;
        int availableHeight = Math.max(entityH, 50);
        previewLeft = x;
        previewRight = x + w;
        previewTop = entityY;
        previewBottom = entityY + availableHeight;
        
        int PANEL_PADDING = 4;
        int btnW = Math.min(width - 16, 140);
        int btnH = 20;
        int btnX = x + (width / 2) - (btnW / 2);
        int startY = y + h - PANEL_PADDING - btnH - 4;
        
        // Compute bottom and middle Y positions
        int bottomY = startY; // bottom row (was reset)
        int middleY = startY - (btnH + 4); // middle row (was select)

        // Compute narrow width and x for the right-side button (when paired with favorite)
        int selectBtnW = btnW - 20 - 2; // 20px for heart button, 2px gap
        int selectBtnX = btnX + 20 + 2; // Offset to the right

        // Select button (moved to bottom) - full width
        selectButton = Button.builder(Component.translatable("bedrockskins.button.select"), b -> applySkin())
                .bounds(btnX, bottomY, btnW, btnH).build();
        widgetAdder.accept(selectButton);

        // Favorite button (left of the middle row)
        //? if >=1.21.11 {
        Identifier heartEmpty = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container");
        Identifier heartFull = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");
        //?} else {
        /*ResourceLocation heartEmpty = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/container");
        ResourceLocation heartFull = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full");*/
        //?}
        favoriteButton = new FavoriteHeartButton(btnX, middleY, 20, heartEmpty, heartFull, b -> toggleFavorite());
        widgetAdder.accept(favoriteButton.getButton());

        // Reset button (placed in the middle row to the right of favorite, narrow)
        resetButton = Button.builder(Component.translatable("bedrockskins.button.reset"), b -> resetSkin())
                .bounds(selectBtnX, middleY, selectBtnW, btnH).build();
        widgetAdder.accept(resetButton);
        
        initPreviewState();
    }

    // Reposition the panel without adding new widgets
    public void reposition(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;

        int PANEL_HEADER_HEIGHT = 24;
        int buttonsHeight = 90;
        int entityY = y + PANEL_HEADER_HEIGHT;
        int entityH = h - PANEL_HEADER_HEIGHT - buttonsHeight;
        int availableHeight = Math.max(entityH, 50);
        previewLeft = x;
        previewRight = x + w;
        previewTop = entityY;
        previewBottom = entityY + availableHeight;

        int PANEL_PADDING = 4;
        int btnW = Math.min(width - 16, 140);
        int btnH = 20;
        int btnX = x + (width / 2) - (btnW / 2);
        int startY = y + h - PANEL_PADDING - btnH - 4;

        int baseStartY = startY; // this is the Y used for the reset button
        int middleY = baseStartY - (btnH + 4); // middle row

        int selectBtnW = btnW - 20 - 2; // 20px for heart button, 2px gap
        int selectBtnX = btnX + 20 + 2; // Offset to the right

        if (selectButton != null) {
            // select should be full-width on the bottom row
            selectButton.setX(btnX);
            selectButton.setY(baseStartY);
            selectButton.setWidth(btnW);
        }
        if (favoriteButton != null) {
            // favorite remains in the middle-left position
            favoriteButton.getButton().setX(btnX);
            favoriteButton.getButton().setY(middleY);
        }
        if (resetButton != null) {
            // reset is narrow and placed to the right of favorite in the middle row
            resetButton.setX(selectBtnX);
            resetButton.setY(middleY);
            resetButton.setWidth(selectBtnW);
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
            
            // Try to load the skin object for the current key
            LoadedSkin currentSkin = SkinPackLoader.getLoadedSkin(currentKey);
            if (currentSkin != null) {
                this.selectedSkin = currentSkin;
            }
            
            updatePreviewModel(dummyUuid, currentKey);
            updateFavoriteButton(); // Enable buttons for current skin
        } else {
            this.currentSkinId = null;
            updatePreviewModel(dummyUuid, null);
            updateFavoriteButton();
            updateActionButtons();
        }
    }

    public void setSelectedSkin(LoadedSkin skin) {
        this.selectedSkin = skin;
        this.currentSkinId = skin != null ? skin.getSkinId() : null;
        updateFavoriteButton();
        updateActionButtons();
        // Ensure reset is enabled after selecting a skin and update preview availability
        if (resetButton != null) resetButton.active = true;
        updateActionButtons();
        if (skin != null) {
            updatePreviewModel(dummyUuid, skin.getSkinId());
        }
    }

    private void updatePreviewModel(UUID uuid, SkinId skinId) {
        if (!this.dummyUuid.equals(uuid)) {
            safeResetPreview(this.dummyUuid.toString());
        }
        this.dummyUuid = uuid;
        
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        GameProfile profile = new GameProfile(uuid, name);
        dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(profile);

        if (skinId == null) {
            applyAutoSelectedSkinBehavior();
        } else {
            String pack = skinId.getPack();
            String skinName = skinId.getName();
            SkinManager.setPreviewSkin(uuid.toString(), pack, skinName);
            safeRegisterTexture(skinId.toString());
            dummyPlayer.clearForcedProfileSkin();
            dummyPlayer.clearForcedBody();
            dummyPlayer.clearForcedCape();
            dummyPlayer.setUseLocalPlayerModel(false);
            var capeToUse = (selectedSkin != null) ? selectedSkin.capeIdentifier : null;
            dummyPlayer.setForcedCape(capeToUse);
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
                dummyPlayer.setForcedProfileSkin(
                        minecraft.getSkinManager().createLookup(profile, false).get()
                );
            } else {
                dummyPlayer.clearForcedProfileSkin();
            }
            dummyPlayer.setUseLocalPlayerModel(false);
        }
    }

    private void applySkin() {
        if (selectedSkin == null) return;
        try {
            byte[] data = loadTextureData(selectedSkin);
            var id = selectedSkin.getSkinId();
            String key = id == null ? null : id.toString();
            String pack = id == null ? "Remote" : (id.getPack() == null || id.getPack().isEmpty() ? "Remote" : id.getPack());
            String name = id == null ? "" : id.getName();
            
            safeRegisterTexture(key);
            
            if (minecraft.player != null) {
                SkinManager.setSkin(minecraft.player.getUUID().toString(), pack, name);
                if (data.length > 0) {
                    ClientSkinSync.sendSetSkinPayload(id, selectedSkin.getGeometryData().toString(), data);
                }
            } else {
                StateManager.saveState(FavoritesManager.getFavoriteKeys(), key);
                updatePreviewModel(dummyUuid, SkinId.parse(key));
        updateActionButtons();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        updateActionButtons();
        if (onFavoritesChanged != null) onFavoritesChanged.run();
    }

    private void updateActionButtons() {
        // Called when skin/player state changes to update reset/preview button states
        if (resetButton != null) {
            boolean resetActive;
            if (minecraft.player != null) {
                // Enable reset if player has a non-default selected key OR a skin is selected in UI
                resetActive = SkinManager.getLocalSelectedKey() != null || selectedSkin != null;
            } else {
                resetActive = selectedSkin != null || currentSkinId != null;
            }
            resetButton.active = resetActive;
        }

    }

    private void updateFavoriteButton() {
        // Ensure action buttons reflect current state
        updateActionButtons();
        if (favoriteButton == null) return;
        
        boolean hasSkin = currentSkinId != null;
        boolean isFav = selectedSkin != null && FavoritesManager.isFavorite(selectedSkin);
        
        favoriteButton.setSelected(isFav);
        favoriteButton.setActive(hasSkin);
        favoriteButton.setTooltip(Component.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));
        
        if (selectButton != null) {
            // Enable select button only when a different skin is selected
            selectButton.active = selectedSkin != null;
        }

        // Reset button should be disabled if player already has default equipped (but enable when a skin is selected)
        boolean resetActive;
        if (minecraft.player != null) {
            resetActive = SkinManager.getLocalSelectedKey() != null || selectedSkin != null;
        } else {
            resetActive = selectedSkin != null || currentSkinId != null;
        }
        if (resetButton != null) resetButton.active = resetActive;
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY) {
        GuiUtils.drawPanelChrome(gui, x, y, width, height, Component.translatable("bedrockskins.gui.preview"), font);
        
        int PANEL_HEADER_HEIGHT = 24;
        int entityY = y + PANEL_HEADER_HEIGHT;
        int buttonsHeight = 90;
        int entityH = height - PANEL_HEADER_HEIGHT - buttonsHeight;
        
        // Reserve vertical space for the rotate hint sprite so it never overlaps preview or buttons
        // Dynamic scaling: ~30% of panel width, clamped between 30px and 90px
        int rotateW = Math.max(30, Math.min((int)(width * 0.3f), 90));
        int rotateH = (int)Math.ceil(rotateW * (7.0f / 45.0f)); // Maintain 45:7 ratio
        
        int rotateGap = 6; // space between preview and rotate sprite
        int reservedForRotate = rotateH + rotateGap;
        int availableHeight = Math.max(entityH - reservedForRotate, 0);
        
        previewLeft = x;
        previewRight = x + width;
        previewTop = entityY;
        previewBottom = entityY + availableHeight;
        int centerX = x + width / 2;
        int centerY = entityY + availableHeight / 2 + 15;

        // Calculate Y position where the UI (text/buttons) starts
        int PANEL_PADDING = 4;
        int btnH = 20;
        int uiStartY = y + height - PANEL_PADDING - (btnH * 2) - 8 - font.lineHeight - 4;

        if (dummyPlayer != null) {
            if (currentSkinId == null && selectedSkin == null) {
                applyAutoSelectedSkinBehavior();
            }

            dummyPlayer.tickCount = (int)(Util.getMillis() / 50L);
            
            // Update rotation when dragging
            if (isDraggingPreview) {
                float sensitivity = 0.5f;
                float deltaX = (mouseX - lastMouseX) * sensitivity;
                rotationX -= deltaX;
            }
            
            lastMouseX = mouseX;
            
            renderRotatableEntity(gui, centerX, centerY, width, availableHeight, dummyPlayer);

            // Draw rotate hint sprite centered between the preview area and the UI buttons/text
            int rotateX = centerX - (rotateW / 2);
            
            // Calculate gap and center the sprite within it
            int gap = uiStartY - previewBottom;
            int rotateY = previewBottom + (gap - rotateH) / 2;
            
            // Safety check: ensure it doesn't overlap the preview bottom (use default gap as min)
            if (rotateY < previewBottom + rotateGap) {
                rotateY = previewBottom + rotateGap;
            }

            gui.blitSprite(RenderPipelines.GUI_TEXTURED, ROTATE_SPRITE, rotateX, rotateY, rotateW, rotateH);
        } else {
            int textY = entityY + (availableHeight / 2) - (font.lineHeight / 2);
            gui.drawCenteredString(font, Component.translatable("bedrockskins.preview.unavailable"), centerX, textY, 0xFFAAAAAA);
        }

        if (selectedSkin != null) {
            String name = SkinPackLoader.getTranslation(selectedSkin.getSafeSkinName());
            if (name == null) name = selectedSkin.getSkinDisplayName();
            // Use calculated uiStartY to ensure consistency with rotation sprite placement
            gui.drawCenteredString(font, name, centerX, uiStartY, 0xFFAAAAAA);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click is within preview area
        if (button == 0 && mouseX >= previewLeft && mouseX <= previewRight && 
            mouseY >= previewTop && mouseY <= previewBottom) {
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
        float fillPercentage = 0.43f; 
        int scale = (int)(height * fillPercentage);
        
        // Ensure reasonable minimum and remove restrictive max cap
        scale = Math.max(scale, 20);

        // Compute yaw offset from rotationX and render via shared helper
        float rotationModifier = 3;
        float yawOffset = rotationX * rotationModifier;
        
        // Pass the dynamically calculated scale
        GuiUtils.renderEntityInRect(gui, entity, yawOffset, (int)(x - width), (int)(y - height), (int)(x + width), (int)(y + height), scale);
    }
    
    public void setButtonsVisible(boolean visible) {
        if (selectButton != null) selectButton.visible = visible;
        if (resetButton != null) resetButton.visible = visible;
        if (favoriteButton != null) favoriteButton.getButton().visible = visible;
    }

    public void renderSprites(GuiGraphics gui) {
        if (favoriteButton != null) {
            favoriteButton.renderSprites(gui);
        }
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
                var resOpt = minecraft.getResourceManager().getResource(res.getId());
                if (resOpt.isPresent()) return resOpt.get().open().readAllBytes();
            } else if (src instanceof AssetSource.File f) {
                return Files.readAllBytes(new File(f.getPath()).toPath());
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    ZipEntry entry = zip.getEntry(z.getInternalPath());
                    if (entry != null) {
                        try (var is = zip.getInputStream(entry)) {
                            return is.readAllBytes();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }
    
    //? if >=1.21.11 {
    private static class FavoriteHeartButton {
        private final Button button;
        private final Identifier containerSprite;
        private final Identifier fullSprite;
        private boolean isFavorited = false;
        
        public FavoriteHeartButton(int x, int y, int size, Identifier containerSprite, Identifier fullSprite, Button.OnPress onPress) {
            this.containerSprite = containerSprite;
            this.fullSprite = fullSprite;
            this.button = Button.builder(Component.empty(), onPress)
                .size(size, size)
                .build();
            this.button.setPosition(x, y);
        }
        
        public AbstractWidget getButton() { return button; }
        public void setSelected(boolean selected) { this.isFavorited = selected; }
        public void setActive(boolean active) { button.active = active; }
        public void setTooltip(Component tooltip) { button.setTooltip(Tooltip.create(tooltip)); }
        
        public void renderSprites(GuiGraphics graphics) {
            if (button.visible) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, containerSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                if (isFavorited) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                }
            }
        }
    }
    //?} else {
    /*private static class FavoriteHeartButton {
        private final Button button;
        private final ResourceLocation containerSprite;
        private final ResourceLocation fullSprite;
        private boolean isFavorited = false;
        
        public FavoriteHeartButton(int x, int y, int size, ResourceLocation containerSprite, ResourceLocation fullSprite, Button.OnPress onPress) {
            this.containerSprite = containerSprite;
            this.fullSprite = fullSprite;
            this.button = Button.builder(Component.empty(), onPress)
                .size(size, size)
                .build();
            this.button.setPosition(x, y);
        }
        
        public AbstractWidget getButton() { return button; }
        public void setSelected(boolean selected) { this.isFavorited = selected; }
        public void setActive(boolean active) { button.active = active; }
        public void setTooltip(Component tooltip) { button.setTooltip(Tooltip.create(tooltip)); }
        
        public void renderSprites(GuiGraphics graphics) {
            if (button.visible) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, containerSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                if (isFavorited) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, button.getX() + 4, button.getY() + 4, 12, 12);
                }
            }
        }
    }*/
    //?}
}