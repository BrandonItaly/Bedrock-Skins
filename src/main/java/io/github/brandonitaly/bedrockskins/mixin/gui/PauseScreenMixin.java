package io.github.brandonitaly.bedrockskins.mixin.gui;

import io.github.brandonitaly.bedrockskins.client.persistence.BedrockSkinsConfig;
import io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import io.github.brandonitaly.bedrockskins.gui.preview.PaperDollWidget;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.llamalad7.mixinextras.sugar.Local;
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
    private PaperDollWidget bedrockskins$dollWidget;

    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockskins$initPausePreview(CallbackInfo ci) {
        if (bedrockskins$dollWidget != null) {
            bedrockskins$dollWidget.removed();
            bedrockskins$dollWidget = null;
        }

        if (!((PauseScreen) (Object) this).showsPauseMenu() || !BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) return;

        int x = PaperDollWidget.getDefaultLeft(this, this.width);
        int y = PaperDollWidget.getDefaultTop(this, this.height);
        bedrockskins$dollWidget = new PaperDollWidget(x, y, this, false);
        this.addRenderableWidget(bedrockskins$dollWidget);
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
        .sprite(BedrockSkinsSprites.WARDROBE_ICON, 16, 16)
        .build();
        button.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.wardrobe.tooltip")));
        iconButtonRow.addChild(button);
    }
    //?}

    @Override
    public void removed() {
        super.removed();
        if (bedrockskins$dollWidget != null) bedrockskins$dollWidget.removed();
    }
}
