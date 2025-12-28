package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntry {

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        GameProfile profile = getProfile();
        java.util.UUID id = null;
        try {
            java.lang.reflect.Field f = profile.getClass().getDeclaredField("id");
            f.setAccessible(true);
            Object o = f.get(profile);
            if (o instanceof java.util.UUID) id = (java.util.UUID) o;
            else if (o instanceof String) id = java.util.UUID.fromString((String) o);
        } catch (Exception ignored) {
            // fallback: try common getters
            try {
                java.lang.reflect.Method m = profile.getClass().getMethod("getId");
                Object o = m.invoke(profile);
                if (o instanceof java.util.UUID) id = (java.util.UUID) o;
                else if (o instanceof String) id = java.util.UUID.fromString((String) o);
            } catch (Exception ignored2) { }
        }
        if (id == null) return;
        String uuid = id.toString();
        String skinKey = SkinManager.getSkin(uuid);
        if (skinKey == null) return;
        var loadedSkin = SkinPackLoader.loadedSkins.get(skinKey);
        if (loadedSkin == null) return;

        if (loadedSkin.capeIdentifier != null) {
            SkinTextures original = cir.getReturnValue();
            Identifier capeId = loadedSkin.capeIdentifier;

            // Create AssetInfo.TextureAsset for the cape
            AssetInfo.TextureAsset capeAsset = new AssetInfo.TextureAssetInfo(capeId, capeId);

            // Preserve original elytra texture:
            // If explicit elytra exists, use it.
            // If not, check if a cape existed (which would have been used as elytra).
            // If neither, use the default elytra texture to prevent the Bedrock cape from being used as elytra.
            Identifier defaultElytraId = Identifier.of("minecraft", "textures/entity/equipment/wings/elytra.png");
            AssetInfo.TextureAsset defaultElytraAsset = new AssetInfo.TextureAssetInfo(defaultElytraId, defaultElytraId);

            AssetInfo.TextureAsset elytraAsset = original.elytra() != null ? original.elytra() : (original.cape() != null ? original.cape() : defaultElytraAsset);

            SkinTextures newTextures = new SkinTextures(
                original.body(),
                capeAsset,
                elytraAsset,
                original.model(),
                original.secure()
            );
            cir.setReturnValue(newTextures);
        }
    }
}
