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

    private static final Identifier VANILLA_ELYTRA = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");

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
            ClientAsset.Texture bodyAsset = current.body();
            ClientAsset.Texture capeAsset = current.cape();
            ClientAsset.Texture elytraAsset = current.elytra();
            boolean modified = false;

            // Fetch the skin ID, if none exists we keep original body
            SkinId skinId = SkinManager.getSkin(uuid);
            var loadedSkin = skinId != null ? SkinPackLoader.getLoadedSkin(skinId) : null;
            if (loadedSkin != null && loadedSkin.identifier != null) {
                bodyAsset = new ClientAsset.ResourceTexture(loadedSkin.identifier, loadedSkin.identifier);
                modified = true;
            }

            // Overwrite cape and elytra using centralized priority logic
            boolean isLocalPlayer = (net.minecraft.client.Minecraft.getInstance().player != null && uuid.equals(net.minecraft.client.Minecraft.getInstance().player.getUUID()));
            SkinManager.ResolvedCape resolved = SkinManager.resolveCape(uuid, loadedSkin, isLocalPlayer);
            if (resolved != null) {
                if (resolved.capeId.equals(SkinManager.CAPE_NONE)) {
                    capeAsset = null;
                    elytraAsset = null;
                } else {
                    capeAsset = new ClientAsset.ResourceTexture(resolved.capeId, resolved.capeId);
                    boolean isMojangCape = resolved.capeId.getNamespace().equals("bedrockskins") && resolved.capeId.getPath().startsWith("capes/mojang/");
                    if (isMojangCape) {
                        elytraAsset = capeAsset;
                    } else {
                        elytraAsset = current.elytra() != null ? current.elytra() : new ClientAsset.ResourceTexture(VANILLA_ELYTRA, VANILLA_ELYTRA);
                    }
                }
                modified = true;
            }

            if (modified) {
                PlayerSkin replaced = new PlayerSkin(bodyAsset, capeAsset, elytraAsset, current.model(), current.secure());
                return ((PlayerSkinRenderCache) (Object) this).new RenderInfo(base.gameProfile(), replaced, profile.skinPatch());
            }

            return base;
        });
    }
}