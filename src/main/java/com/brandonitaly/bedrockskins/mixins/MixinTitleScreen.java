package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.gui.GuiUtils;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft./*? if <1.21.11 {*//**//*?} else {*/util./*?}*/Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Component title) { super(title); }

    @Unique
    private static final int PREVIEW_W = 78, PREVIEW_H = 112, BTN_SIZE = 20, BTN_GAP = -16;

    @Unique
    private PreviewPlayer bedrockskins$menuPreviewPlayer;
    @Unique
    private final UUID bedrockskins$menuPreviewUuid = UUID.randomUUID();
    @Unique
    private SpriteIconButton bedrockskins$openSkinButton;
    @Unique
    private float bedrockskins$previewYaw = 0.0F;
    @Unique
    private boolean bedrockskins$draggingPreview = false;
    @Unique
    private boolean bedrockskins$leftMouseDown = false;
    @Unique
    private double bedrockskins$lastMouseX = 0.0;

    @Unique
    private int bedrockskins$getLeft() {
        return (BedrockSkinsConfig.isPaperDollLeftSideEnabled() ? (width / 6) : (width * 5 / 6)) - (PREVIEW_W / 2);
    }

    @Unique
    private int bedrockskins$getTop() { return (height - PREVIEW_H) / 2; }

    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockskins$initMainMenuPreview(CallbackInfo ci) {
        if (!BedrockSkinsConfig.isShowPaperDollOnMainMenu()) return;

        bedrockskins$draggingPreview = false;
        bedrockskins$leftMouseDown = false;

        // Initialize Player
        minecraft.getGameProfile();
        String name = minecraft.getGameProfile().name();
        bedrockskins$menuPreviewPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(bedrockskins$menuPreviewUuid, name));
        bedrockskins$menuPreviewPlayer.setShowNameTag(true);
        bedrockskins$menuPreviewPlayer.setCustomName(Component.literal(name));
        bedrockskins$menuPreviewPlayer.setCustomNameVisible(true);

        // Setup Skin
        bedrockskins$updatePreviewSkin();

        // Setup Button
        bedrockskins$openSkinButton = SpriteIconButton.builder(Component.empty(), b -> minecraft.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(this)), true)
            .size(BTN_SIZE, BTN_SIZE).sprite(BedrockSkinsSprites.HANGAR_ICON, 16, 16).build();
        
        bedrockskins$updateLayout();
        addRenderableWidget(bedrockskins$openSkinButton);
    }

    @Unique
    private void bedrockskins$updateLayout() {
        if (bedrockskins$openSkinButton != null) {
            bedrockskins$openSkinButton.setX(bedrockskins$getLeft() + (PREVIEW_W - BTN_SIZE) / 2);
            bedrockskins$openSkinButton.setY(bedrockskins$getTop() + PREVIEW_H + BTN_GAP);
        }
    }

    @Unique
    private void bedrockskins$updatePreviewSkin() {
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) {
            SkinManager.setPreviewSkin(bedrockskins$menuPreviewUuid, selected.getPack(), selected.getName());
            SkinPackLoader.registerTextureFor(selected);
            bedrockskins$menuPreviewPlayer.clearForcedProfileSkin();
            bedrockskins$menuPreviewPlayer.clearForcedBody();
            bedrockskins$menuPreviewPlayer.clearForcedCape();

            LoadedSkin loaded = SkinPackLoader.getLoadedSkin(selected);
            if (loaded != null) bedrockskins$menuPreviewPlayer.setForcedCape(loaded.capeIdentifier);
        } else {
            SkinManager.resetPreviewSkin(bedrockskins$menuPreviewUuid);
            bedrockskins$menuPreviewPlayer.clearForcedBody();
            bedrockskins$menuPreviewPlayer.clearForcedCape();
            var profile = minecraft.getGameProfile();
            bedrockskins$menuPreviewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
        }
        bedrockskins$menuPreviewPlayer.setUseLocalPlayerModel(false);
    }

    @Unique
    private boolean bedrockskins$isMouseOverPreview(double mouseX, double mouseY) {
        int l = bedrockskins$getLeft(), t = bedrockskins$getTop();
        return mouseX >= l && mouseX <= l + PREVIEW_W && mouseY >= t && mouseY <= t + PREVIEW_H;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockskins$renderMainMenuPreview(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (bedrockskins$menuPreviewPlayer == null || !BedrockSkinsConfig.isShowPaperDollOnMainMenu()) return;

        if (SkinManager.getLocalSelectedKey() == null) {
            minecraft.getGameProfile();
            bedrockskins$menuPreviewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false).get());
        }

        long window = minecraft.getWindow().handle();
        boolean leftDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        boolean insidePreview = bedrockskins$isMouseOverPreview(mouseX, mouseY);
        
        // Prevent clicking the button from initiating a drag
        boolean insideButton = bedrockskins$openSkinButton != null && bedrockskins$openSkinButton.isHovered();

        if (leftDown && !bedrockskins$leftMouseDown && insidePreview && !insideButton) {
            bedrockskins$draggingPreview = true;
            bedrockskins$lastMouseX = mouseX;
        }
        bedrockskins$leftMouseDown = leftDown;

        if (!leftDown) bedrockskins$draggingPreview = false;

        if (bedrockskins$draggingPreview) {
            double deltaX = mouseX - bedrockskins$lastMouseX;
            bedrockskins$previewYaw -= (float) deltaX * 1.6F;
        }
        bedrockskins$lastMouseX = mouseX;

        bedrockskins$menuPreviewPlayer.tickCount = (int) (Util.getMillis() / 50L);
        GuiUtils.renderEntityInRect(guiGraphics, bedrockskins$menuPreviewPlayer, bedrockskins$previewYaw, 
            bedrockskins$getLeft(), bedrockskins$getTop() - 14, bedrockskins$getLeft() + PREVIEW_W, bedrockskins$getTop() + PREVIEW_H, 56);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void bedrockskins$cleanupMainMenuPreview(CallbackInfo ci) {
        PreviewPlayer.PreviewPlayerPool.remove(bedrockskins$menuPreviewUuid);
        SkinManager.resetPreviewSkin(bedrockskins$menuPreviewUuid);
        bedrockskins$menuPreviewPlayer = null;
        bedrockskins$openSkinButton = null;
        bedrockskins$draggingPreview = false;
    }
}