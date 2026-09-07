package io.github.brandonitaly.bedrockskins.client.render.state;

import com.mojang.blaze3d.vertex.PoseStack;

/** Small helpers for deferred custom-geometry render callbacks. */
public final class RenderPoseUtils {
    private RenderPoseUtils() {
    }

    public static PoseStack copyOf(PoseStack.Pose source) {
        PoseStack copy = new PoseStack();
        copy.last().pose().set(source.pose());
        copy.last().normal().set(source.normal());
        return copy;
    }
}
