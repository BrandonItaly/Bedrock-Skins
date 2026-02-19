package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinHumanoidModel<T extends HumanoidRenderState> {

    @Shadow public ModelPart head;
    @Shadow public ModelPart hat;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void bedrockSkins$syncToBedrockModel(T state, CallbackInfo ci) {
        if ((Object) this instanceof BedrockPlayerModel bedrockPlayerModel) {
            applyBedrockPartVisibility(bedrockPlayerModel, state);
            return;
        }
        if (!(state instanceof BedrockSkinState skinState)) return;

        SkinId skinId = skinState.getBedrockSkinId();
        if (skinId == null) {
            UUID uuid = skinState.getUniqueId();
            skinId = uuid == null ? null : SkinManager.getSkin(uuid.toString());
        }
        if (skinId == null) return;

        BedrockPlayerModel bedrockModel = BedrockModelManager.getModel(skinId);
        if (bedrockModel == null) return;

        copyPose(bedrockModel.head, this.head);
        copyPose(bedrockModel.hat, this.hat);
        copyPose(bedrockModel.body, this.body);
        syncCapePivots(bedrockModel);
        copyPose(bedrockModel.rightArm, this.rightArm);
        copyPose(bedrockModel.leftArm, this.leftArm);
        copyPose(bedrockModel.rightLeg, this.rightLeg);
        copyPose(bedrockModel.leftLeg, this.leftLeg);
    }

    private void applyBedrockPartVisibility(BedrockPlayerModel bedrockModel, T state) {
        bedrockModel.setBedrockPartVisible("bodyArmor", true);
        bedrockModel.setBedrockPartVisible("helmet", true);

        boolean capeVisible = state instanceof AvatarRenderState avatarState && avatarState.showCape;
        if (capeVisible && hasActualCape(state)) {
            bedrockModel.setBedrockPartVisible("bodyArmor", false);
        }

        if (state.chestEquipment != null && !state.chestEquipment.isEmpty()) {
            bedrockModel.setBedrockPartVisible("bodyArmor", false);
        }
        if (state.headEquipment != null && !state.headEquipment.isEmpty()) {
            bedrockModel.setBedrockPartVisible("helmet", false);
        }
    }

    private boolean hasActualCape(T state) {
        if (!(state instanceof BedrockSkinState skinState)) return false;

        UUID uuid = skinState.getUniqueId();
        if (uuid == null) return false;

        try {
            var client = Minecraft.getInstance();
            if (client == null || client.getConnection() == null) return false;
            var entry = client.getConnection().getPlayerInfo(uuid);
            if (entry == null) return false;
            var textures = entry.getSkin();
            return textures != null && textures.cape() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void syncCapePivots(BedrockPlayerModel bedrockModel) {
        if (this.body == null || bedrockModel == null) return;

        ModelPart sourceCape = bedrockModel.partsMap.get("cape");
        if (sourceCape != null) copyChildIfPresent(sourceCape, this.body, "cape");

        ModelPart sourceElytra = bedrockModel.partsMap.get("elytra");
        if (sourceElytra != null) copyChildIfPresent(sourceElytra, this.body, "elytra");
    }

    private static void copyChildIfPresent(ModelPart sourcePart, ModelPart targetParent, String childName) {
        if (sourcePart == null || targetParent == null || !targetParent.hasChild(childName)) return;
        copyPose(sourcePart, targetParent.getChild(childName));
    }

    private static void copyPose(ModelPart from, ModelPart to) {
        if (from == null || to == null) return;
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
    }
}
