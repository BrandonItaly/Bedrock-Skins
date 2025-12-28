package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {
    @Shadow
    public EntityModel model;
    @Unique
    private Object originalModel;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState) {
            BedrockSkinState skinState = (state instanceof BedrockSkinState) ? (BedrockSkinState) state : null;
            java.util.UUID uuid = (skinState != null) ? skinState.getUniqueId() : null;
            if (uuid != null) {
                var bedrockModel = BedrockModelManager.getModel(uuid);
                if (bedrockModel != null) {
                    originalModel = this.model;
                    this.model = (EntityModel) bedrockModel;
                    if (originalModel instanceof net.minecraft.client.render.entity.model.PlayerEntityModel) {
                        bedrockModel.copyFromVanilla((net.minecraft.client.render.entity.model.PlayerEntityModel) originalModel);
                    }
                }
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (originalModel != null) {
            this.model = (EntityModel) originalModel;
            originalModel = null;
        }
    }
}
