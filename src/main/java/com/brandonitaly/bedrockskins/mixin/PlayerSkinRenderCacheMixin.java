package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinRenderCache.class)
public abstract class PlayerSkinRenderCacheMixin {

    @Inject(method = "createLookup", at = @At("RETURN"), cancellable = true)
    private void bedrockSkins$overridePlayerGlyphLookup(ResolvableProfile profile, CallbackInfoReturnable<Supplier<PlayerSkinRenderCache.RenderInfo>> cir) {
        Supplier<PlayerSkinRenderCache.RenderInfo> original = cir.getReturnValue();

        cir.setReturnValue(() -> {
            PlayerSkinRenderCache.RenderInfo base = original.get();
            if (base == null) return null;

            UUID uuid = profile.partialProfile().id();
            if (uuid == null || (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L)) {
                return base;
            }

            SkinId skinId = SkinManager.getSkin(uuid);
            if (skinId == null) return base;

            var loadedSkin = SkinPackLoader.getLoadedSkin(skinId);
            if (loadedSkin == null || loadedSkin.identifier == null) return base;

            PlayerSkin current = base.playerSkin();
            ClientAsset.Texture bodyAsset = new ClientAsset.ResourceTexture(loadedSkin.identifier, loadedSkin.identifier);

            PlayerSkin replaced = new PlayerSkin(bodyAsset, current.cape(), current.elytra(), current.model(), current.secure());

            return ((PlayerSkinRenderCache) (Object) this).new RenderInfo(base.gameProfile(), replaced, profile.skinPatch());
        });
    }
}