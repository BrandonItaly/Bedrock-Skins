package io.github.brandonitaly.bedrockskins.client.render.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/** Adapts the backward crouch to Java's independent humanoid parts. */
public final class InvertedCrouchPose {
    private InvertedCrouchPose() {}

    public static void apply(HumanoidModel<?> model, HumanoidRenderState state) {
        if (!state.isCrouching || state.isVisuallySwimming || state.isFallFlying || state.swimAmount > 0) return;

        // Reverse only the crouch contribution, preserving walking and item-use poses.
        model.body.xRot = -model.body.xRot;
        model.rightArm.xRot -= 0.8f;
        model.leftArm.xRot -= 0.8f;
        // Bedrock's inverted sneaking animation adds a downward head tilt.
        model.head.xRot -= 28 * Mth.DEG_TO_RAD;
        model.hat.xRot = model.head.xRot;
        mirrorCrouchOffset(model.head);
        mirrorCrouchOffset(model.hat);
        mirrorCrouchOffset(model.body);
        mirrorCrouchOffset(model.rightArm);
        mirrorCrouchOffset(model.leftArm);
        mirrorCrouchOffset(model.rightLeg);
        mirrorCrouchOffset(model.leftLeg);
    }

    private static void mirrorCrouchOffset(ModelPart part) {
        part.z = 2 * part.getInitialPose().z() - part.z;
    }
}
