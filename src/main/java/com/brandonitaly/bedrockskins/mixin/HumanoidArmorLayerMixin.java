package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> {

    @Shadow private A getArmorModel(S state, EquipmentSlot slot) { return null; }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$hideArmor(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot, CallbackInfo ci) {
        if (!(state instanceof BedrockRenderStateAccessor skinState)) return;

        SkinId skinId = skinState.bedrockSkins$getBedrockSkinId();
        if (skinId == null) {
            UUID uuid = skinState.bedrockSkins$getUniqueId();
            skinId = uuid == null ? null : SkinManager.getSkin(uuid);
        }
        if (skinId == null) return;

        BedrockPlayerModel model = BedrockModelManager.getModel(skinId);
        if (model != null && model.shouldHideArmor()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$applyPieceVisibility(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int lightCoords, S state, CallbackInfo ci) {
        if (!(state instanceof BedrockRenderStateAccessor skinState)) return;

        SkinId skinId = skinState.bedrockSkins$getBedrockSkinId();
        if (skinId == null) {
            UUID uuid = skinState.bedrockSkins$getUniqueId();
            skinId = uuid == null ? null : SkinManager.getSkin(uuid);
        }
        if (skinId == null) return;

        BedrockPlayerModel bedrockModel = BedrockModelManager.getModel(skinId);
        if (bedrockModel != null) {
            A armorModel = this.getArmorModel(state, slot);
            if (armorModel != null) {
                boolean shouldRender = bedrockModel.applyArmorVisibility(armorModel, slot);
                // If the entire piece shouldn't render, cancel it completely.
                if (!shouldRender) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void bedrockSkins$resetPieceVisibility(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int lightCoords, S state, CallbackInfo ci) {
        A armorModel = this.getArmorModel(state, slot);
        if (armorModel != null) {
            armorModel.head.visible = true;
            armorModel.hat.visible = true;
            armorModel.body.visible = true;
            armorModel.rightArm.visible = true;
            armorModel.leftArm.visible = true;
            armorModel.rightLeg.visible = true;
            armorModel.leftLeg.visible = true;
        }
    }
}