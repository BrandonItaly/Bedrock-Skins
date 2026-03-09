package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
        UUID id = getProfile().id();
        if (id == null) return;

        // Fetch the skin ID, exit early if none exists
        SkinId skinId = SkinManager.getSkin(id.toString());
        if (skinId == null) return;

        // Fetch the loaded skin, exit early if it failed to load
        var loadedSkin = SkinPackLoader.getLoadedSkin(skinId);
        if (loadedSkin == null) return;

        // Start with the original vanilla assets
        PlayerSkin original = cir.getReturnValue();
        ClientAsset.Texture bodyAsset = original.body();
        ClientAsset.Texture capeAsset = original.cape();
        ClientAsset.Texture elytraAsset = original.elytra();

        // Overwrite body if a custom Bedrock body exists
        if (loadedSkin.identifier != null) {
            bodyAsset = new ClientAsset.ResourceTexture(loadedSkin.identifier, loadedSkin.identifier);
        }

        // Overwrite cape and elytra if a custom Bedrock cape exists
        if (loadedSkin.capeIdentifier != null) {
            capeAsset = new ClientAsset.ResourceTexture(loadedSkin.capeIdentifier, loadedSkin.capeIdentifier);
            Identifier elytraId = Identifier.withDefaultNamespace("textures/entity/equipment/wings/elytra.png");
            elytraAsset = new ClientAsset.ResourceTexture(elytraId, elytraId);
        }

        // Apply the newly assembled skin
        cir.setReturnValue(new PlayerSkin(bodyAsset, capeAsset, elytraAsset, original.model(), original.secure()));
    }
}