package io.github.brandonitaly.bedrockskins.mixin.skin;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.HeadIconTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.PlayerSkinRenderCache$RenderInfo")
public abstract class PlayerHeadGlyphMixin {
    @Shadow public abstract PlayerSkin playerSkin();

    @Inject(method = "textureView", at = @At("HEAD"), cancellable = true)
    private void bedrockskins$headTexture(CallbackInfoReturnable<Object> callback) {
        var source = playerSkin().body().texturePath();
        var icon = HeadIconTextures.resolve(source, true);
        if (!icon.equals(source)) callback.setReturnValue(Minecraft.getInstance().getTextureManager().getTexture(icon).getTextureView());
    }
    @Inject(method = "glyphRenderTypes", at = @At("HEAD"), cancellable = true)
    private void bedrockskins$headGlyph(CallbackInfoReturnable<GlyphRenderTypes> callback) {
        var source = playerSkin().body().texturePath();
        var icon = HeadIconTextures.resolve(source, true);
        if (!icon.equals(source)) callback.setReturnValue(GlyphRenderTypes.createForColorTexture(icon));
    }
}
