package io.github.brandonitaly.bedrockskins.mixin.skin;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinRenderCache.class)
public abstract class PlayerSkinRenderCacheMixin {

    //? if >=26.4-snapshot-5 {
    /*@Inject(method = "getOrDefault", at = @At("RETURN"), cancellable = true)
    private void bedrockskins$overridePlayerFace(ResolvableProfile profile,
            CallbackInfoReturnable<PlayerSkinRenderCache.RenderInfo> cir) {
        cir.setReturnValue(bedrockskins$equippedSkin(profile, cir.getReturnValue()));
    }
    *///?}

    @Inject(method = "createLookup", at = @At("RETURN"), cancellable = true)
    private void bedrockSkins$overridePlayerGlyphLookup(ResolvableProfile profile,
            CallbackInfoReturnable<Supplier<PlayerSkinRenderCache.RenderInfo>> cir) {
        Supplier<PlayerSkinRenderCache.RenderInfo> original = cir.getReturnValue();

        cir.setReturnValue(() -> bedrockskins$equippedSkin(profile, original.get()));
    }

    @Unique
    private PlayerSkinRenderCache.RenderInfo bedrockskins$equippedSkin(ResolvableProfile profile,
            PlayerSkinRenderCache.RenderInfo base) {
        if (base == null) return null;

        UUID uuid = profile.partialProfile().id();
        if (uuid == null || (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L)) {
            uuid = base.gameProfile().id();
        }
        if (uuid == null || (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L)) return base;

        var skinId = SkinManager.getSkin(uuid);
        if (skinId != null) SkinPackLoader.registerTextureFor(skinId);
        PlayerSkin current = base.playerSkin();
        PlayerSkin replaced = SkinManager.applySkinOverrides(uuid, current);
        return replaced == current ? base
            : ((PlayerSkinRenderCache) (Object) this).new RenderInfo(base.gameProfile(), replaced, profile.skinPatch());
    }
}
