package io.github.brandonitaly.bedrockskins.client.appearance.skin;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.client.render.model.HeadIconRasterizer;
import io.github.brandonitaly.bedrockskins.mixin.render.ModelPartAccessor;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

/** GUI-only atlases: the usual face UV rectangle contains a rendered 64px portrait. */
public final class HeadIconTextures {
    private record Key(Identifier source, boolean hat) {}
    private static final Map<Key, Identifier> CACHE = new HashMap<>();
    private static long nextId;
    private HeadIconTextures() {}

    public static Identifier resolve(Identifier source, boolean hat) {
        Key key = new Key(source, hat);
        Identifier previous = CACHE.get(key);
        if (previous != null) return previous;
        var skin = SkinPackLoader.getLoadedSkinByTexture(source);
        if (skin == null) return source;
        var manager = Minecraft.getInstance().getTextureManager();
        if (!(manager.getTexture(source) instanceof DynamicTexture texture) || texture.getPixels() == null) return source;
        var model = BedrockModelManager.getModel(skin.skinId);
        if (model == null) return source;
        List<HeadIconRasterizer.Triangle> triangles = new ArrayList<>();
        Set<ModelPart> hats = Collections.newSetFromMap(new IdentityHashMap<>());
        if (model.customHat != null) hats.add(model.customHat);
        model.partsMap.forEach((name, part) -> { if (name.toLowerCase(Locale.ROOT).endsWith("helmet")) hats.add(part); });
        var crop = collect(model.root(), new Matrix4f(), false, false, model.customHead, hats, hat, triangles);
        if (triangles.isEmpty()) return source;
        NativeImage input = texture.getPixels();
        int[] pixels = HeadIconRasterizer.render(triangles, 64, input.getWidth(), input.getHeight(), input::getPixel, crop);
        // Static portraits only retain the texture ID; TextureManager owns the GPU resource.
        if (CACHE.size() >= 128) clear();
        NativeImage atlas = new NativeImage(512, 512, true);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            atlas.setPixel(64 + x, 64 + y, pixels[y * 64 + x]);
        }
        DynamicTexture output = new DynamicTexture(() -> "bedrock_head_icon", atlas);
        Identifier id = Identifier.fromNamespaceAndPath("bedrockskins", "head_icons/" + nextId++);
        manager.register(id, output);
        output.upload();
        CACHE.put(key, id);
        return id;
    }

    private static HeadIconRasterizer.Crop collect(ModelPart part, Matrix4f parent, boolean active, boolean hatLayer, ModelPart head,
                                Set<ModelPart> hats, boolean showHat, List<HeadIconRasterizer.Triangle> output) {
        if (hats.contains(part) && !showHat) return null;
        HeadIconRasterizer.Crop crop = null;
        hatLayer |= hats.contains(part);
        active |= part == head || hatLayer;
        var pose = part.getInitialPose();
        Matrix4f matrix = new Matrix4f(parent).translate(pose.x(), pose.y(), pose.z())
            .rotateZYX(pose.zRot(), pose.yRot(), pose.xRot()).scale(pose.xScale(), pose.yScale(), pose.zScale());
        if (part == head) {
            // Frame the standard face area relative to the head pivot, not the
            // accessory bounds. Hat geometry can overlap it but cannot shrink it.
            float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = -minX, maxY = -minX;
            for (float x : new float[]{-4, 4}) for (float y : new float[]{-8, 0}) {
                Vector3f point = matrix.transformPosition(x, y, 0, new Vector3f());
                minX = Math.min(minX, point.x); maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y); maxY = Math.max(maxY, point.y);
            }
            crop = new HeadIconRasterizer.Crop(minX, minY, maxX, maxY);
        }
        var access = (ModelPartAccessor)(Object)part;
        if (active) for (var cube : access.bedrockSkins$getCubes()) {
            Matrix4f iconMatrix = hatLayer ? withoutInflation(cube, matrix) : matrix;
            for (var polygon : cube.polygons) {
                var vertices = polygon.vertices();
                for (int i = 1; i + 1 < vertices.length; i++) output.add(new HeadIconRasterizer.Triangle(
                    vertex(vertices[0], iconMatrix), vertex(vertices[i], iconMatrix), vertex(vertices[i+1], iconMatrix)));
            }
        }
        for (var child : access.bedrockSkins$getChildren().values()) {
            var childCrop = collect(child, matrix, active, hatLayer, head, hats, showHat, output);
            if (childCrop != null) crop = childCrop;
        }
        return crop;
    }
    /** Flatten only symmetric cube inflation in the portrait plane; retain depth for overlays. */
    private static Matrix4f withoutInflation(ModelPart.Cube cube, Matrix4f matrix) {
        float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = -minX, maxY = -minX;
        for (var polygon : cube.polygons) for (var vertex : polygon.vertices()) {
            minX = Math.min(minX, vertex.x()); maxX = Math.max(maxX, vertex.x());
            minY = Math.min(minY, vertex.y()); maxY = Math.max(maxY, vertex.y());
        }
        float sx = inflationScale(cube.minX, cube.maxX, minX, maxX);
        float sy = inflationScale(cube.minY, cube.maxY, minY, maxY);
        if (sx == 1 && sy == 1) return matrix;
        float cx = (cube.minX + cube.maxX) / 2, cy = (cube.minY + cube.maxY) / 2;
        return new Matrix4f(matrix).translate(cx, cy, 0).scale(sx, sy, 1).translate(-cx, -cy, 0);
    }

    static float inflationScale(float min, float max, float inflatedMin, float inflatedMax) {
        float padding = min - inflatedMin;
        // Meshes, flat planes and asymmetrically edited faces must retain their authored shape.
        if (max <= min || padding <= 0 || Math.abs(padding - (inflatedMax - max)) > 0.001f) return 1;
        return (max - min) / (inflatedMax - inflatedMin);
    }

    private static HeadIconRasterizer.Vertex vertex(ModelPart.Vertex vertex, Matrix4f matrix) {
        Vector3f point = matrix.transformPosition(vertex.x(), vertex.y(), vertex.z(), new Vector3f());
        return new HeadIconRasterizer.Vertex(point.x, point.y, point.z, vertex.u(), vertex.v());
    }
    public static void remove(Identifier source) {
        CACHE.entrySet().removeIf(entry -> {
            if (!entry.getKey().source.equals(source)) return false;
            Minecraft.getInstance().getTextureManager().release(entry.getValue());
            return true;
        });
    }
    public static void clear() {
        CACHE.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        CACHE.clear();
    }
}
