package io.github.brandonitaly.bedrockskins.client.render.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/** Skin overrides replace locomotion, before vanilla layers action poses over them. */
public final class SkinAnimationPose {
    private SkinAnimationPose() {}

    public static void apply(HumanoidModel<?> model, BedrockPlayerModel.BedrockAnimFlags flags,
                             HumanoidRenderState state) {
        if (!state.isVisuallySwimming) {
            if (flags.armsDown() || flags.armsOutFront()) {
                float pitch = flags.armsDown() ? 0 : -Mth.HALF_PI;
                model.rightArm.xRot = model.leftArm.xRot = pitch;
                model.rightArm.zRot = model.leftArm.zRot = 0;
            } else if (flags.singleArm()) {
                model.leftArm.xRot = model.rightArm.xRot;
                model.rightArm.zRot = model.leftArm.zRot = 0;
            } else if (flags.statueArms() && state.rightArmPose == HumanoidModel.ArmPose.EMPTY
                //? if <=26.2 {
                && state.attackTime == 0
                //?} else {
                /*&& state.swingAnimation == 0
                *///?}
            ) {
                model.rightArm.xRot = -Mth.PI;
                model.rightArm.zRot = -0.3f;
            }
        }
        if (!state.isPassenger && !state.isVisuallySwimming) {
            if (flags.stationaryLegs()) {
                model.rightLeg.xRot = model.leftLeg.xRot = 0;
                model.rightLeg.yRot = model.leftLeg.yRot = 0;
                model.rightLeg.zRot = model.leftLeg.zRot = 0;
            } else if (flags.singleLeg()) {
                model.leftLeg.xRot = model.rightLeg.xRot;
            }
        }
    }
}
