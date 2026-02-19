package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WingsLayer.class)
public abstract class MixinWingsLayer {

    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    @Inject(method = "submit", at = @At("HEAD"))
    private void bedrockSkins$beforeRender(PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (!(state instanceof BedrockSkinState skinState)) return;

        SkinId skinId = skinState.getBedrockSkinId();
        if (skinId == null) {
            UUID uuid = skinState.getUniqueId();
            skinId = uuid == null ? null : SkinManager.getSkin(uuid.toString());
        }
        if (skinId == null) return;

        BedrockPlayerModel model = BedrockModelManager.getModel(skinId);
        if (model == null || model.elytraYOffset == 0f) return;

        matrices.pushPose();
        matrices.translate(0.0, model.elytraYOffset * 0.0625f, 0.0);
        pushed.set(true);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void bedrockSkins$afterRender(PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try {
                matrices.popPose();
            } catch (Exception ignored) {
            }
            pushed.remove();
        }
    }
}
