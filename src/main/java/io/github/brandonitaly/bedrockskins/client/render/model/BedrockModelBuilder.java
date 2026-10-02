package io.github.brandonitaly.bedrockskins.client.render.model;

import io.github.brandonitaly.bedrockskins.bedrock.BedrockBone;
import io.github.brandonitaly.bedrockskins.bedrock.BedrockCube;
import io.github.brandonitaly.bedrockskins.bedrock.BedrockGeometry;
import io.github.brandonitaly.bedrockskins.bedrock.BedrockPolyMesh;
import io.github.brandonitaly.bedrockskins.bedrock.GeometryDescription;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.*;

/** Prepares Bedrock geometry and constructs the vanilla-compatible model hierarchy. */
final class BedrockModelBuilder {
    private final BedrockGeometry geometry;
    private final Map<String, BoneNode> nodes = new LinkedHashMap<>();
    private final Map<String, String> boneNames = new HashMap<>();
    private final MeshDefinition mesh = new MeshDefinition();
    private final PartDefinition rootData = mesh.getRoot();
    private final Map<String, PartDefinition> partDefs = new HashMap<>();
    private final Set<String> building = new HashSet<>();

    private BedrockModelBuilder(BedrockGeometry geometry) {
        this.geometry = geometry;
        for (BedrockBone bone : geometry.getBones()) {
            if (bone.getName() != null) nodes.putIfAbsent(bone.getName(), new BoneNode(bone));
        }
        addMissingParts();
        nodes.keySet().forEach(name -> boneNames.putIfAbsent(mapBoneName(name), name));
    }

    record Result(ModelPart root, Map<String, ModelPart> parts, float heightMultiplier) {}

    static Result build(BedrockGeometry geometry) {
        normalizeGeometry(geometry);
        splitArmorBones(geometry);
        return new BedrockModelBuilder(geometry).buildRoot();
    }

    private float heightMultiplier() {
        var headBones = geometry.getBones().stream()
            .filter(bone -> bone.getName() != null && "head".equalsIgnoreCase(mapBoneName(bone.getName())))
            .toList();
        float headPivotY = (float) headBones.stream()
            .map(BedrockBone::getPivot)
            .filter(pivot -> pivot != null && pivot.size() >= 2)
            .mapToDouble(pivot -> pivot.get(1))
            .findFirst().orElse(headBones.isEmpty() ? 0.0 : 24.0);
        return Math.max(headPivotY / 24.0f, 0.001f);
    }

    private static final Set<String> VANILLA_ROOT_PARTS = Set.of(
        PartNames.HEAD, PartNames.BODY, PartNames.RIGHT_ARM, PartNames.LEFT_ARM, PartNames.RIGHT_LEG, PartNames.LEFT_LEG
    );

    private static final String[][] REQUIRED_BONES = {
        {"head", "body"}, {"hat", "head"}, {"body", null}, {"jacket", "body"},
        {"leftArm", "body"}, {"leftSleeve", "leftArm"}, {"rightArm", "body"}, {"rightSleeve", "rightArm"},
        {"leftLeg", "body"}, {"leftPants", "leftLeg"}, {"rightLeg", "body"}, {"rightPants", "rightLeg"}
    };

    private static void normalizeGeometry(BedrockGeometry geometry) {
        if (geometry == null) throw new IllegalArgumentException("geometry cannot be null");
        if (geometry.getDescription() == null) geometry.setDescription(new GeometryDescription());

        GeometryDescription desc = geometry.getDescription();
        if (desc.getTextureWidth() <= 0) desc.setTextureWidth(64);
        if (desc.getTextureHeight() <= 0) desc.setTextureHeight(64);
        if (geometry.getBones() == null) geometry.setBones(new ArrayList<>());
    }

    private void addMissingParts() {
        Set<String> existing = new HashSet<>();
        nodes.keySet().forEach(name -> existing.add(mapBoneName(name)));
        for (String[] req : REQUIRED_BONES) {
            String name = req[0];
            if (!existing.contains(mapBoneName(name))) {
                nodes.put(name, new BoneNode(req[1], 0, 0, 0, 0, 0, 0, null, false, List.of(), List.of()));
            }
        }
    }

    private record BoneNode(String parent, float pivotX, float pivotY, float pivotZ,
                             float rotX, float rotY, float rotZ, Float inflate, Boolean mirror,
                             List<BedrockCube> cubes, List<BedrockPolyMesh> polyMeshes) {
        BoneNode(BedrockBone bone) {
            this(bone.getParent(),
                 getListValue(bone.getPivot(), 0), getListValue(bone.getPivot(), 1), getListValue(bone.getPivot(), 2),
                 (float) Math.toRadians(getListValue(bone.getRotation(), 0)),
                 (float) Math.toRadians(getListValue(bone.getRotation(), 1)),
                 (float) Math.toRadians(getListValue(bone.getRotation(), 2)),
                 bone.getInflate(), bone.getMirror(),
                 bone.getCubes() != null ? bone.getCubes() : List.of(),
                 bone.getPolyMeshes() != null ? bone.getPolyMeshes() : List.of());
        }
    }

    private Result buildRoot() {
        // Recursively construct hierarchy ensuring parents build before children
        nodes.keySet().forEach(this::getOrCreatePartDef);
        // Authored overlays can live elsewhere, but PlayerModel still requires these children.
        for (String[] required : REQUIRED_BONES) {
            String name = mapBoneName(required[0]);
            if (VANILLA_ROOT_PARTS.contains(name)) continue;
            PartDefinition parent = rootData.getChild(mapBoneName(required[1]));
            if (parent.getChild(name) == null) parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        }

        LayerDefinition layer = LayerDefinition.create(mesh, geometry.getDescription().getTextureWidth(), geometry.getDescription().getTextureHeight());
        ModelPart rootPart = layer.bakeRoot();

        Map<PartDefinition, ModelPart> bakedParts = new IdentityHashMap<>();
        indexParts(rootData, rootPart, bakedParts);
        Map<String, ModelPart> finalParts = new HashMap<>();
        for (String boneName : nodes.keySet()) {
            ModelPart part = bakedParts.get(partDefs.get(boneName));
            if (part != null) {
                finalParts.put(boneName, part);
                String alias = mapBoneName(boneName);
                if (!Objects.equals(alias, boneName)) finalParts.put(alias, part);
            }
        }

        return new Result(rootPart, finalParts, heightMultiplier());
    }

    private PartDefinition getOrCreatePartDef(String name) {
        PartDefinition existing = partDefs.get(name);
        if (existing != null) return existing;
        if (!building.add(name)) throw new IllegalArgumentException("Cyclic bone hierarchy at " + name);
        BoneNode node = nodes.get(name);

        String mappedName = mapBoneName(name);
        boolean forceRoot = VANILLA_ROOT_PARTS.contains(mappedName);
        PartDefinition parentDef = rootData;
        String parentName = node.parent() == null ? null : boneNames.get(mapBoneName(node.parent()));

        if (parentName != null && !forceRoot) {
            parentDef = getOrCreatePartDef(parentName);
        }

        float localX = node.pivotX();
        float localY = 24f - node.pivotY();
        float localZ = node.pivotZ();

        if (parentDef != rootData) {
            BoneNode parent = nodes.get(parentName);
            if (parent != null) {
                localX -= parent.pivotX();
                localY -= (24f - parent.pivotY());
                localZ -= parent.pivotZ();
            }
        }

        PartDefinition partDef = parentDef.addOrReplaceChild(mappedName, buildCubeList(node),
                PartPose.offsetAndRotation(localX, localY, localZ, node.rotX(), node.rotY(), node.rotZ()));

        partDefs.put(name, partDef);
        building.remove(name);

        return partDef;
    }

    private static void indexParts(PartDefinition definition, ModelPart part, Map<PartDefinition, ModelPart> parts) {
        parts.put(definition, part);
        for (var child : definition.getChildren()) {
            indexParts(child.getValue(), part.getChild(child.getKey()), parts);
        }
    }

    private static CubeListBuilder buildCubeList(BoneNode node) {
        CubeListBuilder builder = CubeListBuilder.create();
        for (BedrockCube cube : node.cubes()) {
            int[] uv = readUv(cube.getUv());
            float inflate = (node.inflate() != null ? node.inflate() : 0f) + (cube.getInflate() != null ? cube.getInflate() : 0f);
            boolean mirror = cube.getMirror() != null ? cube.getMirror() : Boolean.TRUE.equals(node.mirror());

            float offsetX = getListValue(cube.getOrigin(), 0) - node.pivotX();
            float offsetY = node.pivotY() - getListValue(cube.getOrigin(), 1) - getListValue(cube.getSize(), 1);
            float offsetZ = getListValue(cube.getOrigin(), 2) - node.pivotZ();

            builder.mirror(mirror).texOffs(uv[0], uv[1])
                   .addBox(offsetX, offsetY, offsetZ, getListValue(cube.getSize(), 0), getListValue(cube.getSize(), 1), getListValue(cube.getSize(), 2), new CubeDeformation(inflate));
        }
        int polygonCount = 0;
        for (BedrockPolyMesh mesh : node.polyMeshes()) {
            if (mesh.getPolys() != null) polygonCount += mesh.getPolys().size();
        }
        for (int i = 0; i < (polygonCount + 5) / 6; i++) {
            builder.texOffs(0, 0).addBox(0, 0, 0, 0, 0, 0);
        }
        return builder;
    }

    private static int[] readUv(Object uvObj) {
        if (uvObj instanceof Map<?, ?> map) uvObj = map.get("uv");
        if (uvObj instanceof List<?> list && list.size() >= 2) {
            return new int[] { ((Number) list.get(0)).intValue(), ((Number) list.get(1)).intValue() };
        }
        return new int[] { 0, 0 };
    }

    private static float getListValue(List<Float> list, int index) {
        return (list != null && list.size() > index && list.get(index) != null) ? list.get(index) : 0f;
    }

    static String mapBoneName(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "head" -> PartNames.HEAD;
            case "hat" -> PartNames.HAT;
            case "body" -> PartNames.BODY;
            case "jacket" -> PartNames.JACKET;
            case "rightarm" -> PartNames.RIGHT_ARM;
            case "leftarm" -> PartNames.LEFT_ARM;
            case "rightleg" -> PartNames.RIGHT_LEG;
            case "leftleg" -> PartNames.LEFT_LEG;
            case "rightsleeve" -> "right_sleeve";
            case "leftsleeve" -> "left_sleeve";
            case "rightpants" -> "right_pants";
            case "leftpants" -> "left_pants";
            default -> name;
        };
    }

    private static void splitArmorBones(BedrockGeometry geometry) {
        if (geometry == null || geometry.getBones() == null) return;
        List<BedrockBone> newBones = new ArrayList<>();
        for (BedrockBone bone : geometry.getBones()) {
            if (bone.getCubes() == null || bone.getCubes().isEmpty()) continue;

            List<BedrockCube> remainingCubes = new ArrayList<>();
            Map<String, List<BedrockCube>> armorGroups = new HashMap<>();

            for (BedrockCube cube : bone.getCubes()) {
                int mask = cube.getArmorMask();
                if (mask == 0) {
                    remainingCubes.add(cube);
                } else {
                    String subBoneName = getSubBoneName(bone.getName(), mask);
                    armorGroups.computeIfAbsent(subBoneName, k -> new ArrayList<>()).add(cube);
                }
            }

            bone.setCubes(remainingCubes);

            for (Map.Entry<String, List<BedrockCube>> entry : armorGroups.entrySet()) {
                String subBoneName = entry.getKey();
                List<BedrockCube> cubes = entry.getValue();

                BedrockBone subBone = new BedrockBone();
                subBone.setName(subBoneName);
                subBone.setParent(bone.getName());
                subBone.setPivot(bone.getPivot() != null ? new ArrayList<>(bone.getPivot()) : List.of(0f, 0f, 0f));
                subBone.setRotation(bone.getRotation() != null ? new ArrayList<>(bone.getRotation()) : List.of(0f, 0f, 0f));
                subBone.setMirror(bone.getMirror());
                subBone.setInflate(bone.getInflate());
                subBone.setCubes(cubes);

                newBones.add(subBone);
            }
        }
        geometry.getBones().addAll(newBones);
    }

    private static String getSubBoneName(String parentName, int mask) {
        if ((mask & 1) != 0) { // HELMET
            if ("head".equalsIgnoreCase(parentName)) return "helmet";
            return parentName + "_helmet";
        }
        if ((mask & 2) != 0) { // CHESTPLATE
            if ("body".equalsIgnoreCase(parentName)) return "bodyArmor";
            if ("rightArm".equalsIgnoreCase(parentName)) return "rightArmArmor";
            if ("leftArm".equalsIgnoreCase(parentName)) return "leftArmArmor";
            return parentName + "_bodyArmor";
        }
        if ((mask & 4) != 0) { // LEGGINGS
            if ("rightLeg".equalsIgnoreCase(parentName)) return "rightLegArmor";
            if ("leftLeg".equalsIgnoreCase(parentName)) return "leftLegArmor";
            return parentName + "_leggings";
        }
        if ((mask & 8) != 0) { // BOOTS
            if ("rightLeg".equalsIgnoreCase(parentName)) return "rightBootArmor";
            if ("leftLeg".equalsIgnoreCase(parentName)) return "leftBootArmor";
            return parentName + "_boots";
        }
        return parentName + "_armor_" + mask;
    }
}
