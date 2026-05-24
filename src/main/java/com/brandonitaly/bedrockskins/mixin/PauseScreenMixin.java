package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.gui.PaperDollHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    //~ if >=26.1 'render' -> 'extractRenderState' {
    @Inject(method = "extractRenderState", at = @At("TAIL"))//~}
    private void bedrockskins$renderPausePreview(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (bedrockskins$helper != null && BedrockSkinsConfig.isShowPaperDollOnPauseScreen()) {
            bedrockskins$helper.render(guiGraphics, mouseX, mouseY, this.width, this.height, this.font, this.minecraft);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (bedrockskins$helper != null) bedrockskins$helper.removed();
    }
}