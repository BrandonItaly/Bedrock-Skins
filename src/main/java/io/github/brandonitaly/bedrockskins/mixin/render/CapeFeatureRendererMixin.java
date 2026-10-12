package io.github.brandonitaly.bedrockskins.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brandonitaly.bedrockskins.client.render.model.ElytraAttachmentPose;
import io.github.brandonitaly.bedrockskins.client.render.model.ResourcePackAnimationPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.resources.Identifier;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Mixin(CapeLayer.class)
public abstract class CapeFeatureRendererMixin {
    @Shadow @Final private HumanoidModel<AvatarRenderState> model;

    @WrapOperation(method = "submit", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"))
    private void bedrockskins$attachCape(PoseStack matrices, Operation<Void> original,
                                        @Local(argsOnly = true) AvatarRenderState state) {
        original.call(matrices);
        var parent = ((RenderLayer<?, ?>) (Object) this).getParentModel();
        if (ResourcePackAnimationPose.hasAnimation(model) && parent instanceof HumanoidModel<?> player) {
            ElytraAttachmentPose.applyResourcePackCorrection(matrices, state, player);
        }
    }

    // Allow capes to use the translucent render layer instead of solid
    @WrapOperation(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
        )
    )
    private RenderType useTranslucentLayer(Identifier texture, Operation<RenderType> original) {
        return RenderTypes.entityTranslucent(texture);
    }
}
