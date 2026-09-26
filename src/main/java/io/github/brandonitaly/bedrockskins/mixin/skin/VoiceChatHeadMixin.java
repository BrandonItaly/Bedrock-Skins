package io.github.brandonitaly.bedrockskins.mixin.skin;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.HeadIconTextures;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Simple Voice Chat draws face UVs directly in its group HUD and player lists. */
@Pseudo
@Mixin(targets = "de.maxhenkel.voicechat.gui.GameProfileUtils", remap = false)
public abstract class VoiceChatHeadMixin {
    @Inject(method = "getSkin(Ljava/util/UUID;)Lnet/minecraft/world/entity/player/PlayerSkin;",
        at = @At("RETURN"), cancellable = true, require = 0)
    private static void bedrockskins$portrait(UUID uuid, CallbackInfoReturnable<PlayerSkin> callback) {
        PlayerSkin skin = callback.getReturnValue();
        var source = skin.body().texturePath();
        var icon = HeadIconTextures.resolve(source, true);
        if (icon.equals(source)) return;
        // The portrait includes the hat; the atlas's separate hat rectangle is transparent.
        callback.setReturnValue(new PlayerSkin(new ClientAsset.ResourceTexture(icon, icon),
            skin.cape(), skin.elytra(), skin.model(), skin.secure()));
    }
}
