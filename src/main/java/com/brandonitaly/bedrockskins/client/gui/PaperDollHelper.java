package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;

public class PaperDollHelper {
    private static final int PREVIEW_W = 78, PREVIEW_H = 112, BTN_SIZE = 20;
    private static final int RENDER_PADDING_TOP = 14;
    private static final int SIZE_CAP = 56;
    private static final double Y_TRANSLATION = 0.9;
    private static final double PLAYER_HEIGHT_BLOCKS = 1.8;
    private static final int SPACER = 8;
    private static final int HOVER_PADDING = 5;

    private PreviewPlayer previewPlayer;
    private final UUID previewUuid = UUID.randomUUID();
    private SpriteIconButton openSkinButton;
    
    private float previewYaw = 0.0F;
    private boolean draggingPreview = false;
    private boolean leftMouseDown = false;
    private double lastRotMouseX = 0.0;

    private final Screen parentScreen;
    private final boolean isTitleScreen;

    public PaperDollHelper(Screen parentScreen, boolean isTitleScreen) {
        this.parentScreen = parentScreen;
        this.isTitleScreen = isTitleScreen;
    }

    private AbstractWidget findMenuTargetButton() {
        AbstractWidget target = null;
        for (var child : parentScreen.children()) {
            if (child instanceof Button widget && widget.visible && widget != openSkinButton) {
                if (!(widget instanceof PlainTextButton) && !(widget instanceof SpriteIconButton)) {
                    if (target == null) {
                        target = widget;
                    } else {
                        if (widget.getY() > target.getY()) {
                            target = widget;
                        } else if (widget.getY() == target.getY()) {
                            if (widget.getX() > target.getX()) {
                                target = widget;
                            }
                        }
                    }
                }
            }
        }
        return target;
    }

    private int getLeft(int width) { 
        AbstractWidget target = findMenuTargetButton();
        if (target != null) {
            return target.getX() + target.getWidth() + 20;
        }
        return Math.round((float) ((width * 5 / 6.0) - (PREVIEW_W / 2.0))); 
    }
    
    private int getTop(int height) { 
        AbstractWidget target = findMenuTargetButton();
        if (target != null) {
            return target.getY() + target.getHeight() - 115;
        }
        return Math.round((float) ((height / 2.0) - (PREVIEW_H / 2.0))); 
    }

    private double getModelFeet(int top) {
        int renderTop = top - RENDER_PADDING_TOP;
        int renderBottom = top + PREVIEW_H;
        int size = Math.min((renderBottom - renderTop) / 3, SIZE_CAP);
        double centerY = (renderTop + renderBottom) / 2.0;
        return centerY + Y_TRANSLATION * size;
    }

    private double getModelTop(int top) {
        float heightMultiplier = 1.0f;
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) {
            BedrockPlayerModel model = BedrockModelManager.getModel(selected);
            if (model != null) {
                heightMultiplier = model.heightMultiplier;
            }
        }
        int renderTop = top - RENDER_PADDING_TOP;
        int renderBottom = top + PREVIEW_H;
        int size = Math.min((renderBottom - renderTop) / 3, SIZE_CAP);
        return getModelFeet(top) - (PLAYER_HEIGHT_BLOCKS * size * heightMultiplier);
    }

    public SpriteIconButton init(Minecraft minecraft, int width, int height) {
        draggingPreview = false;
        leftMouseDown = false;

        String name = minecraft.getGameProfile() != null ? minecraft.getGameProfile().name() : "Preview";
        previewPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(previewUuid, name));
        previewPlayer.setShowNameTag(true);
        previewPlayer.setDisplayName(Component.literal(name));

        updatePreviewSkin(minecraft);

        openSkinButton = SpriteIconButton.builder(Component.empty(), b -> minecraft.gui.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(parentScreen)), true)
            .size(BTN_SIZE, BTN_SIZE).sprite(BedrockSkinsSprites.MY_CHARACTERS_ICON, 16, 16).build();
        openSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.change_skin.tooltip")));
        
        updateLayout(width, height);
        return openSkinButton;
    }

    public void updateLayout(int width, int height) {
        if (openSkinButton != null) {
            int top = getTop(height);
            openSkinButton.setX(getLeft(width) + (PREVIEW_W - BTN_SIZE) / 2);
            openSkinButton.setY((int) Math.round(getModelFeet(top) + SPACER));
        }
    }

    private void updatePreviewSkin(Minecraft minecraft) {
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) {
            SkinManager.setPreviewSkin(previewUuid, selected.pack(), selected.name());
            SkinPackLoader.registerTextureFor(selected);
            previewPlayer.clearForcedProfileSkin();
            previewPlayer.clearForcedBody();
        } else {
            SkinManager.resetPreviewSkin(previewUuid);
            previewPlayer.clearForcedBody();
            var profile = minecraft.getGameProfile();
            if (profile != null) previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
        }

        // Determine what cape to show
        LoadedSkin loaded = selected != null ? SkinPackLoader.getLoadedSkin(selected) : null;
        SkinManager.ResolvedCape resolved = SkinManager.resolveCape(previewUuid, loaded, true);
        if (resolved != null) {
            if (resolved.capeId.equals(SkinManager.CAPE_NONE)) {
                previewPlayer.setForcedCape(null);
            } else {
                previewPlayer.setForcedCape(resolved.capeId);
            }
        } else {
            previewPlayer.clearForcedCape();
        }

        previewPlayer.setUseLocalPlayerModel(false);
    }

    private boolean isMouseOverPreview(int width, int height, double mouseX, double mouseY) {
        int l = getLeft(width), t = getTop(height);
        return mouseX >= l && mouseX <= l + PREVIEW_W && mouseY >= getModelTop(t) - HOVER_PADDING && mouseY <= getModelFeet(t) + HOVER_PADDING;
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int width, int height, Font font, Minecraft minecraft) {
        if (previewPlayer == null) return;

        if (SkinManager.getLocalSelectedKey() == null) {
            previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false).get());
        }

        long window = minecraft.getWindow().handle();
        boolean leftDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

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

        int left = getLeft(width), top = getTop(height);
        GuiUtils.renderEntityInRect(guiGraphics, previewPlayer, previewYaw, left - 40, top - RENDER_PADDING_TOP, left + PREVIEW_W + 40, top + PREVIEW_H, SIZE_CAP);

        if (previewPlayer.shouldShowName()) {
            int nametagTopY = (int) Math.round(getModelTop(top) - SPACER - font.lineHeight);
            GuiUtils.renderNameTag(guiGraphics, font, previewPlayer.getDisplayName(), left + (PREVIEW_W / 2), Math.max(2, nametagTopY));
        }
    }

    public void removed() {
        PreviewPlayer.PreviewPlayerPool.remove(previewUuid);
        SkinManager.resetPreviewSkin(previewUuid);
        previewPlayer = null;
        openSkinButton = null;
    }
}