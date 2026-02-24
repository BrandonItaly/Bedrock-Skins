package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$disableBobView(PoseStack matrices, float partialTicks, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.getCameraEntity() instanceof Player player) {
            SkinId skinId = SkinManager.getSkin(player.getUUID());
            if (skinId != null) {
                BedrockPlayerModel model = BedrockModelManager.getModel(skinId);
                if (model != null && model.isStationaryLegs()) {
                    ci.cancel();
                }
            }
        }
    }
}
