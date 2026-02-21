package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

//? if >=1.21.11 {
import net.minecraft.client.model.object.equipment.ElytraModel;
//?} else {
/*import net.minecraft.client.model.ElytraModel;*/
//?}

@Mixin(ElytraModel.class)
public abstract class MixinElytraModel {

    @Shadow public ModelPart rightWing;
    @Shadow public ModelPart leftWing;

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void bedrockSkins$syncElytraPivots(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof BedrockSkinState skinState)) return;

        SkinId skinId = skinState.getBedrockSkinId();
        if (skinId == null) {
            UUID uuid = skinState.getUniqueId();
            if (uuid == null) return;
            skinId = SkinManager.getSkin(uuid);
        }

        if (skinId == null) return;
        BedrockPlayerModel bedrockModel = BedrockModelManager.getModel(skinId);
        if (bedrockModel == null) return;

        ModelPart body = bedrockModel.partsMap.getOrDefault("body", bedrockModel.body);
        if (body == null) return;

        // Transform the wings to be relative to the Bedrock body bone
        float leftWingX = 5.0F;
        float rightWingX = -5.0F;

        this.leftWing.x = body.x + leftWingX;
        this.leftWing.y = body.y;
        this.leftWing.z = body.z;

        this.rightWing.x = body.x + rightWingX;
        this.rightWing.y = body.y;
        this.rightWing.z = body.z;
    }
}