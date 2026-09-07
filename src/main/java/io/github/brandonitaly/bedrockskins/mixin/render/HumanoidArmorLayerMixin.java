package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.appearance.AppearanceResolver;
import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> {

    @Shadow private A getArmorModel(S state, EquipmentSlot slot) { return null; }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$hideArmor(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot, CallbackInfo ci) {
        SkinId skinId = AppearanceResolver.skinId(state);
        if (skinId == null) return;

        BedrockPlayerModel model = BedrockModelManager.getModel(skinId);
        if (model != null && model.shouldHideArmor()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$applyPieceVisibility(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int lightCoords, S state, CallbackInfo ci) {
        UUID uuid = BedrockRenderStateStore.getUniqueId(state);
        SkinId skinId = AppearanceResolver.skinId(state);
        A armorModel = this.getArmorModel(state, slot);
        if (armorModel == null) return;

        boolean shouldRender = true;
        if (skinId != null) {
            BedrockPlayerModel bedrockModel = BedrockModelManager.getModel(skinId);
            if (bedrockModel != null) {
                shouldRender = bedrockModel.applyArmorVisibility(armorModel, slot);
            }
        }
        shouldRender &= bedrockSkins$applyPersonaArmorVisibility(armorModel, slot, PersonaManager.occlusion(uuid));
        if (!shouldRender) ci.cancel();
    }

    @Unique
    private boolean bedrockSkins$applyPersonaArmorVisibility(A armorModel, EquipmentSlot slot,
                                                              PersonaManager.Occlusion hidden) {
        if (hidden.isEmpty()) return true;
        return switch (slot) {
            case HEAD -> {
                if (hidden.head()) {
                    armorModel.head.visible = false;
                    armorModel.hat.visible = false;
                }
                yield armorModel.head.visible || armorModel.hat.visible;
            }
            case CHEST -> {
                if (hidden.body()) armorModel.body.visible = false;
                if (hidden.rightArm()) armorModel.rightArm.visible = false;
                if (hidden.leftArm()) armorModel.leftArm.visible = false;
                yield armorModel.body.visible || armorModel.rightArm.visible || armorModel.leftArm.visible;
            }
            case LEGS -> {
                if (hidden.body()) armorModel.body.visible = false;
                if (hidden.rightLeg()) armorModel.rightLeg.visible = false;
                if (hidden.leftLeg()) armorModel.leftLeg.visible = false;
                yield armorModel.body.visible || armorModel.rightLeg.visible || armorModel.leftLeg.visible;
            }
            case FEET -> {
                if (hidden.rightLeg()) armorModel.rightLeg.visible = false;
                if (hidden.leftLeg()) armorModel.leftLeg.visible = false;
                yield armorModel.rightLeg.visible || armorModel.leftLeg.visible;
            }
            default -> true;
        };
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
