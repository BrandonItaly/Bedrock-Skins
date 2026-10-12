package io.github.brandonitaly.bedrockskins.client.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix4f;

/** Transfers EMF's animated skeleton without replacing Bedrock geometry or pivots. */
public final class ResourcePackAnimationPose {
    private static PlayerModel animatingSource;
    private final PartPose[] poses;
    private final PartPose[] rest;

    private ResourcePackAnimationPose(PlayerModel source) {
        ModelPart[] parts = parts(source);
        poses = new PartPose[parts.length];
        rest = new PartPose[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            // ModelPart.storePose() deliberately omits live scale values.
            poses[i] = livePose(part);
            rest[i] = part.getInitialPose();
        }
    }

    public static boolean isAnimatingSource(Object model) { return model == animatingSource; }

    public static boolean hasAnimation(Model<?> model) {
        return (Object) model.root() instanceof ResourcePackAnimatedModel animated
            && animated.hasAnimation();
    }

    public static ResourcePackAnimationPose capture(PlayerModel model) {
        return new ResourcePackAnimationPose(model);
    }

    public static ResourcePackAnimationPose animate(PlayerModel source, AvatarRenderState state) {
        if (!((Object) source.root() instanceof ResourcePackAnimatedModel animated)
            || !animated.hasAnimation()) return null;
        PlayerModel previous = animatingSource;
        animatingSource = source;
        try {
            source.setupAnim(state);
            animated.animate();
            return capture(source);
        } finally {
            animatingSource = previous;
        }
    }

    public void apply(PlayerModel target) {
        apply(target, target);
    }

    /** Replace only the attachment's inherited pack motion, keeping its own flap. */
    public Matrix4f attachmentCorrection(HumanoidModel<?> target) {
        Matrix4f packTorso = transform(new Matrix4f(), poses[0]);
        transform(packTorso, poses[2]);
        Matrix4f torso = new Matrix4f();
        transform(torso, livePose(target.root()));
        transform(torso, livePose(target.body));
        return torso.mul(packTorso.invert());
    }

    private static PartPose livePose(ModelPart part) {
        return new PartPose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
            part.xScale, part.yScale, part.zScale);
    }

    private static Matrix4f transform(Matrix4f matrix, PartPose pose) {
        return matrix.translate(pose.x() / 16, pose.y() / 16, pose.z() / 16)
            .rotateZYX(pose.zRot(), pose.yRot(), pose.xRot())
            .scale(pose.xScale(), pose.yScale(), pose.zScale());
    }

    public void apply(HumanoidModel<?> target, HumanoidModel<?> skeleton) {
        ModelPart[] parts = parts(target);
        ModelPart[] anchors = parts(skeleton);
        for (int i = 0; i < parts.length; i++) {
            apply(parts[i], anchors[i].getInitialPose(), poses[i], rest[i]);
        }
    }

    static void apply(ModelPart target, PartPose pose, PartPose sourceRest) {
        apply(target, target.getInitialPose(), pose, sourceRest);
    }

    private static void apply(ModelPart target, PartPose targetRest, PartPose pose, PartPose sourceRest) {
        target.x = targetRest.x() + pose.x() - sourceRest.x();
        target.y = targetRest.y() + pose.y() - sourceRest.y();
        target.z = targetRest.z() + pose.z() - sourceRest.z();
        target.xRot = targetRest.xRot() + pose.xRot() - sourceRest.xRot();
        target.yRot = targetRest.yRot() + pose.yRot() - sourceRest.yRot();
        target.zRot = targetRest.zRot() + pose.zRot() - sourceRest.zRot();
        target.xScale = targetRest.xScale() * pose.xScale() / sourceRest.xScale();
        target.yScale = targetRest.yScale() * pose.yScale() / sourceRest.yScale();
        target.zScale = targetRest.zScale() * pose.zScale() / sourceRest.zScale();
    }

    private static ModelPart[] parts(HumanoidModel<?> model) {
        // Overlays are children of these bones and inherit their transforms.
        return new ModelPart[]{model.root(), model.head, model.body, model.rightArm,
            model.leftArm, model.rightLeg, model.leftLeg};
    }
}
