package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class MixinPlayerListEntry {

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
        GameProfile profile = getProfile();
        java.util.UUID id = profile.id();
        if (id == null) return;

        //? if >=1.21.11 {
        Identifier capeId = null;
        //?} else {
        /*ResourceLocation capeId = null;*/
        //?}

        // Check for a Custom Bedrock Skin Cape first
        SkinId skinId = SkinManager.getSkin(id.toString());
        if (skinId != null) {
            var loadedSkin = SkinPackLoader.getLoadedSkin(skinId);
            if (loadedSkin != null) {
                capeId = loadedSkin.capeIdentifier;
            }
        }

        // Apply modifications if a cape was found from either source
        PlayerSkin original = cir.getReturnValue();

        ClientAsset.Texture capeAsset;
        ClientAsset.Texture elytraAsset;

        //? if >=1.21.11 {
        Identifier elytraId = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");
        //?} else {
        /*ResourceLocation elytraId = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");*/
        //?}

        // Priority logic for cape and elytra selection
        boolean hasBedrockCape = skinId != null && capeId != null;
        boolean hasVanillaCape = original.cape() != null;

        if (hasBedrockCape) {
            capeAsset = new ClientAsset.ResourceTexture(capeId, capeId);
            elytraAsset = new ClientAsset.ResourceTexture(elytraId, elytraId);
        } else if (hasVanillaCape) {
            capeAsset = original.cape();
            elytraAsset = original.elytra();
        } else {
            capeAsset = null;
            elytraAsset = null;
        }

        // Use Bedrock skin texture as body if available
        ClientAsset.Texture bodyAsset = original.body();
        if (skinId != null) {
            var loadedSkin = SkinPackLoader.getLoadedSkin(skinId);
            if (loadedSkin != null && loadedSkin.identifier != null) {
                bodyAsset = new ClientAsset.ResourceTexture(loadedSkin.identifier, loadedSkin.identifier);
            }
        }
        cir.setReturnValue(new PlayerSkin(bodyAsset, capeAsset, elytraAsset, original.model(), original.secure()));
    }
}