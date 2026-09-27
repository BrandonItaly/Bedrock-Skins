package io.github.brandonitaly.bedrockskins.client.render.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/** Legacy action offsets applied before emotes and attachment pose copying. */
public final class LegacyActionPose {
    private LegacyActionPose() {}

    public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
        if (state.isVisuallySwimming || state.isFallFlying || state.swimAmount > 0) return;
        if (state.isPassenger) {
            model.rightLeg.xRot = model.leftLeg.xRot = -Mth.PI * 0.4f;
            model.rightLeg.yRot = Mth.PI * 0.1f;
            model.leftLeg.yRot = -model.rightLeg.yRot;
            model.rightLeg.zRot = model.leftLeg.zRot = 0;
        }
        // Preserve authored pivots: offsets are relative to each part's bind pose.
        positionY(model.head, state.isCrouching ? 1 : 0);
        positionY(model.body, 0);
        positionY(model.rightArm, 0);
        positionY(model.leftArm, 0);
        positionLeg(model.rightLeg, state.isCrouching);
        positionLeg(model.leftLeg, state.isCrouching);
        positionY(model.hat, state.isCrouching ? 1 : 0);

        if (state.rightArmPose == HumanoidModel.ArmPose.BOW_AND_ARROW
            || state.leftArmPose == HumanoidModel.ArmPose.BOW_AND_ARROW) {
            boolean left = state.leftArmPose == HumanoidModel.ArmPose.BOW_AND_ARROW;
            ModelPart draw = left ? model.leftArm : model.rightArm;
            ModelPart support = left ? model.rightArm : model.leftArm;
            float side = left ? -1 : 1;
            draw.yRot = model.head.yRot - side * 0.1f;
            support.yRot = model.head.yRot + side * 0.5f;
            float pitch = model.head.xRot - Mth.HALF_PI;
            float bobX = Mth.sin(state.ageInTicks * 0.067f) * 0.05f;
            float bobZ = Mth.cos(state.ageInTicks * 0.09f) * 0.05f + 0.05f;
            model.rightArm.xRot = pitch + bobX;
            model.leftArm.xRot = pitch - bobX;
            model.rightArm.zRot = bobZ;
            model.leftArm.zRot = -bobZ;
        }
    }

    private static void positionLeg(ModelPart leg, boolean crouching) {
        positionY(leg, crouching ? -3 : 0);
        leg.z = leg.getInitialPose().z() + (crouching ? 4 : 0.1f);
    }

    private static void positionY(ModelPart part, float offset) {
        part.y = part.getInitialPose().y() + offset;
    }
}
