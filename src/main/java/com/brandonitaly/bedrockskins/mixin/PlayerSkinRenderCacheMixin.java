package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.SkinManager;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinRenderCache.class)
public abstract class PlayerSkinRenderCacheMixin {

    @Inject(method = "createLookup", at = @At("RETURN"), cancellable = true)
    private void bedrockSkins$overridePlayerGlyphLookup(ResolvableProfile profile,
            CallbackInfoReturnable<Supplier<PlayerSkinRenderCache.RenderInfo>> cir) {
        Supplier<PlayerSkinRenderCache.RenderInfo> original = cir.getReturnValue();

        cir.setReturnValue(() -> {
            PlayerSkinRenderCache.RenderInfo base = original.get();
            if (base == null)
                return null;

            UUID uuid = profile.partialProfile().id();
            if (uuid == null || (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L)) {
                return base;
            }

            PlayerSkin current = base.playerSkin();
            PlayerSkin replaced = SkinManager.applySkinOverrides(uuid, current);
            if (replaced != current) {
                return ((PlayerSkinRenderCache) (Object) this).new RenderInfo(base.gameProfile(), replaced, profile.skinPatch());
            }

            return base;
        });
    }
}
