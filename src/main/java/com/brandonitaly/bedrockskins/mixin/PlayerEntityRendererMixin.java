package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockRenderStateStore;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer./*? if <1.21.11 {*//*RenderType*//*?} else {*/rendertype.RenderTypes/*?}*/;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Unique
    private void bedrockSkins$renderArm(PoseStack matrices, int light, Identifier skinTexture, boolean sleeveVisible, ModelPart vanillaArm, SkinId skinId, ModelPart bedrockArm, ModelPart bedrockSleeve, boolean sleeveIsChild, SubmitNodeCollector queue) {
        var bedrockSkin = SkinPackLoader.getLoadedSkin(skinId);
        var texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;

        //? if <1.21.11 {
        /*var layer = RenderType.entityTranslucent(texture);*/
        //?} else {
        var layer = RenderTypes.entityTranslucent(texture);
        //?}

        final PartPose armPose = vanillaArm.storePose();

        queue.submitCustomGeometry(matrices, layer, (entry, consumer) -> {
            PoseStack ms = new PoseStack();
            ms.last().pose().set(entry.pose());
            ms.last().normal().set(entry.normal());

            bedrockArm.resetPose();
            bedrockArm.loadPose(armPose);
            bedrockArm.visible = true;
            
            if (bedrockSleeve != null) {
                if (!sleeveIsChild) {
                    bedrockSleeve.resetPose();
                    bedrockSleeve.loadPose(armPose);
                }
                bedrockSleeve.visible = sleeveVisible;
            }

            bedrockArm.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);

            if (bedrockSleeve != null && !sleeveIsChild && sleeveVisible) {
                bedrockSleeve.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);
            }
        });
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void updateRenderState(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer cp) {
            java.util.UUID uuid = cp.getUUID();
            BedrockRenderStateStore.setUniqueId(state, uuid);
            BedrockRenderStateStore.setSkinId(state, SkinManager.getSkin(uuid));
        }
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void bedrockSkins$renderBedrockHand(PoseStack matrices, SubmitNodeCollector queue, int light, Identifier tex, ModelPart arm, boolean sleeveVisible, CallbackInfo ci) {
        SkinId skinId = SkinManager.getLocalSelectedKey();
        if (skinId == null) return;

        var bedrockModel = BedrockModelManager.getModel(skinId);
        if (bedrockModel == null) return;

        HumanoidModel<?> model = ((AvatarRenderer<?>)(Object)this).getModel();
        boolean isRightArm = (arm == model.rightArm);

        ModelPart bedrockArm = isRightArm ? bedrockModel.customRightArm : bedrockModel.customLeftArm;
        if (bedrockArm == null) return;

        ModelPart bedrockSleeve = null;
        boolean sleeveIsChild = false;

        String prefix = isRightArm ? "right" : "left";
        if (bedrockArm.hasChild(prefix + "_sleeve")) {
            bedrockSleeve = bedrockArm.getChild(prefix + "_sleeve");
            sleeveIsChild = true;
        } else if (bedrockArm.hasChild(prefix + "Sleeve")) {
            bedrockSleeve = bedrockArm.getChild(prefix + "Sleeve");
            sleeveIsChild = true;
        } else {
            bedrockSleeve = bedrockModel.partsMap.get(prefix + "_sleeve");
            if (bedrockSleeve == null) bedrockSleeve = bedrockModel.partsMap.get(prefix + "Sleeve");
        }

        bedrockSkins$renderArm(matrices, light, tex, sleeveVisible, arm, skinId, bedrockArm, bedrockSleeve, sleeveIsChild, queue);
    }

    @Inject(method = "isEntityUpsideDown", at = @At("HEAD"), cancellable = true)
    private void isEntityUpsideDown(Avatar player, CallbackInfoReturnable<Boolean> ci) {
        if (player instanceof AbstractClientPlayer cp) {
            SkinId skinId = SkinManager.getSkin(cp.getUUID());
            if (skinId != null) {
                var skin = SkinPackLoader.getLoadedSkin(skinId);
                if (skin != null && skin.upsideDown) ci.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void getTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> ci) {
        java.util.UUID uuid = BedrockRenderStateStore.getUniqueId(state);
        if (uuid != null) {
            SkinId skinId = SkinManager.getSkin(uuid);
            if (skinId != null) {
                var skin = SkinPackLoader.getLoadedSkin(skinId);
                if (skin != null && skin.identifier != null) ci.setReturnValue(skin.identifier);
            }
        }
    }
}