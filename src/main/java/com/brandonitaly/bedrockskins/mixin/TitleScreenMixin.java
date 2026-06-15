package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.gui.PaperDollHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) { super(title); }

    @Unique
    private PaperDollHelper bedrockskins$helper;

    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockskins$initMainMenuPreview(CallbackInfo ci) {
        if (bedrockskins$helper != null) {
            bedrockskins$helper.removed();
            bedrockskins$helper = null;
        }

        if (!BedrockSkinsConfig.isShowPaperDollOnMainMenu()) return;
        
        bedrockskins$helper = new PaperDollHelper(this, true);
        addRenderableWidget(bedrockskins$helper.init(this.minecraft, this.width, this.height));
    }

    //~ if >=26.1 'render' -> 'extractRenderState' {
    @Inject(method = "extractRenderState", at = @At("HEAD"))//~}
    private void bedrockskins$updateMainMenuLayout(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (bedrockskins$helper != null && BedrockSkinsConfig.isShowPaperDollOnMainMenu()) {
            bedrockskins$helper.updateLayout(this.width, this.height);
        }
    }

    //~ if >=26.1 'render' -> 'extractRenderState' {
    @Inject(method = "extractRenderState", at = @At("TAIL"))//~}
    private void bedrockskins$renderMainMenuPreview(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (bedrockskins$helper != null && BedrockSkinsConfig.isShowPaperDollOnMainMenu()) {
            bedrockskins$helper.render(guiGraphics, mouseX, mouseY, this.width, this.height, this.font, this.minecraft);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (bedrockskins$helper != null) bedrockskins$helper.removed();
    }
}