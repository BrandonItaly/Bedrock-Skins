package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayer {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$hideArmor(PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (!(state instanceof BedrockSkinState skinState)) return;

        SkinId skinId = skinState.getBedrockSkinId();
        if (skinId == null) {
            UUID uuid = skinState.getUniqueId();
            skinId = uuid == null ? null : SkinManager.getSkin(uuid);
        }
        if (skinId == null) return;

        BedrockPlayerModel model = BedrockModelManager.getModel(skinId);
        if (model != null && model.shouldHideArmor()) {
            ci.cancel();
        }
    }
}
