package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.gui.PaperDollHelper;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

        //? if >=26.2 {
        if (!BedrockSkinsConfig.isShowPaperDollOnMainMenu()) {
            List<AbstractWidget> iconButtons = new ArrayList<>();
            int topPos = this.height / 4 + 48 + 24;
            for (var listener : this.children()) {
                if (listener instanceof AbstractWidget widget) {
                    if (widget.getWidth() == 20 && widget.getHeight() == 20 && widget.getY() > this.height / 2) {
                        iconButtons.add(widget);
                        topPos = widget.getY();
                    }
                }
            }

            iconButtons.sort(Comparator.comparingInt(AbstractWidget::getX));

            int k = iconButtons.size();
            int numberOfButtons = k + 1;
            int buttonWidth = 20;
            int spacing = 4;
            int totalWidth = numberOfButtons * buttonWidth + (numberOfButtons - 1) * spacing;
            int startX = this.width / 2 - totalWidth / 2;

            for (int i = 0; i < k; i++) {
                iconButtons.get(i).setX(startX + i * (buttonWidth + spacing));
            }

            int btnX = startX + k * (buttonWidth + spacing);
            
            SpriteIconButton button = SpriteIconButton.builder(
                Component.empty(),
                b -> this.minecraft.gui.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(this)),
                true
            )
            .size(20, 20)
            .sprite(BedrockSkinsSprites.MY_CHARACTERS_ICON, 16, 16)
            .build();
            button.setX(btnX);
            button.setY(topPos);
            button.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.change_skin.tooltip")));
            this.addRenderableWidget(button);
            return;
        }
        //?} else {
        /*if (!BedrockSkinsConfig.isShowPaperDollOnMainMenu()) return;
        *///?}
        
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
            bedrockskins$helper.extractRenderState(guiGraphics, mouseX, mouseY, this.width, this.height, this.font, this.minecraft);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (bedrockskins$helper != null) bedrockskins$helper.removed();
    }
}