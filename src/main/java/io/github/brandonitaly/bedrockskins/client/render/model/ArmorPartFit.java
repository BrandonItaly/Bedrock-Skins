package io.github.brandonitaly.bedrockskins.client.render.model;

import io.github.brandonitaly.bedrockskins.bedrock.BedrockBone;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Maps the standard vertical interval onto a skin's uninflated leg geometry. */
public record ArmorPartFit(float offset, float scale) {
    public static final ArmorPartFit STANDARD = new ArmorPartFit(0, 1);

    public static ArmorPartFit from(BedrockBone bone, float referenceTop, float referenceHeight) {
        if (bone.getPivot() == null || bone.getPivot().size() < 3 || bone.getCubes() == null) return STANDARD;
        float bottom = Float.POSITIVE_INFINITY, top = Float.NEGATIVE_INFINITY;
        for (var cube : bone.getCubes()) {
            if (cube.getArmorMask() != 0 || cube.getOrigin() == null || cube.getSize() == null
                || cube.getOrigin().size() < 3 || cube.getSize().size() < 3) continue;
            if (cube.getSize().get(0) <= 0 || cube.getSize().get(1) <= 0 || cube.getSize().get(2) <= 0) continue;
            // Rotated cubes require transformed bounds; leave unusual models at their authored fit.
            if (cube.getRotation() != null && cube.getRotation().stream().anyMatch(v -> v != 0)) return STANDARD;
            bottom = Math.min(bottom, cube.getOrigin().get(1));
            top = Math.max(top, cube.getOrigin().get(1) + cube.getSize().get(1));
        }
        float height = top - bottom;
        if (!Float.isFinite(height) || height <= 0) return STANDARD;
        float scale = height / referenceHeight;
        return new ArmorPartFit(bone.getPivot().get(1) - top - referenceTop * scale, scale);
    }

    public void apply(ModelPart leg) {
        if (offset != 0) {
            Vector3f translation = new Vector3f(0, offset * leg.yScale, 0).rotate(
                new Quaternionf().rotationZYX(leg.zRot, leg.yRot, leg.xRot));
            leg.x += translation.x; leg.y += translation.y; leg.z += translation.z;
        }
        leg.yScale *= scale;
    }
}
