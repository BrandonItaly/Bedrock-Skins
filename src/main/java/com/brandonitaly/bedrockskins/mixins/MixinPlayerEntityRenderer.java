package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
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
    //? if >=1.21.11 {
    private void bedrockSkins$renderArm(boolean isRightArm, PoseStack matrices, int light, Identifier skinTexture, boolean sleeveVisible, Object rendererOrQueue, CallbackInfo ci) {
    //?} else {
    /*private void bedrockSkins$renderArm(boolean isRightArm, PoseStack matrices, int light, ResourceLocation skinTexture, boolean sleeveVisible, Object rendererOrQueue, CallbackInfo ci) {*/
    //?}
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var uuid = player.getUUID();
        var bedrockModel = BedrockModelManager.getModel(uuid);

        if (bedrockModel != null) {
            String side = isRightArm ? "right" : "left";
            
            var parts = bedrockModel.partsMap;
            var part = parts.get(side + "_arm");
            if (part == null) part = parts.get(side + "Arm");
            
            var sleeve = parts.get(side + "_sleeve");
            if (sleeve == null) sleeve = parts.get(side + "Sleeve");

            if (part != null) {
                String skinKey = SkinManager.getSkin(uuid.toString());
                var bedrockSkin = (skinKey != null) ? SkinPackLoader.loadedSkins.get(skinKey) : null;
                var texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;

                final var finalPart = part;
                final var finalSleeve = sleeve;
                boolean sleeveIsChild = finalPart.hasChild(side + "_sleeve") || finalPart.hasChild(side + "Sleeve");

                //? if <=1.21.8 {
                /*
                var consumers = (MultiBufferSource) rendererOrQueue;
                var layer = RenderLayer.getEntityTranslucent(texture);
                
                MatrixStack ms = new MatrixStack();
                ms.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
                ms.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());

                finalPart.resetTransform();
                if (finalSleeve != null) {
                    finalSleeve.resetTransform();
                    finalSleeve.visible = sleeveVisible;
                }

                finalPart.render(ms, consumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);
                if (finalSleeve != null && !sleeveIsChild && sleeveVisible) {
                    finalSleeve.render(ms, consumers.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);
                }
                */
                //?} else {
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

                    finalPart.resetPose();
                    if (finalSleeve != null) {
                        finalSleeve.resetPose();
                        finalSleeve.visible = sleeveVisible;
                    }

                    finalPart.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);

                    if (finalSleeve != null && !sleeveIsChild && sleeveVisible) {
                        finalSleeve.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);
                    }
                });
                //?}
                ci.cancel();
            }
        }
    }

    //? if <=1.21.8 {
    /*
    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void updateRenderState(AbstractClientPlayer player, net.minecraft.client.renderer.entity.state.PlayerRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof BedrockSkinState skinState) skinState.setUniqueId(player.getUUID());
    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"), cancellable = true)
    private void renderHand(PoseStack matrices, MultiBufferSource consumers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) ((AvatarRenderer)(Object)this).getModel();
        boolean isRightArm = (arm == model.rightArm);
        bedrockSkins$renderArm(isRightArm, matrices, light, player.getSkinTextureLocation(), sleeve.visible, consumers, ci);
    }*/
    //?} else {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void updateRenderState(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer cp && state instanceof BedrockSkinState skinState) {
            skinState.setUniqueId(cp.getUUID());
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.11 {
    private void renderHand(PoseStack matrices, SubmitNodeCollector queue, int light, Identifier tex, ModelPart arm, boolean sleeve, CallbackInfo ci) {
    //?} else {
    /*private void renderHand(PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation tex, ModelPart arm, boolean sleeve, CallbackInfo ci) {*///?}
        HumanoidModel<?> model = (HumanoidModel<?>) ((AvatarRenderer)(Object)this).getModel();
        boolean isRightArm = (arm == model.rightArm);
        bedrockSkins$renderArm(isRightArm, matrices, light, tex, sleeve, queue, ci);
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    //? if >=1.21.11 {
    private void getTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> ci) {
    //?} else {
    /*private void getTexture(AvatarRenderState state, CallbackInfoReturnable<ResourceLocation> ci) {*/
    //?}
        if (state instanceof BedrockSkinState skinState) {
            java.util.UUID uuid = skinState.getUniqueId();
            if (uuid != null) {
                String skinKey = SkinManager.getSkin(uuid.toString());
                if (skinKey != null) {
                    var skin = SkinPackLoader.loadedSkins.get(skinKey);
                    if (skin != null && skin.identifier != null) ci.setReturnValue(skin.identifier);
                }
            }
        }
    }
}