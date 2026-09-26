package io.github.brandonitaly.bedrockskins.mixin.gui;

import io.github.brandonitaly.bedrockskins.gui.screen.FullScreenPreviewScreen;
import io.github.brandonitaly.bedrockskins.gui.preview.HeadPreviewButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntry.class)
public abstract class SocialPlayerEntryMixin {
    @Unique private Button bedrockskins$previewButton;

    @Unique
    private Button bedrockskins$headButton() {
        PlayerEntry entry = (PlayerEntry) (Object) this;
        if (bedrockskins$previewButton == null) {
            Component label = Component.translatable("bedrockskins.gui.open_player_preview");
            bedrockskins$previewButton = new HeadPreviewButton(label, button -> {
                Minecraft client = Minecraft.getInstance();
                client.gui.setScreen(new FullScreenPreviewScreen(client.gui.screen(),
                    entry.getPlayerId(), entry.getPlayerName(), entry.getSkinGetter()));
            });
            bedrockskins$previewButton.setTooltip(Tooltip.create(label));
        }
        bedrockskins$previewButton.setX(entry.getContentX() + 4);
        bedrockskins$previewButton.setY(entry.getContentY() + (entry.getContentHeight() - 24) / 2);
        return bedrockskins$previewButton;
    }

    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void bedrockskins$addHeadClick(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
        List<GuiEventListener> children = new ArrayList<>(cir.getReturnValue());
        children.addFirst(bedrockskins$headButton());
        cir.setReturnValue(children);
    }

    @Inject(method = "narratables", at = @At("RETURN"), cancellable = true)
    private void bedrockskins$addHeadNarration(CallbackInfoReturnable<List<? extends NarratableEntry>> cir) {
        List<NarratableEntry> entries = new ArrayList<>(cir.getReturnValue());
        entries.addFirst(bedrockskins$headButton());
        cir.setReturnValue(entries);
    }

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void bedrockskins$drawHeadButton(GuiGraphicsExtractor gui, int mouseX, int mouseY,
                                           boolean hovered, float delta, CallbackInfo ci) {
        bedrockskins$headButton().extractRenderState(gui, mouseX, mouseY, delta);
    }

}
