package io.github.brandonitaly.bedrockskins.mixin.skin;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.HeadIconTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerFaceExtractor.class)
public abstract class PlayerFaceExtractorMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;IIIZZI)V", at = @At("HEAD"), cancellable = true)
    private static void bedrockskins$modelHead(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                                               int size, boolean hat, boolean upsideDown, int color, CallbackInfo callback) {
        Identifier icon = HeadIconTextures.resolve(texture, hat);
        if (icon.equals(texture)) return;
        PlayerFaceExtractor.extractRenderState(graphics, icon, x, y, size, false, upsideDown, color);
        callback.cancel();
    }
}
