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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;*/
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    //? if >=1.21.11 {
    @Unique
    private static final Identifier BEDROCKSKINS_MENU_BUTTON_SPRITE = Identifier.fromNamespaceAndPath("bedrockskins", "container/icon_hangar");
    //?} else {
    /*@Unique
    private static final ResourceLocation BEDROCKSKINS_MENU_BUTTON_SPRITE = ResourceLocation.fromNamespaceAndPath("bedrockskins", "container/icon_hangar");*/
    //?}

    @Unique
    private PreviewPlayer bedrockskins$menuPreviewPlayer;
    @Unique
    private UUID bedrockskins$menuPreviewUuid = UUID.randomUUID();
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
    private void bedrockskins$initMainMenuPreview(CallbackInfo ci) {
        if (minecraft == null) return;
        if (!BedrockSkinsConfig.isShowPaperDollOnMainMenu()) {
            if (bedrockskins$openSkinButton != null) bedrockskins$openSkinButton.visible = false;
            return;
        }

        var accountProfile = minecraft.getGameProfile();
        if (accountProfile != null && accountProfile.id() != null) {
            bedrockskins$menuPreviewUuid = UUID.randomUUID();
        } else {
            bedrockskins$menuPreviewUuid = UUID.randomUUID();
        }

        String previewName = accountProfile != null && accountProfile.name() != null
            ? accountProfile.name()
                : "Preview";
        GameProfile previewProfile = new GameProfile(bedrockskins$menuPreviewUuid, previewName);
        bedrockskins$menuPreviewPlayer = PreviewPlayer.PreviewPlayerPool.get(previewProfile);
        bedrockskins$menuPreviewPlayer.setShowNameTag(true);
        bedrockskins$menuPreviewPlayer.setCustomName(Component.literal(previewName));
        bedrockskins$menuPreviewPlayer.setCustomNameVisible(true);

        int previewX = bedrockskins$getPreviewLeft();
        int previewY = bedrockskins$getPreviewTop();
        int buttonX = bedrockskins$getButtonX();
        int buttonY = bedrockskins$getButtonY();

        bedrockskins$openSkinButton = Button.builder(net.minecraft.network.chat.Component.empty(), b -> {
                    minecraft.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(this));
                })
                .bounds(buttonX, buttonY, BEDROCKSKINS_BUTTON_SIZE, BEDROCKSKINS_BUTTON_SIZE)
                .build();
        addRenderableWidget(bedrockskins$openSkinButton);

        bedrockskins$applyCurrentEquippedPreviewBehavior();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockskins$renderMainMenuPreview(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!BedrockSkinsConfig.isShowPaperDollOnMainMenu()) {
            if (bedrockskins$openSkinButton != null) bedrockskins$openSkinButton.visible = false;
            return;
        }
        if (bedrockskins$menuPreviewPlayer == null) return;

        int left = bedrockskins$getPreviewLeft();
        int top = bedrockskins$getPreviewTop();
        int right = left + BEDROCKSKINS_PREVIEW_WIDTH;
        int bottom = top + BEDROCKSKINS_PREVIEW_HEIGHT;

        long window = minecraft != null ? minecraft.getWindow().handle() : 0L;
        boolean leftDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (!leftDown) {
            bedrockskins$draggingPreview = false;
        }

        if (bedrockskins$draggingPreview && leftDown) {
            double deltaX = mouseX - bedrockskins$lastMouseX;
            bedrockskins$previewYaw -= (float) deltaX * BEDROCKSKINS_ROTATION_SENSITIVITY;
        }
        bedrockskins$lastMouseX = mouseX;

        bedrockskins$applyCurrentEquippedPreviewBehavior();
        bedrockskins$menuPreviewPlayer.tickCount = (int) (Util.getMillis() / 50L);
        int namePaddingTop = 14;
        GuiUtils.renderEntityInRect(guiGraphics, bedrockskins$menuPreviewPlayer, bedrockskins$previewYaw, left, top - namePaddingTop, right, bottom, 56);

        if (bedrockskins$openSkinButton != null && bedrockskins$openSkinButton.visible) {
            int buttonX = bedrockskins$getButtonX();
            int buttonY = bedrockskins$getButtonY();
            bedrockskins$openSkinButton.setX(buttonX);
            bedrockskins$openSkinButton.setY(buttonY);
            int iconX = bedrockskins$openSkinButton.getX() + 2;
            int iconY = bedrockskins$openSkinButton.getY() + 2;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BEDROCKSKINS_MENU_BUTTON_SPRITE, iconX, iconY, 16, 16);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bedrockskins$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;

        int left = bedrockskins$getPreviewLeft();
        int top = bedrockskins$getPreviewTop();
        int right = left + BEDROCKSKINS_PREVIEW_WIDTH;
        int bottom = top + BEDROCKSKINS_PREVIEW_HEIGHT;

        int buttonX = bedrockskins$getButtonX();
        int buttonY = bedrockskins$getButtonY();
        int buttonRight = buttonX + BEDROCKSKINS_BUTTON_SIZE;
        int buttonBottom = buttonY + BEDROCKSKINS_BUTTON_SIZE;
        if (event.x() >= buttonX && event.x() <= buttonRight && event.y() >= buttonY && event.y() <= buttonBottom) {
            return;
        }

        if (event.x() >= left && event.x() <= right && event.y() >= top && event.y() <= bottom) {
            bedrockskins$draggingPreview = true;
            bedrockskins$lastMouseX = event.x();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void bedrockskins$cleanupMainMenuPreview(CallbackInfo ci) {
        if (bedrockskins$menuPreviewUuid != null) {
            PreviewPlayer.PreviewPlayerPool.remove(bedrockskins$menuPreviewUuid);
            SkinManager.resetPreviewSkin(bedrockskins$menuPreviewUuid.toString());
        }
        bedrockskins$menuPreviewPlayer = null;
        bedrockskins$openSkinButton = null;
        bedrockskins$draggingPreview = false;
    }

    @Unique
    private void bedrockskins$applyCurrentEquippedPreviewBehavior() {
        if (bedrockskins$menuPreviewPlayer == null || minecraft == null) return;

        SkinId selectedSkinId = SkinManager.getLocalSelectedKey();
        if (selectedSkinId != null) {
            SkinManager.setPreviewSkin(
                    bedrockskins$menuPreviewUuid.toString(),
                    selectedSkinId.getPack(),
                    selectedSkinId.getName()
            );
            SkinPackLoader.registerTextureFor(selectedSkinId);
            bedrockskins$menuPreviewPlayer.clearForcedProfileSkin();
            bedrockskins$menuPreviewPlayer.clearForcedBody();
            bedrockskins$menuPreviewPlayer.clearForcedCape();
            bedrockskins$menuPreviewPlayer.setUseLocalPlayerModel(false);

            LoadedSkin selectedSkin = SkinPackLoader.getLoadedSkin(selectedSkinId);
            if (selectedSkin != null) {
                bedrockskins$menuPreviewPlayer.setForcedCape(selectedSkin.capeIdentifier);
            }
            return;
        }

        SkinManager.resetPreviewSkin(bedrockskins$menuPreviewUuid.toString());
        if (minecraft.player != null) {
            bedrockskins$menuPreviewPlayer.clearForcedProfileSkin();
            bedrockskins$menuPreviewPlayer.setForcedBody(minecraft.player.getSkin().body());
            bedrockskins$menuPreviewPlayer.setForcedCapeTexture(minecraft.player.getSkin().cape());
            bedrockskins$menuPreviewPlayer.setUseLocalPlayerModel(true);
        } else {
            bedrockskins$menuPreviewPlayer.clearForcedBody();
            bedrockskins$menuPreviewPlayer.clearForcedCape();
            var profile = minecraft.getGameProfile();
            if (profile != null) {
                bedrockskins$menuPreviewPlayer.setForcedProfileSkin(
                        minecraft.getSkinManager().createLookup(profile, false).get()
                );
            } else {
                bedrockskins$menuPreviewPlayer.clearForcedProfileSkin();
            }
            bedrockskins$menuPreviewPlayer.setUseLocalPlayerModel(false);
        }
    }
}