package io.github.brandonitaly.bedrockskins.client.render.model;

import io.github.brandonitaly.bedrockskins.bedrock.BedrockBone;
import io.github.brandonitaly.bedrockskins.bedrock.BedrockCube;
import io.github.brandonitaly.bedrockskins.bedrock.BedrockPolyMesh;
import io.github.brandonitaly.bedrockskins.mixin.render.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Applies Bedrock features that Minecraft's box-only model builder cannot represent. */
final class BedrockGeometryBaker {
    /** ModelPart stores polygons as Java DOWN, UP, WEST, NORTH, EAST, SOUTH. */
    private static final String[] FACE_KEYS = {"up", "down", "west", "north", "east", "south"};

    private BedrockGeometryBaker() {
    }

    static void apply(List<BedrockBone> bones, Map<String, ModelPart> parts,
                      int textureWidth, int textureHeight) {
        Map<String, BedrockBone> uniqueBones = new LinkedHashMap<>();
        for (BedrockBone bone : bones) {
            if (bone.getName() != null) uniqueBones.putIfAbsent(bone.getName(), bone);
        }
        List<BedrockBone> sourceBones = List.copyOf(uniqueBones.values());
        applyPerFaceUvs(sourceBones, parts, textureWidth, textureHeight);
        applyPolyMeshes(sourceBones, parts, textureWidth, textureHeight);
    }

    private static void applyPerFaceUvs(List<BedrockBone> bones, Map<String, ModelPart> parts,
                                        int textureWidth, int textureHeight) {
        for (BedrockBone bone : bones) {
            ModelPart part = parts.get(bone.getName());
            if (part == null) continue;
            List<ModelPart.Cube> baked = ((ModelPartAccessor) (Object) part).bedrockSkins$getCubes();
            List<BedrockCube> source = bone.getCubes() != null ? bone.getCubes() : List.of();
            int count = Math.min(baked.size(), source.size());
            for (int cubeIndex = 0; cubeIndex < count; cubeIndex++) {
                BedrockCube sourceCube = source.get(cubeIndex);
                if (sourceCube.getUv() instanceof Map<?, ?> faces && isPerFaceUv(faces)) {
                    applyCubeFaceUvs(baked.get(cubeIndex), sourceCube, faces, textureWidth, textureHeight);
                }
            }
        }
    }

    private static boolean isPerFaceUv(Map<?, ?> faces) {
        for (String key : FACE_KEYS) if (faces.containsKey(key)) return true;
        return false;
    }

    private static void applyCubeFaceUvs(ModelPart.Cube cube, BedrockCube sourceCube, Map<?, ?> faces,
                                         int textureWidth, int textureHeight) {
        ((CustomFaceCube) (Object) cube).bedrockSkins$useLivePolygons();
        for (int faceIndex = 0; faceIndex < Math.min(FACE_KEYS.length, cube.polygons.length); faceIndex++) {
            ModelPart.Polygon polygon = cube.polygons[faceIndex];
            String targetFace = FACE_KEYS[faceIndex];
            String sourceFace = switch (targetFace) {
                case "west" -> "east";
                case "east" -> "west";
                default -> targetFace;
            };
            float[] rect = faceRect(faces.get(sourceFace), sourceCube, sourceFace, textureWidth, textureHeight);
            float u1 = rect[0] / textureWidth;
            float v1 = rect[1] / textureHeight;
            float u2 = rect[2] / textureWidth;
            float v2 = rect[3] / textureHeight;
            ModelPart.Vertex[] oldVertices = polygon.vertices();
            ModelPart.Vertex[] vertices = new ModelPart.Vertex[oldVertices.length];
            for (int i = 0; i < oldVertices.length; i++) {
                ModelPart.Vertex old = oldVertices[i];
                float[] uv = faceUv(targetFace, old, oldVertices, u1, v1, u2, v2);
                vertices[i] = new ModelPart.Vertex(old.x(), old.y(), old.z(), uv[0], uv[1]);
            }
            cube.polygons[faceIndex] = new ModelPart.Polygon(vertices, polygon.normal());
        }
    }

    private static float[] faceUv(String face, ModelPart.Vertex vertex, ModelPart.Vertex[] vertices,
                                  float u1, float v1, float u2, float v2) {
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (ModelPart.Vertex value : vertices) {
            minX = Math.min(minX, value.x()); maxX = Math.max(maxX, value.x());
            minY = Math.min(minY, value.y()); maxY = Math.max(maxY, value.y());
            minZ = Math.min(minZ, value.z()); maxZ = Math.max(maxZ, value.z());
        }

        boolean lowX = Math.abs(vertex.x() - minX) <= Math.abs(vertex.x() - maxX);
        boolean lowY = Math.abs(vertex.y() - minY) <= Math.abs(vertex.y() - maxY);
        boolean lowZ = Math.abs(vertex.z() - minZ) <= Math.abs(vertex.z() - maxZ);
        return switch (face) {
            case "north" -> new float[] {lowX ? u1 : u2, lowY ? v1 : v2};
            case "south" -> new float[] {lowX ? u2 : u1, lowY ? v1 : v2};
            case "west" -> new float[] {lowZ ? u2 : u1, lowY ? v1 : v2};
            case "east" -> new float[] {lowZ ? u1 : u2, lowY ? v1 : v2};
            case "up" -> new float[] {lowX ? u2 : u1, lowZ ? v2 : v1};
            case "down" -> new float[] {lowX ? u1 : u2, lowZ ? v1 : v2};
            default -> new float[] {u1, v1};
        };
    }

    private static float[] faceRect(Object value, BedrockCube cube, String face,
                                    int textureWidth, int textureHeight) {
        if (!(value instanceof Map<?, ?> data) || !(data.get("uv") instanceof List<?> uv) || uv.size() < 2) {
            return new float[] {textureWidth - 1, textureHeight - 1, textureWidth - 1, textureHeight - 1};
        }
        float u = number(uv.get(0));
        float v = number(uv.get(1));
        float[] defaultSize = defaultFaceSize(cube, face);
        float width = defaultSize[0];
        float height = defaultSize[1];
        if (data.get("uv_size") instanceof List<?> size && size.size() >= 2) {
            width = number(size.get(0));
            height = number(size.get(1));
        }
        return new float[] {u, v, u + width, v + height};
    }

    private static float[] defaultFaceSize(BedrockCube cube, String face) {
        float x = listValue(cube.getSize(), 0);
        float y = listValue(cube.getSize(), 1);
        float z = listValue(cube.getSize(), 2);
        return switch (face) {
            case "up", "down" -> new float[] {x, z};
            case "west", "east" -> new float[] {z, y};
            default -> new float[] {x, y};
        };
    }

    private static float number(Object value) {
        return value instanceof Number number ? number.floatValue() : 0.0f;
    }

    private static void applyPolyMeshes(List<BedrockBone> bones, Map<String, ModelPart> parts,
                                        int textureWidth, int textureHeight) {
        for (BedrockBone bone : bones) {
            List<BedrockPolyMesh> meshes = bone.getPolyMeshes() != null ? bone.getPolyMeshes() : List.of();
            if (meshes.isEmpty()) continue;
            ModelPart part = parts.get(bone.getName());
            if (part == null) continue;
            List<ModelPart.Cube> baked = ((ModelPartAccessor) (Object) part).bedrockSkins$getCubes();
            int cubeIndex = bone.getCubes() != null ? bone.getCubes().size() : 0;
            int polygonIndex = 0;
            for (BedrockPolyMesh mesh : meshes) {
                if (mesh.getPolys() == null) continue;
                for (List<List<Integer>> polygonData : mesh.getPolys()) {
                    if (polygonData == null || polygonData.size() < 3) continue;
                    ModelPart.Polygon polygon = bakePoly(mesh, polygonData, bone, textureWidth, textureHeight);
                    if (polygon == null) continue;
                    int targetCube = cubeIndex + polygonIndex / 6;
                    int targetFace = polygonIndex % 6;
                    if (targetCube < baked.size() && targetFace < baked.get(targetCube).polygons.length) {
                        ModelPart.Cube cube = baked.get(targetCube);
                        cube.polygons[targetFace] = polygon;
                        ((CustomFaceCube) (Object) cube).bedrockSkins$useLivePolygons();
                    }
                    polygonIndex++;
                }
            }
        }
    }

    private static ModelPart.Polygon bakePoly(BedrockPolyMesh mesh, List<List<Integer>> polygonData,
                                               BedrockBone bone, int textureWidth, int textureHeight) {
        if (mesh.getPositions() == null || mesh.getUvs() == null) return null;
        float pivotX = listValue(bone.getPivot(), 0);
        float pivotY = listValue(bone.getPivot(), 1);
        float pivotZ = listValue(bone.getPivot(), 2);
        ModelPart.Vertex[] vertices = new ModelPart.Vertex[polygonData.size()];
        Vector3f normal = null;
        for (int i = 0; i < polygonData.size(); i++) {
            List<Integer> indices = polygonData.get(i);
            if (indices == null || indices.size() < 3) return null;
            int positionIndex = indices.get(0);
            int normalIndex = indices.get(1);
            int uvIndex = indices.get(2);
            if (positionIndex < 0 || positionIndex >= mesh.getPositions().size()
                || uvIndex < 0 || uvIndex >= mesh.getUvs().size()) return null;
            List<Float> position = mesh.getPositions().get(positionIndex);
            List<Float> uv = mesh.getUvs().get(uvIndex);
            if (position.size() < 3 || uv.size() < 2) return null;
            vertices[i] = new ModelPart.Vertex(
                listValue(position, 0) - pivotX,
                pivotY - listValue(position, 1),
                listValue(position, 2) - pivotZ,
                listValue(uv, 0) / textureWidth,
                listValue(uv, 1) / textureHeight);
            if (normal == null && mesh.getNormals() != null
                && normalIndex >= 0 && normalIndex < mesh.getNormals().size()) {
                List<Float> value = mesh.getNormals().get(normalIndex);
                normal = new Vector3f(listValue(value, 0), -listValue(value, 1), listValue(value, 2));
            }
        }
        if (normal == null) normal = calculateNormal(vertices);
        return new ModelPart.Polygon(vertices, normal);
    }

    private static Vector3f calculateNormal(ModelPart.Vertex[] vertices) {
        if (vertices.length < 3) return new Vector3f(0, 1, 0);
        Vector3f first = new Vector3f(vertices[1].x() - vertices[0].x(),
            vertices[1].y() - vertices[0].y(), vertices[1].z() - vertices[0].z());
        Vector3f second = new Vector3f(vertices[2].x() - vertices[0].x(),
            vertices[2].y() - vertices[0].y(), vertices[2].z() - vertices[0].z());
        return first.cross(second).normalize();
    }

    private static float listValue(List<Float> list, int index) {
        return list != null && list.size() > index && list.get(index) != null ? list.get(index) : 0f;
    }
}
