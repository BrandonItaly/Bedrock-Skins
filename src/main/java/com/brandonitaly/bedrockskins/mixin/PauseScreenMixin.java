package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.gui.PaperDollHelper;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) { super(title); }

    @Unique
    private PaperDollHelper bedrockskins$helper;

    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockskins$initPausePreview(CallbackInfo ci) {
        if (bedrockskins$helper != null) {
            bedrockskins$helper.removed();
            bedrockskins$helper = null;
        }

        if (!((PauseScreen) (Object) this).showsPauseMenu() || !BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) return;

        bedrockskins$helper = new PaperDollHelper(this, false);
        addRenderableWidget(bedrockskins$helper.init(this.minecraft, this.width, this.height));
    }

    //? if >=26.2 {
    @Inject(
        method = "createPauseMenu",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/GridLayout;arrangeElements()V"
        )
    )
    private void bedrockskins$insertPauseIconButton(CallbackInfo ci, @Local LinearLayout iconButtonRow) {
        if (BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) return;

        SpriteIconButton button = SpriteIconButton.builder(
            Component.empty(),
            b -> this.minecraft.gui.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(this)),
            true
        )
        .size(20, 20)
        .sprite(BedrockSkinsSprites.MY_CHARACTERS_ICON, 16, 16)
        .build();
        button.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.change_skin.tooltip")));
        iconButtonRow.addChild(button);
    }
    //?}

    //~ if >=26.1 'render' -> 'extractRenderState' {
    @Inject(method = "extractRenderState", at = @At("HEAD"))//~}
    private void bedrockskins$updatePauseMenuLayout(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (bedrockskins$helper != null && BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) {
            bedrockskins$helper.updateLayout(this.width, this.height);
        }
    }

    //~ if >=26.1 'render' -> 'extractRenderState' {
    @Inject(method = "extractRenderState", at = @At("TAIL"))//~}
    private void bedrockskins$renderPausePreview(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (bedrockskins$helper != null && BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) {
            bedrockskins$helper.extractRenderState(guiGraphics, mouseX, mouseY, this.width, this.height, this.font, this.minecraft);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (bedrockskins$helper != null) bedrockskins$helper.removed();
    }
}