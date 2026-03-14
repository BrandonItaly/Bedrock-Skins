package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Shadow
    protected EntityModel model;

    @Unique
    private Object originalModel;

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$swapModel(LivingEntityRenderState state) {
        if (!(state instanceof AvatarRenderState) || !(state instanceof BedrockRenderStateAccessor skinState)) return;
        
        java.util.UUID uuid = skinState.bedrockSkins$getUniqueId();
        if (uuid == null) return;
        
        var skinId = SkinManager.getSkin(uuid.toString());
        if (skinId == null) return;
        
        BedrockPlayerModel bedrockModel = BedrockModelManager.getModel(skinId);
        if (bedrockModel == null) return;

        this.originalModel = this.model;
        this.model = bedrockModel;
        
        if (this.originalModel instanceof net.minecraft.client.model.player.PlayerModel playerModel) {
            bedrockModel.copyFromVanilla(playerModel);
        }
    }

    @Unique
    private void bedrockSkins$restoreModel() {
        if (this.originalModel != null) {
            this.model = (EntityModel) this.originalModel;
            this.originalModel = null;
        }
    }

    // --- Injectors ---

    @Inject(method = "submit", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci) {
        this.bedrockSkins$swapModel(state);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci) {
        this.bedrockSkins$restoreModel();
    }
}