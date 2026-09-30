package io.github.brandonitaly.bedrockskins.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.brandonitaly.bedrockskins.client.render.model.ElytraAttachmentPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
    @WrapOperation(method = "submit", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void bedrockSkins$attachWings(PoseStack matrices, float x, float y, float z,
            Operation<Void> original, @Local(argsOnly = true) HumanoidRenderState state) {
        var parent = ((RenderLayer<?, ?>) (Object) this).getParentModel();
        if (ElytraAttachmentPose.applies(state) && parent instanceof HumanoidModel<?> model) {
            ElytraAttachmentPose.apply(matrices, model);
            // Vanilla's wing angles already include the normal crouching lean.
            // Retain only the torso's additional skin/emote rotation.
            if (state.isCrouching) {
                //? if >=26.3 {
                /*matrices.rotate(Axis.XP, -0.5F);*/
                //?} else {
                matrices.mulPose(Axis.XP.rotation(-0.5F));
                //?}
                // The torso already supplies vanilla's three-model-unit crouch offset.
                y -= 3.0F / 16.0F;
            }
        }
        original.call(matrices, x, y, z);
    }
}
