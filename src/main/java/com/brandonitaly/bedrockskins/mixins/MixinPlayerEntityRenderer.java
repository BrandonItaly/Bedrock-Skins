package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.resources./*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;*/
//?}

import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    @Unique
    private void bedrockSkins$renderArm(boolean isRightArm, PoseStack matrices, int light, /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ skinTexture, boolean sleeveVisible, Object rendererOrQueue, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var uuid = player.getUUID();
        var skinId = SkinManager.getSkin(uuid);
        var bedrockModel = skinId == null ? null : BedrockModelManager.getModel(skinId);

        if (bedrockModel != null) {
            String side = isRightArm ? "right" : "left";
            
            var parts = bedrockModel.partsMap;
            var part = parts.get(side + "_arm");
            if (part == null) part = parts.get(side + "Arm");
            
            var sleeve = parts.get(side + "_sleeve");
            if (sleeve == null) sleeve = parts.get(side + "Sleeve");

            if (part != null) {
                var bedrockSkin = SkinPackLoader.getLoadedSkin(skinId);

                var texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;

                final var finalPart = part;
                final var finalSleeve = sleeve;
                boolean sleeveIsChild = finalPart.hasChild(side + "_sleeve") || finalPart.hasChild(side + "Sleeve");

                var queue = (SubmitNodeCollector) rendererOrQueue;
                
                //? if <1.21.11 {
                /*var layer = RenderType.entityTranslucent(texture);*/
                //?} else {
                var layer = RenderTypes.entityTranslucent(texture);
                //?}

                queue.submitCustomGeometry(matrices, layer, (entry, consumer) -> {
                    PoseStack ms = new PoseStack();
                    ms.last().pose().set(entry.pose());
                    ms.last().normal().set(entry.normal());

                    float handZRot = isRightArm ? 0.1F : -0.1F;
                    finalPart.resetPose();
                    finalPart.visible = true;
                    finalPart.zRot = handZRot;
                    if (finalSleeve != null) {
                        finalSleeve.resetPose();
                        finalSleeve.visible = sleeveVisible;
                        if (!sleeveIsChild) {
                            finalSleeve.zRot = handZRot;
                        }
                    }

                    finalPart.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);

                    if (finalSleeve != null && !sleeveIsChild && sleeveVisible) {
                        finalSleeve.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);
                    }
                });
                ci.cancel();
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void updateRenderState(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer cp && state instanceof BedrockRenderStateAccessor skinState) {
            java.util.UUID uuid = cp.getUUID();
            skinState.bedrockSkins$setUniqueId(uuid);
            skinState.bedrockSkins$setBedrockSkinId(SkinManager.getSkin(uuid));
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(PoseStack matrices, SubmitNodeCollector queue, int light, /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ tex, ModelPart arm, boolean sleeve, CallbackInfo ci) {
        HumanoidModel<?> model = ((AvatarRenderer<?>)(Object)this).getModel();
        boolean isRightArm = (arm == model.rightArm);
        bedrockSkins$renderArm(isRightArm, matrices, light, tex, sleeve, queue, ci);
    }

    @Inject(method = "isEntityUpsideDown", at = @At("HEAD"), cancellable = true)
    private void isEntityUpsideDown(Avatar player, CallbackInfoReturnable<Boolean> ci) {
        if (player instanceof AbstractClientPlayer cp) {
            SkinId skinId = SkinManager.getSkin(cp.getUUID());
            if (skinId != null) {
                var skin = SkinPackLoader.getLoadedSkin(skinId);
                if (skin != null && skin.isUpsideDown()) ci.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void getTexture(AvatarRenderState state, CallbackInfoReturnable</*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/> ci) {
        if (state instanceof BedrockRenderStateAccessor skinState) {
            java.util.UUID uuid = skinState.bedrockSkins$getUniqueId();
            if (uuid != null) {
                SkinId skinId = SkinManager.getSkin(uuid);
                if (skinId != null) {
                    var skin = SkinPackLoader.getLoadedSkin(skinId);
                    if (skin != null && skin.identifier != null) ci.setReturnValue(skin.identifier);
                }
            }
        }
    }
}