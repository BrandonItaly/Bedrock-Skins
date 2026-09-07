package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.model.CustomFaceCube;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders only Bedrock cubes with post-bake face changes from their live polygons.
 * The higher priority runs before Sodium's cached-cuboid compile injection, while
 * ordinary entity cubes remain on Sodium's optimized path.
 */
@Mixin(value = ModelPart.Cube.class, priority = 1100)
public abstract class ModelPartCubeMixin implements CustomFaceCube {
    @Unique
    private boolean bedrockSkins$livePolygons;

    @Override
    public void bedrockSkins$useLivePolygons() {
        bedrockSkins$livePolygons = true;
    }

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$compileLivePolygons(PoseStack.Pose pose, VertexConsumer vertices,
                                                   int light, int overlay, int color,
                                                   CallbackInfo ci) {
        if (!bedrockSkins$livePolygons) return;

        Matrix4f matrix = pose.pose();
        Vector3f transformed = new Vector3f();
        for (ModelPart.Polygon polygon : ((ModelPart.Cube) (Object) this).polygons) {
            pose.transformNormal(polygon.normal(), transformed);
            float normalX = transformed.x();
            float normalY = transformed.y();
            float normalZ = transformed.z();

            for (ModelPart.Vertex vertex : polygon.vertices()) {
                matrix.transformPosition(vertex.x() / 16.0F, vertex.y() / 16.0F,
                    vertex.z() / 16.0F, transformed);
                vertices.addVertex(transformed.x(), transformed.y(), transformed.z(), color,
                    vertex.u(), vertex.v(), overlay, light, normalX, normalY, normalZ);
            }
        }
        ci.cancel();
    }
}
