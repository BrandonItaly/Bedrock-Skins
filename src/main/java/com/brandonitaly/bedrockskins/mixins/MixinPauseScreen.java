package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.gui.GuiUtils;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;*/
//?}
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(Component title) {
        super(title);
    }

    //? if >=1.21.11 {
    @Unique
    private static final Identifier BEDROCKSKINS_PAUSE_BUTTON_SPRITE = Identifier.fromNamespaceAndPath("bedrockskins", "container/icon_hangar");
    //?} else {
    /*@Unique
    private static final ResourceLocation BEDROCKSKINS_PAUSE_BUTTON_SPRITE = ResourceLocation.fromNamespaceAndPath("bedrockskins", "container/icon_hangar");*/
    //?}

    @Unique
    private PreviewPlayer bedrockskins$pausePreviewPlayer;
    @Unique
    private UUID bedrockskins$pausePreviewUuid = UUID.randomUUID();
    @Unique
    private Button bedrockskins$openSkinButton;
    @Unique
    private static final int BEDROCKSKINS_PREVIEW_WIDTH = 78;
    @Unique
    private static final int BEDROCKSKINS_PREVIEW_HEIGHT = 112;
    @Unique
    private static final int BEDROCKSKINS_BUTTON_SIZE = 20;
    @Unique
    private static final int BEDROCKSKINS_BUTTON_GAP = -8;
    @Unique
    private static final float BEDROCKSKINS_ROTATION_SENSITIVITY = 1.6F;
    @Unique
    private float bedrockskins$previewYaw = 0.0F;
    @Unique
    private boolean bedrockskins$draggingPreview = false;
    @Unique
    private boolean bedrockskins$leftMouseDown = false;
    @Unique
    private double bedrockskins$lastMouseX = 0.0;

    @Unique
    private int bedrockskins$getPreviewLeft() {
        int columnCenterX = BedrockSkinsConfig.isPaperDollLeftSideEnabled()
                ? (this.width / 6)
                : ((this.width * 5) / 6);
        return columnCenterX - (BEDROCKSKINS_PREVIEW_WIDTH / 2);
    }

    @Unique
    private int bedrockskins$getPreviewTop() {
        return (this.height - BEDROCKSKINS_PREVIEW_HEIGHT) / 2;
    }

    @Unique
    private int bedrockskins$getButtonX() {
        int left = bedrockskins$getPreviewLeft();
        return left + (BEDROCKSKINS_PREVIEW_WIDTH - BEDROCKSKINS_BUTTON_SIZE) / 2;
    }

    @Unique
    private int bedrockskins$getButtonY() {
        int top = bedrockskins$getPreviewTop();
        return top + BEDROCKSKINS_PREVIEW_HEIGHT + BEDROCKSKINS_BUTTON_GAP;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockskins$initPausePreview(CallbackInfo ci) {
        if (minecraft == null) return;
        if (!((PauseScreen) (Object) this).showsPauseMenu()) return;
        if (!BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) {
            if (bedrockskins$openSkinButton != null) bedrockskins$openSkinButton.visible = false;
            return;
        }

        var accountProfile = minecraft.getGameProfile();
        bedrockskins$pausePreviewUuid = UUID.randomUUID();
        bedrockskins$draggingPreview = false;
        bedrockskins$leftMouseDown = false;
        SkinManager.resetPreviewSkin(bedrockskins$pausePreviewUuid.toString());

        String previewName = accountProfile != null && accountProfile.name() != null
                ? accountProfile.name()
                : "Preview";
        GameProfile previewProfile = new GameProfile(bedrockskins$pausePreviewUuid, previewName);
        bedrockskins$pausePreviewPlayer = PreviewPlayer.PreviewPlayerPool.get(previewProfile);
        bedrockskins$pausePreviewPlayer.setShowNameTag(true);
        bedrockskins$pausePreviewPlayer.setCustomName(Component.literal(previewName));
        bedrockskins$pausePreviewPlayer.setCustomNameVisible(true);

        int buttonX = bedrockskins$getButtonX();
        int buttonY = bedrockskins$getButtonY();

        bedrockskins$openSkinButton = Button.builder(Component.empty(), b ->
                        minecraft.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(this)))
                .bounds(buttonX, buttonY, BEDROCKSKINS_BUTTON_SIZE, BEDROCKSKINS_BUTTON_SIZE)
                .build();
        this.addRenderableWidget(bedrockskins$openSkinButton);

        bedrockskins$applyCurrentEquippedPreviewBehavior();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockskins$renderPausePreview(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) {
            if (bedrockskins$openSkinButton != null) bedrockskins$openSkinButton.visible = false;
            return;
        }
        if (bedrockskins$pausePreviewPlayer == null) return;

        int left = bedrockskins$getPreviewLeft();
        int top = bedrockskins$getPreviewTop();
        int right = left + BEDROCKSKINS_PREVIEW_WIDTH;
        int bottom = top + BEDROCKSKINS_PREVIEW_HEIGHT;

        long window = minecraft != null ? minecraft.getWindow().handle() : 0L;
        boolean leftDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        int buttonX = bedrockskins$getButtonX();
        int buttonY = bedrockskins$getButtonY();
        int buttonRight = buttonX + BEDROCKSKINS_BUTTON_SIZE;
        int buttonBottom = buttonY + BEDROCKSKINS_BUTTON_SIZE;

        boolean insidePreview = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
        boolean insideButton = mouseX >= buttonX && mouseX <= buttonRight && mouseY >= buttonY && mouseY <= buttonBottom;
        if (leftDown && !bedrockskins$leftMouseDown && insidePreview && !insideButton) {
            bedrockskins$draggingPreview = true;
            bedrockskins$lastMouseX = mouseX;
        }
        bedrockskins$leftMouseDown = leftDown;

        if (!leftDown) {
            bedrockskins$draggingPreview = false;
        }

        if (bedrockskins$draggingPreview && leftDown) {
            double deltaX = mouseX - bedrockskins$lastMouseX;
            bedrockskins$previewYaw -= (float) deltaX * BEDROCKSKINS_ROTATION_SENSITIVITY;
        }
        bedrockskins$lastMouseX = mouseX;

        bedrockskins$applyCurrentEquippedPreviewBehavior();
        bedrockskins$pausePreviewPlayer.tickCount = (int) (Util.getMillis() / 50L);
        int namePaddingTop = 14;
        GuiUtils.renderEntityInRect(guiGraphics, bedrockskins$pausePreviewPlayer, bedrockskins$previewYaw, left, top - namePaddingTop, right, bottom, 56);

        if (bedrockskins$openSkinButton != null && bedrockskins$openSkinButton.visible) {
            bedrockskins$openSkinButton.setX(buttonX);
            bedrockskins$openSkinButton.setY(buttonY);
            int iconX = bedrockskins$openSkinButton.getX() + 2;
            int iconY = bedrockskins$openSkinButton.getY() + 2;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BEDROCKSKINS_PAUSE_BUTTON_SPRITE, iconX, iconY, 16, 16);
        }
    }

    @Unique
    private void bedrockskins$applyCurrentEquippedPreviewBehavior() {
        if (bedrockskins$pausePreviewPlayer == null || minecraft == null) return;

        SkinId selectedSkinId = SkinManager.getLocalSelectedKey();
        if (selectedSkinId != null) {
            SkinManager.setPreviewSkin(
                    bedrockskins$pausePreviewUuid.toString(),
                    selectedSkinId.getPack(),
                    selectedSkinId.getName()
            );
            SkinPackLoader.registerTextureFor(selectedSkinId);
            bedrockskins$pausePreviewPlayer.clearForcedProfileSkin();
            bedrockskins$pausePreviewPlayer.clearForcedBody();
            bedrockskins$pausePreviewPlayer.clearForcedCape();
            bedrockskins$pausePreviewPlayer.setUseLocalPlayerModel(false);

            LoadedSkin selectedSkin = SkinPackLoader.getLoadedSkin(selectedSkinId);
            if (selectedSkin != null) {
                bedrockskins$pausePreviewPlayer.setForcedCape(selectedSkin.capeIdentifier);
            }
            return;
        }

        SkinManager.resetPreviewSkin(bedrockskins$pausePreviewUuid.toString());
        if (minecraft.player != null) {
            bedrockskins$pausePreviewPlayer.clearForcedProfileSkin();
            bedrockskins$pausePreviewPlayer.setForcedBody(minecraft.player.getSkin().body());
            bedrockskins$pausePreviewPlayer.setForcedCapeTexture(minecraft.player.getSkin().cape());
            bedrockskins$pausePreviewPlayer.setUseLocalPlayerModel(true);
        } else {
            bedrockskins$pausePreviewPlayer.clearForcedBody();
            bedrockskins$pausePreviewPlayer.clearForcedCape();
            var profile = minecraft.getGameProfile();
            if (profile != null) {
                bedrockskins$pausePreviewPlayer.setForcedProfileSkin(
                        minecraft.getSkinManager().createLookup(profile, false).get()
                );
            } else {
                bedrockskins$pausePreviewPlayer.clearForcedProfileSkin();
            }
            bedrockskins$pausePreviewPlayer.setUseLocalPlayerModel(false);
        }
    }
}
