package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;

public class PaperDollHelper {
    private static final int PREVIEW_W = 78, PREVIEW_H = 112, BTN_SIZE = 20, BTN_GAP = -16;

    private PreviewPlayer previewPlayer;
    private final UUID previewUuid = UUID.randomUUID();
    private SpriteIconButton openSkinButton;
    
    private float previewYaw = 0.0F;
    private boolean draggingPreview = false;
    private boolean movingPreview = false;
    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;
    private double lastRotMouseX = 0.0;
    private double lastMoveMouseX = 0.0;
    private double lastMoveMouseY = 0.0;

    private final Screen parentScreen;
    private final boolean isTitleScreen;

    public PaperDollHelper(Screen parentScreen, boolean isTitleScreen) {
        this.parentScreen = parentScreen;
        this.isTitleScreen = isTitleScreen;
    }

    private double getOffsetX() { return isTitleScreen ? BedrockSkinsConfig.getPaperDollOffsetXTitle() : BedrockSkinsConfig.getPaperDollOffsetXPause(); }
    private void setOffsetX(double val) { if (isTitleScreen) BedrockSkinsConfig.setPaperDollOffsetXTitle(val); else BedrockSkinsConfig.setPaperDollOffsetXPause(val); }
    private double getOffsetY() { return isTitleScreen ? BedrockSkinsConfig.getPaperDollOffsetYTitle() : BedrockSkinsConfig.getPaperDollOffsetYPause(); }
    private void setOffsetY(double val) { if (isTitleScreen) BedrockSkinsConfig.setPaperDollOffsetYTitle(val); else BedrockSkinsConfig.setPaperDollOffsetYPause(val); }

    private int getLeft(int width) { 
        return (int) ((width * 5 / 6.0) - (PREVIEW_W / 2.0) + (getOffsetX() * width)); 
    }
    
    private int getTop(int height) { 
        return (int) ((height / 2.0) - (PREVIEW_H / 2.0) + (getOffsetY() * height)); 
    }

    public SpriteIconButton init(Minecraft minecraft, int width, int height) {
        draggingPreview = false; movingPreview = false;
        leftMouseDown = false; rightMouseDown = false;

        String name = minecraft.getGameProfile() != null ? minecraft.getGameProfile().name() : "Preview";
        previewPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(previewUuid, name));
        previewPlayer.setShowNameTag(true);
        previewPlayer.setDisplayName(Component.literal(name));

        updatePreviewSkin(minecraft);

        openSkinButton = SpriteIconButton.builder(Component.empty(), b -> minecraft.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(parentScreen)), true)
            .size(BTN_SIZE, BTN_SIZE).sprite(BedrockSkinsSprites.MY_CHARACTERS_ICON, 16, 16).build();
        openSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.change_skin.tooltip")));
        
        updateLayout(width, height);
        return openSkinButton;
    }

    private void updateLayout(int width, int height) {
        if (openSkinButton != null) {
            openSkinButton.setX(getLeft(width) + (PREVIEW_W - BTN_SIZE) / 2);
            openSkinButton.setY(getTop(height) + PREVIEW_H + BTN_GAP);
        }
    }

    private void updatePreviewSkin(Minecraft minecraft) {
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) {
            SkinManager.setPreviewSkin(previewUuid, selected.pack(), selected.name());
            SkinPackLoader.registerTextureFor(selected);
            previewPlayer.clearForcedProfileSkin();
            previewPlayer.clearForcedBody();
            previewPlayer.clearForcedCape();

            LoadedSkin loaded = SkinPackLoader.getLoadedSkin(selected);
            if (loaded != null) previewPlayer.setForcedCape(loaded.capeIdentifier);
        } else {
            SkinManager.resetPreviewSkin(previewUuid);
            previewPlayer.clearForcedBody();
            previewPlayer.clearForcedCape();
            var profile = minecraft.getGameProfile();
            if (profile != null) previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
        }
        previewPlayer.setUseLocalPlayerModel(false);
    }

    private boolean isMouseOverPreview(int width, int height, double mouseX, double mouseY) {
        int l = getLeft(width), t = getTop(height);
        return mouseX >= l && mouseX <= l + PREVIEW_W && mouseY >= t && mouseY <= t + PREVIEW_H;
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int width, int height, Font font, Minecraft minecraft) {
        if (previewPlayer == null) return;

        if (SkinManager.getLocalSelectedKey() == null) {
            previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false).get());
        }

        long window = minecraft.getWindow().handle();
        boolean leftDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean shiftDown = window != 0L && (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);

        boolean insidePreview = isMouseOverPreview(width, height, mouseX, mouseY);
        boolean insideButton = openSkinButton != null && openSkinButton.isHovered();

        // Rotation
        if (leftDown && !leftMouseDown && insidePreview && !insideButton) {
            draggingPreview = true;
            lastRotMouseX = mouseX;
        }
        leftMouseDown = leftDown;
        if (!leftDown) draggingPreview = false;

        if (draggingPreview) {
            previewYaw -= (float) (mouseX - lastRotMouseX) * 1.6F;
            lastRotMouseX = mouseX;
        }

        // Panning relative to screen size
        if (rightDown && shiftDown && !rightMouseDown && insidePreview && !insideButton) {
            movingPreview = true;
            lastMoveMouseX = mouseX;
            lastMoveMouseY = mouseY;
        }
        rightMouseDown = rightDown;
        if (!rightDown || !shiftDown) movingPreview = false;

        if (movingPreview) {
            double deltaX = (mouseX - lastMoveMouseX) / (double) width;
            double deltaY = (mouseY - lastMoveMouseY) / (double) height;
            
            setOffsetX(getOffsetX() + deltaX);
            setOffsetY(getOffsetY() + deltaY);
            updateLayout(width, height);
            
            lastMoveMouseX = mouseX;
            lastMoveMouseY = mouseY;
        }

        int left = getLeft(width), top = getTop(height);
        GuiUtils.renderEntityInRect(guiGraphics, previewPlayer, previewYaw, left - 40, top - 14, left + PREVIEW_W + 40, top + PREVIEW_H, 56);

        if (previewPlayer.shouldShowName()) {
            GuiUtils.renderNameTag(guiGraphics, font, previewPlayer.getDisplayName(), left + (PREVIEW_W / 2), Math.max(2, top - 12));
        }
    }

    public void removed() {
        PreviewPlayer.PreviewPlayerPool.remove(previewUuid);
        SkinManager.resetPreviewSkin(previewUuid);
        previewPlayer = null;
        openSkinButton = null;
    }
}