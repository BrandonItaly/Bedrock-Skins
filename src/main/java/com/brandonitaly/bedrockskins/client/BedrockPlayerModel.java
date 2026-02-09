package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.bedrock.BedrockBone;
import com.brandonitaly.bedrockskins.bedrock.BedrockCube;
import com.brandonitaly.bedrockskins.bedrock.BedrockGeometry;
import com.brandonitaly.bedrockskins.bedrock.GeometryDescription;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
//? if >=1.21.11 {
import net.minecraft.client.model.player.PlayerModel;
//?} else {
/*import net.minecraft.client.model.PlayerModel;*/
//?}
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class BedrockPlayerModel extends PlayerModel {
    public final ModelPart root;
    public final Map<String, ModelPart> partsMap;
    public final Map<String, PartTransform> defaultTransforms;
    public float armorYOffset = 0f;
    public float capeYOffset = 0f;
    public float upperArmorYOffset = 0f;
    private final boolean animationArmsOutFront;
    private final boolean animationStationaryLegs;

    public BedrockPlayerModel(ModelPart root, boolean thinArms, Map<String, ModelPart> partsMap, Map<String, PartTransform> defaultTransforms, boolean animationArmsOutFront, boolean animationStationaryLegs) {
        super(root, thinArms);
        this.root = root;
        this.partsMap = Collections.unmodifiableMap(new HashMap<>(partsMap));
        this.defaultTransforms = Collections.unmodifiableMap(new HashMap<>(defaultTransforms));
        this.animationArmsOutFront = animationArmsOutFront;
        this.animationStationaryLegs = animationStationaryLegs;
    }

    public static class PartTransform {
        public final float x;
        public final float y;
        public final float z;
        public final float pitch;
        public final float yaw;
        public final float roll;

        public PartTransform(float x, float y, float z, float pitch, float yaw, float roll) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }
    }

    private static final Set<String> VANILLA_ROOT_PARTS = new HashSet<>(Arrays.asList(
        PartNames.HEAD,
        PartNames.BODY,
        PartNames.RIGHT_ARM,
        PartNames.LEFT_ARM,
        PartNames.RIGHT_LEG,
        PartNames.LEFT_LEG
    ));

    public static BedrockPlayerModel create(BedrockGeometry geometry, boolean thinArms) {
        BedrockGeometry normalized = normalizeGeometry(geometry);
        ensureRequiredBones(normalized);
        BuildRootResult result = buildRoot(normalized);
        boolean armsOutFront = Boolean.TRUE.equals(normalized.getAnimationArmsOutFront());
        boolean stationaryLegs = Boolean.TRUE.equals(normalized.getAnimationStationaryLegs());
        return new BedrockPlayerModel(result.root, thinArms, result.parts, result.defaults, armsOutFront, stationaryLegs);
    }

    private static BedrockGeometry normalizeGeometry(BedrockGeometry geometry) {
        if (geometry == null) throw new IllegalArgumentException("geometry");
        if (geometry.getDescription() == null) geometry.setDescription(new GeometryDescription());
        GeometryDescription description = geometry.getDescription();
        if (description.getTextureWidth() <= 0) description.setTextureWidth(64);
        if (description.getTextureHeight() <= 0) description.setTextureHeight(64);
        if (geometry.getBones() == null) geometry.setBones(new ArrayList<>());
        return geometry;
    }

    private static void ensureRequiredBones(BedrockGeometry geometry) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("head", "body");
        required.put("hat", "head");
        required.put("body", null);
        required.put("jacket", "body");
        required.put("leftArm", "body");
        required.put("leftSleeve", "leftArm");
        required.put("rightArm", "body");
        required.put("rightSleeve", "rightArm");
        required.put("leftLeg", "body");
        required.put("leftPants", "leftLeg");
        required.put("rightLeg", "body");
        required.put("rightPants", "rightLeg");

        Set<String> existing = new HashSet<>();
        for (BedrockBone bone : geometry.getBones()) {
            if (bone.getName() != null) existing.add(bone.getName().toLowerCase());
        }

        for (Map.Entry<String, String> entry : required.entrySet()) {
            String name = entry.getKey();
            String parent = entry.getValue();
            if (existing.contains(name.toLowerCase())) continue;
            if (existing.contains(mapBoneName(name).toLowerCase())) continue;
            geometry.getBones().add(createEmptyBone(name, parent));
        }
    }

    private static BedrockBone createEmptyBone(String name, String parent) {
        BedrockBone bone = new BedrockBone();
        bone.setName(name);
        bone.setParent(parent);
        bone.setPivot(Arrays.asList(0f, 0f, 0f));
        bone.setRotation(Arrays.asList(0f, 0f, 0f));
        bone.setCubes(Collections.emptyList());
        bone.setMirror(false);
        return bone;
    }

    private static class BuildRootResult {
        final ModelPart root;
        final Map<String, ModelPart> parts;
        final Map<String, PartTransform> defaults;

        BuildRootResult(ModelPart root, Map<String, ModelPart> parts, Map<String, PartTransform> defaults) {
            this.root = root;
            this.parts = parts;
            this.defaults = defaults;
        }
    }

    private static class BoneNode {
        final String name;
        final String parent;
        final float pivotX;
        final float pivotY;
        final float pivotZ;
        final float rotX;
        final float rotY;
        final float rotZ;
        final Float inflate;
        final Boolean mirror;
        final List<BedrockCube> cubes;

        BoneNode(BedrockBone bone) {
            this.name = bone.getName();
            this.parent = bone.getParent();
            float bPx = getListValue(bone.getPivot(), 0);
            float bPy = getListValue(bone.getPivot(), 1);
            float bPz = getListValue(bone.getPivot(), 2);
            this.pivotX = bPx;
            this.pivotY = bPy;
            this.pivotZ = bPz;
            this.rotX = toRadiansNeg(getListValue(bone.getRotation(), 0));
            this.rotY = toRadiansNeg(getListValue(bone.getRotation(), 1));
            this.rotZ = (float) Math.toRadians(getListValue(bone.getRotation(), 2));
            this.inflate = bone.getInflate();
            this.mirror = bone.getMirror();
            this.cubes = bone.getCubes() != null ? bone.getCubes() : Collections.emptyList();
        }
    }

    private static BuildRootResult buildRoot(BedrockGeometry geometry) {
        Map<String, BoneNode> nodes = new LinkedHashMap<>();
        for (BedrockBone bone : geometry.getBones()) {
            if (bone.getName() != null && !nodes.containsKey(bone.getName())) {
                nodes.put(bone.getName(), new BoneNode(bone));
            }
        }

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootData = mesh.getRoot();
        addVanillaScaffold(rootData);

        Map<String, PartDefinition> partDefinitions = new HashMap<>();
        Map<String, PartTransform> defaultTransforms = new HashMap<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>(nodes.keySet());

        while (!stack.isEmpty()) {
            String name = stack.peek();
            if (visited.contains(name)) {
                stack.pop();
                continue;
            }
            BoneNode node = nodes.get(name);
            if (node == null) {
                stack.pop();
                continue;
            }
            if (node.parent != null && nodes.containsKey(node.parent) && !visited.contains(node.parent)) {
                stack.push(node.parent);
                continue;
            }
            buildPart(node, nodes, rootData, partDefinitions, defaultTransforms);
            visited.add(name);
            stack.pop();
        }

        int texWidth = geometry.getDescription().getTextureWidth();
        int texHeight = geometry.getDescription().getTextureHeight();
        LayerDefinition layer = LayerDefinition.create(mesh, texWidth, texHeight);
        ModelPart rootPart = layer.bakeRoot();

        Map<String, ModelPart> finalParts = new HashMap<>();
        for (String boneName : nodes.keySet()) {
            ModelPart part = resolvePart(rootPart, boneName, nodes);
            if (part != null) {
                finalParts.put(boneName, part);
                String alias = mapBoneName(boneName);
                finalParts.put(alias, part);
            }
        }

        return new BuildRootResult(rootPart, finalParts, defaultTransforms);
    }

    private static void addVanillaScaffold(PartDefinition root) {
        PartDefinition head = root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild(PartNames.HAT, CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild(PartNames.JACKET, CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition rightArm = root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        leftArm.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition rightLeg = root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
        leftLeg.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);
    }

    private static void buildPart(BoneNode node, Map<String, BoneNode> nodes, PartDefinition rootData, Map<String, PartDefinition> partDefinitions, Map<String, PartTransform> defaultTransforms) {
        String mappedName = mapBoneName(node.name);
        boolean forceRoot = VANILLA_ROOT_PARTS.contains(mappedName);
        PartDefinition parentDef = (node.parent != null && !forceRoot) ? partDefinitions.get(node.parent) : rootData;
        if (parentDef == null) parentDef = rootData;

        float modelPivotX = node.pivotX;
        float modelPivotY = 24f - node.pivotY;
        float modelPivotZ = node.pivotZ;

        float localX = modelPivotX;
        float localY = modelPivotY;
        float localZ = modelPivotZ;
        if (node.parent != null && parentDef != rootData) {
            BoneNode parent = nodes.get(node.parent);
            if (parent != null) {
                localX -= parent.pivotX;
                localY -= (24f - parent.pivotY);
                localZ -= parent.pivotZ;
            }
        }

        CubeListBuilder builder = buildCubeList(node);
        PartPose pose = PartPose.offsetAndRotation(localX, localY, localZ, node.rotX, node.rotY, node.rotZ);
        PartDefinition partDef = parentDef.addOrReplaceChild(mappedName, builder, pose);

        partDefinitions.put(node.name, partDef);
        defaultTransforms.put(node.name, new PartTransform(localX, localY, localZ, node.rotX, node.rotY, node.rotZ));
        if (!Objects.equals(mappedName, node.name)) {
            defaultTransforms.put(mappedName, new PartTransform(localX, localY, localZ, node.rotX, node.rotY, node.rotZ));
        }
    }

    private static CubeListBuilder buildCubeList(BoneNode node) {
        CubeListBuilder builder = CubeListBuilder.create();
        for (BedrockCube cube : node.cubes) {
            int[] uv = readUv(cube.getUv());
            float inflate = 0f;
            if (node.inflate != null) inflate += node.inflate;
            if (cube.getInflate() != null) inflate += cube.getInflate();
            boolean mirror = cube.getMirror() != null ? cube.getMirror() : Boolean.TRUE.equals(node.mirror);

            float cubeOriginX = getListValue(cube.getOrigin(), 0);
            float cubeOriginY = getListValue(cube.getOrigin(), 1);
            float cubeOriginZ = getListValue(cube.getOrigin(), 2);
            float sizeX = getListValue(cube.getSize(), 0);
            float sizeY = getListValue(cube.getSize(), 1);
            float sizeZ = getListValue(cube.getSize(), 2);

            float offsetX = cubeOriginX - node.pivotX;
            float offsetY = node.pivotY - cubeOriginY - sizeY;
            float offsetZ = cubeOriginZ - node.pivotZ;

            builder.mirror(mirror).texOffs(uv[0], uv[1])
                .addBox(offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, new CubeDeformation(inflate));
        }
        return builder;
    }

    private static int[] readUv(Object uvObj) {
        int u = 0;
        int v = 0;
        if (uvObj instanceof List<?>) {
            List<?> list = (List<?>) uvObj;
            if (list.size() >= 2) {
                u = ((Number) list.get(0)).intValue();
                v = ((Number) list.get(1)).intValue();
            }
        } else if (uvObj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) uvObj;
            Object uvListObj = map.get("uv");
            if (uvListObj instanceof List<?>) {
                List<?> list = (List<?>) uvListObj;
                if (list.size() >= 2) {
                    u = ((Number) list.get(0)).intValue();
                    v = ((Number) list.get(1)).intValue();
                }
            }
        }
        return new int[] { u, v };
    }

    private static float getListValue(List<Float> list, int index) {
        if (list == null || list.size() <= index || list.get(index) == null) return 0f;
        return list.get(index);
    }

    private static float toRadiansNeg(float degrees) {
        return (float) Math.toRadians(-degrees);
    }

    private static ModelPart resolvePart(ModelPart root, String boneName, Map<String, BoneNode> nodes) {
        String mapped = mapBoneName(boneName);
        if (root.hasChild(mapped)) return root.getChild(mapped);
        BoneNode node = nodes.get(boneName);
        if (node == null) return null;
        List<String> path = new ArrayList<>();
        BoneNode current = node;
        while (current != null) {
            path.add(mapBoneName(current.name));
            current = current.parent != null ? nodes.get(current.parent) : null;
        }
        Collections.reverse(path);
        ModelPart part = root;
        for (String segment : path) {
            if (!part.hasChild(segment)) return null;
            part = part.getChild(segment);
        }
        return part;
    }

    public static String mapBoneName(String name) {
        String lower = name.toLowerCase();
        switch (lower) {
            case "head": return PartNames.HEAD;
            case "hat": return PartNames.HAT;
            case "body": return PartNames.BODY;
            case "jacket": return PartNames.JACKET;
            case "rightarm": return PartNames.RIGHT_ARM;
            case "leftarm": return PartNames.LEFT_ARM;
            case "rightleg": return PartNames.RIGHT_LEG;
            case "leftleg": return PartNames.LEFT_LEG;
            case "rightsleeve": return "right_sleeve";
            case "leftsleeve": return "left_sleeve";
            case "rightpants": return "right_pants";
            case "leftpants": return "left_pants";
            default: return name;
        }
    }

    public void setBedrockPartVisible(String partName, boolean visible) {
        ModelPart part = resolvePart(partName);
        if (part != null) part.visible = visible;
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        if (animationArmsOutFront) {
            setArmAngle(resolvePart("rightArm", PartNames.RIGHT_ARM));
            setArmAngle(resolvePart("leftArm", PartNames.LEFT_ARM));
        }
        if (animationStationaryLegs) {
            resetLegAngle("rightLeg", PartNames.RIGHT_LEG);
            resetLegAngle("leftLeg", PartNames.LEFT_LEG);
        }
    }

    private void setArmAngle(ModelPart part) {
        if (part == null) return;
        part.xRot = -1.5707964f;
        part.yRot = 0f;
        part.zRot = 0f;
    }

    private void resetLegAngle(String primaryKey, String fallbackKey) {
        ModelPart leg = resolvePart(primaryKey, fallbackKey);
        PartTransform def = defaultTransforms.getOrDefault(primaryKey, defaultTransforms.get(fallbackKey));
        if (leg == null || def == null) return;
        leg.xRot = def.pitch;
        leg.yRot = def.yaw;
        leg.zRot = def.roll;
    }

    public void copyFromVanilla(PlayerModel vanillaModel) {
        copyRotation("head", vanillaModel.head);
        copyRotation("body", vanillaModel.body);
        copyRotation("hat", vanillaModel.hat);

        if (!animationArmsOutFront) {
            copyRotation("rightArm", vanillaModel.rightArm);
            copyRotation("leftArm", vanillaModel.leftArm);
        }
        if (!animationStationaryLegs) {
            copyRotation("rightLeg", vanillaModel.rightLeg);
            copyRotation("leftLeg", vanillaModel.leftLeg);
        }

        float vanillaBodyPivotY = readInitialPoseY(vanillaModel.body);
        float vanillaHeadPivotY = readInitialPoseY(vanillaModel.head);

        PartTransform bodyTransform = defaultTransforms.getOrDefault("body", defaultTransforms.get(PartNames.BODY));
        PartTransform headTransform = defaultTransforms.getOrDefault("head", defaultTransforms.get(PartNames.HEAD));
        float bedrockBodyY = bodyTransform != null ? bodyTransform.y : 0f;
        float bedrockHeadY = headTransform != null ? headTransform.y : bedrockBodyY;

        upperArmorYOffset = ((bedrockBodyY + bedrockHeadY) * 0.5f) - ((vanillaBodyPivotY + vanillaHeadPivotY) * 0.5f);
        armorYOffset = upperArmorYOffset;

        PartTransform capeTransform = defaultTransforms.get("cape");
        float bedrockCapeY = capeTransform != null ? capeTransform.y : bedrockBodyY;
        capeYOffset = bedrockCapeY - vanillaBodyPivotY;
    }

    private void copyRotation(String name, ModelPart source) {
        if (source == null) return;
        ModelPart dest = resolvePart(name);
        if (dest == null) return;
        dest.xRot = source.xRot;
        dest.yRot = source.yRot;
        dest.zRot = source.zRot;
    }

    private ModelPart resolvePart(String name) {
        ModelPart part = partsMap.get(name);
        if (part == null) part = partsMap.get(mapBoneName(name));
        return part;
    }

    private ModelPart resolvePart(String primary, String fallback) {
        ModelPart part = resolvePart(primary);
        return part != null ? part : resolvePart(fallback);
    }

    private float readInitialPoseY(ModelPart part) {
        if (part == null) return 0f;
        try {
            PartPose pose = part.getInitialPose();
            return pose != null ? pose.y() : 0f;
        } catch (Exception e) {
            return 0f;
        }
    }
}
