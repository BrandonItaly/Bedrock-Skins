package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockRenderStateStore;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Shadow
    protected EntityModel<?> model;

    @Shadow
    protected List<?> layers;

    @Unique
    private Object originalModel;

    @Unique
    private CapeLayer bedrockSkins$capturedCapeLayer;

    // null = unchecked, true = suppressed (no CapeLayer in layers), false = present
    @Unique
    @Nullable
    private Boolean bedrockSkins$capeLayerSuppressed;

    @Inject(method = "addLayer", at = @At("HEAD"))
    private void onAddLayerHead(RenderLayer<?, ?> layer, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) layer instanceof CapeLayer capeLayer) {
            this.bedrockSkins$capturedCapeLayer = capeLayer;
            this.bedrockSkins$capeLayerSuppressed = null;
        }
    }

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$swapModel(LivingEntityRenderState state) {
        if (!(state instanceof AvatarRenderState))
            return;

        java.util.UUID uuid = BedrockRenderStateStore.getUniqueId(state);
        if (uuid == null)
            return;

        var skinId = SkinManager.getSkin(uuid);
        if (skinId == null)
            return;

        BedrockPlayerModel bedrockModel = BedrockModelManager.getModel(skinId);
        if (bedrockModel == null)
            return;

        this.originalModel = this.model;
        this.model = bedrockModel;

        if (this.originalModel instanceof net.minecraft.client.model.player.PlayerModel playerModel) {
            bedrockModel.copyFromVanilla(playerModel);
        }
    }

    @Unique
    private void bedrockSkins$restoreModel() {
        if (this.originalModel != null) {
            this.model = (EntityModel<?>) this.originalModel;
            this.originalModel = null;
        }
    }

    // --- Injectors ---

    @Inject(method = "submit", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue,
            CameraRenderState camera, CallbackInfo ci) {
        this.bedrockSkins$swapModel(state);
    }

    @Inject(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
        )
    )
    private void onRenderBeforePopPose(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue,
            CameraRenderState camera, CallbackInfo ci) {
        if (this.bedrockSkins$capturedCapeLayer == null) return;
        if (!(state instanceof AvatarRenderState avatarState)) return;
        if (!BedrockRenderStateStore.isGuiRender(avatarState)) return;
        if (avatarState.skin == null || avatarState.skin.cape() == null) return;
        if (!avatarState.showCape || avatarState.isInvisible) return;

        // Lazily scan layers once to check if CapeLayer was suppressed by another mod
        if (this.bedrockSkins$capeLayerSuppressed == null) {
            boolean found = false;
            if (this.layers != null) {
                for (Object layer : this.layers) {
                    if (layer instanceof CapeLayer) { found = true; break; }
                }
            }
            this.bedrockSkins$capeLayerSuppressed = !found;
        }

        if (this.bedrockSkins$capeLayerSuppressed) {
            this.bedrockSkins$capturedCapeLayer.submit(
                matrices, queue, avatarState.lightCoords, avatarState,
                avatarState.bodyRot, avatarState.xRot
            );
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue,
            CameraRenderState camera, CallbackInfo ci) {
        this.bedrockSkins$restoreModel();
    }
}