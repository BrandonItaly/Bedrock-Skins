package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$disableBobView(CallbackInfo ci) {
        if (!(Minecraft.getInstance().getCameraEntity() instanceof Player player)) return;
        
        SkinId skinId = SkinManager.getSkin(player.getUUID());
        if (skinId == null) return;
        
        BedrockPlayerModel model = BedrockModelManager.getModel(skinId);
        if (model != null && model.isStationaryLegs()) {
            ci.cancel();
        }
    }
}