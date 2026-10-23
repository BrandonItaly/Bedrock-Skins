package io.github.brandonitaly.bedrockskins.mixin.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
//? if >=26.4-snapshot-5 {
/*import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Pseudo
@Mixin(targets = "net.minecraft.client.gui.screens.social.PlayerOptionsScreen")
public abstract class PlayerOptionsScreenMixin {
    //? if >=26.4-snapshot-5 {
    /*@Shadow @Final protected UUID playerId;
    @Shadow @Final private AvatarRenderState playerRenderState;

    @Inject(method = "extractRenderState", at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;skin:Lnet/minecraft/world/entity/player/PlayerSkin;",
        opcode = org.objectweb.asm.Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void bedrockskins$equippedPortrait(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        var skinId = SkinManager.getSkin(playerId);
        if (skinId != null) SkinPackLoader.registerTextureFor(skinId);
        BedrockRenderStateStore.setUniqueId(playerRenderState, playerId);
        BedrockRenderStateStore.setSkinId(playerRenderState, skinId);
        BedrockRenderStateStore.setGuiRender(playerRenderState, true);
        playerRenderState.skin = SkinManager.applySkinOverrides(playerId, playerRenderState.skin);
    }
    *///?}
}
