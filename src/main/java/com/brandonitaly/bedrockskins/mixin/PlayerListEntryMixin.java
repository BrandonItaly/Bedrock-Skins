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

    private static final Identifier VANILLA_ELYTRA = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
        UUID id = getProfile().id();
        if (id == null) return;

        PlayerSkin original = cir.getReturnValue();
        ClientAsset.Texture bodyAsset = original.body();
        ClientAsset.Texture capeAsset = original.cape();
        ClientAsset.Texture elytraAsset = original.elytra();
        boolean modified = false;

        // Fetch the skin ID, if none exists we keep original body
        SkinId skinId = SkinManager.getSkin(id);
        var loadedSkin = skinId != null ? SkinPackLoader.getLoadedSkin(skinId) : null;
        if (loadedSkin != null && loadedSkin.identifier != null) {
            bodyAsset = new ClientAsset.ResourceTexture(loadedSkin.identifier, loadedSkin.identifier);
            modified = true;
        }

        // Overwrite cape and elytra using centralized priority logic
        boolean isLocalPlayer = (net.minecraft.client.Minecraft.getInstance().player != null && id.equals(net.minecraft.client.Minecraft.getInstance().player.getUUID()));
        SkinManager.ResolvedCape resolved = SkinManager.resolveCape(id, loadedSkin, isLocalPlayer);
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
                    elytraAsset = original.elytra() != null ? original.elytra() : new ClientAsset.ResourceTexture(VANILLA_ELYTRA, VANILLA_ELYTRA);
                }
            }
            modified = true;
        }

        // Apply the newly assembled skin if modified
        if (modified) {
            cir.setReturnValue(new PlayerSkin(bodyAsset, capeAsset, elytraAsset, original.model(), original.secure()));
        }
    }
}