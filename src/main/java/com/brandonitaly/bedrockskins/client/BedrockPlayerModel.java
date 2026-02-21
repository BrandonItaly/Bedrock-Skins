package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.bedrock.BedrockBone;
import com.brandonitaly.bedrockskins.bedrock.BedrockCube;
import com.brandonitaly.bedrockskins.bedrock.BedrockGeometry;
import com.brandonitaly.bedrockskins.bedrock.GeometryDescription;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
//? if >=1.21.11 {
import net.minecraft.client.model.player.PlayerModel;
//?} else {
/*import net.minecraft.client.model.PlayerModel;*/
//?}
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import java.util.*;
import java.util.stream.Collectors;

public class BedrockPlayerModel extends PlayerModel {
    public final Map<String, ModelPart> partsMap;
    public final Map<String, PartTransform> defaultTransforms;
    
    private final boolean animationArmsOutFront;
    private final boolean animationSingleArmAnimation;
    private final boolean animationStationaryLegs;
    private final boolean animationSingleLegAnimation;
    private final boolean animationDontShowArmor;

    public BedrockPlayerModel(ModelPart root, boolean thinArms, Map<String, ModelPart> partsMap, Map<String, PartTransform> defaultTransforms, boolean animationArmsOutFront, boolean animationSingleArmAnimation, boolean animationStationaryLegs, boolean animationSingleLegAnimation, boolean animationDontShowArmor) {
        super(root, thinArms);
        this.partsMap = Map.copyOf(partsMap);
        this.defaultTransforms = Map.copyOf(defaultTransforms);
        this.animationArmsOutFront = animationArmsOutFront;
        this.animationSingleArmAnimation = animationSingleArmAnimation;
        this.animationStationaryLegs = animationStationaryLegs;
        this.animationSingleLegAnimation = animationSingleLegAnimation;
        this.animationDontShowArmor = animationDontShowArmor;
    }

    public record PartTransform(float x, float y, float z, float pitch, float yaw, float roll) {}

    private static final Set<String> VANILLA_ROOT_PARTS = Set.of(
        PartNames.HEAD, PartNames.BODY, PartNames.RIGHT_ARM, PartNames.LEFT_ARM, PartNames.RIGHT_LEG, PartNames.LEFT_LEG
    );

    private static final String[][] REQUIRED_BONES = {
        {"head", "body"}, {"hat", "head"}, {"body", null}, {"jacket", "body"},
        {"leftArm", "body"}, {"leftSleeve", "leftArm"}, {"rightArm", "body"}, {"rightSleeve", "rightArm"},
        {"leftLeg", "body"}, {"leftPants", "leftLeg"}, {"rightLeg", "body"}, {"rightPants", "rightLeg"}
    };

    public static BedrockPlayerModel create(BedrockGeometry geometry, boolean thinArms) {
        BedrockGeometry normalized = normalizeGeometry(geometry);
        ensureRequiredBones(normalized);
        BuildRootResult result = buildRoot(normalized);
        
        return new BedrockPlayerModel(
            result.root(), thinArms, result.parts(), result.defaults(),
            Boolean.TRUE.equals(normalized.getAnimationArmsOutFront()),
            Boolean.TRUE.equals(normalized.getAnimationSingleArmAnimation()),
            Boolean.TRUE.equals(normalized.getAnimationStationaryLegs()),
            Boolean.TRUE.equals(normalized.getAnimationSingleLegAnimation()),
            Boolean.TRUE.equals(normalized.getAnimationDontShowArmor())
        );
    }

    private static BedrockGeometry normalizeGeometry(BedrockGeometry geometry) {
        if (geometry == null) throw new IllegalArgumentException("geometry cannot be null");
        if (geometry.getDescription() == null) geometry.setDescription(new GeometryDescription());
        
        GeometryDescription desc = geometry.getDescription();
        if (desc.getTextureWidth() <= 0) desc.setTextureWidth(64);
        if (desc.getTextureHeight() <= 0) desc.setTextureHeight(64);
        
        if (geometry.getBones() == null) geometry.setBones(new ArrayList<>());
        return geometry;
    }

    private static void ensureRequiredBones(BedrockGeometry geometry) {
        Set<String> existing = geometry.getBones().stream()
            .map(BedrockBone::getName).filter(Objects::nonNull).map(String::toLowerCase)
            .collect(Collectors.toSet());

        for (String[] req : REQUIRED_BONES) {
            String name = req[0];
            if (!existing.contains(name.toLowerCase()) && !existing.contains(mapBoneName(name).toLowerCase())) {
                BedrockBone bone = new BedrockBone();
                bone.setName(name);
                bone.setParent(req[1]);
                bone.setPivot(List.of(0f, 0f, 0f));
                bone.setRotation(List.of(0f, 0f, 0f));
                bone.setCubes(List.of());
                bone.setMirror(false);
                geometry.getBones().add(bone);
            }
        }
    }

    private record BuildRootResult(ModelPart root, Map<String, ModelPart> parts, Map<String, PartTransform> defaults) {}

    private record BoneNode(String name, String parent, float pivotX, float pivotY, float pivotZ,
                            float rotX, float rotY, float rotZ, Float inflate, Boolean mirror,
                            List<BedrockCube> cubes) {
        BoneNode(BedrockBone bone) {
            this(bone.getName(), bone.getParent(),
                 getListValue(bone.getPivot(), 0), getListValue(bone.getPivot(), 1), getListValue(bone.getPivot(), 2),
                 toRadiansNeg(getListValue(bone.getRotation(), 0)), toRadiansNeg(getListValue(bone.getRotation(), 1)), toRadiansNeg(getListValue(bone.getRotation(), 2)),
                 bone.getInflate(), bone.getMirror(),
                 bone.getCubes() != null ? bone.getCubes() : List.of());
        }
    }

    private static BuildRootResult buildRoot(BedrockGeometry geometry) {
        Map<String, BoneNode> nodes = new LinkedHashMap<>();
        for (BedrockBone bone : geometry.getBones()) {
            if (bone.getName() != null) nodes.putIfAbsent(bone.getName(), new BoneNode(bone));
        }

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootData = mesh.getRoot();
        addVanillaScaffold(rootData);

        Map<String, PartDefinition> partDefs = new HashMap<>();
        Map<String, PartTransform> defaultTransforms = new HashMap<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>(nodes.keySet());

        while (!stack.isEmpty()) {
            String name = stack.peek();
            BoneNode node = nodes.get(name);
            
            if (visited.contains(name) || node == null) {
                stack.pop();
                continue;
            }
            
            if (node.parent() != null && nodes.containsKey(node.parent()) && !visited.contains(node.parent())) {
                stack.push(node.parent());
                continue;
            }
            
            buildPart(node, nodes, rootData, partDefs, defaultTransforms);
            visited.add(name);
            stack.pop();
        }

        LayerDefinition layer = LayerDefinition.create(mesh, geometry.getDescription().getTextureWidth(), geometry.getDescription().getTextureHeight());
        ModelPart rootPart = layer.bakeRoot();

        Map<String, ModelPart> finalParts = new HashMap<>();
        for (String boneName : nodes.keySet()) {
            ModelPart part = resolvePart(rootPart, boneName, nodes);
            if (part != null) {
                finalParts.put(boneName, part);
                String alias = mapBoneName(boneName);
                if (!Objects.equals(alias, boneName)) finalParts.put(alias, part);
            }
        }

        return new BuildRootResult(rootPart, finalParts, defaultTransforms);
    }

    private static void addVanillaScaffold(PartDefinition root) {
        root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create(), PartPose.ZERO)
            .addOrReplaceChild(PartNames.HAT, CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create(), PartPose.ZERO)
            .addOrReplaceChild(PartNames.JACKET, CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create(), PartPose.ZERO)
            .addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create(), PartPose.ZERO)
            .addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create(), PartPose.ZERO)
            .addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create(), PartPose.ZERO)
            .addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);
    }

    private static void buildPart(BoneNode node, Map<String, BoneNode> nodes, PartDefinition rootData, Map<String, PartDefinition> partDefs, Map<String, PartTransform> defaultTransforms) {
        String mappedName = mapBoneName(node.name());
        boolean forceRoot = VANILLA_ROOT_PARTS.contains(mappedName);
        PartDefinition parentDef = (node.parent() != null && !forceRoot) ? partDefs.getOrDefault(node.parent(), rootData) : rootData;

        float localX = node.pivotX();
        float localY = 24f - node.pivotY();
        float localZ = node.pivotZ();

        if (node.parent() != null && parentDef != rootData) {
            BoneNode parent = nodes.get(node.parent());
            if (parent != null) {
                localX -= parent.pivotX();
                localY -= (24f - parent.pivotY());
                localZ -= parent.pivotZ();
            }
        }

        PartDefinition partDef = parentDef.addOrReplaceChild(mappedName, buildCubeList(node), PartPose.offsetAndRotation(localX, localY, localZ, node.rotX(), node.rotY(), node.rotZ()));
        partDefs.put(node.name(), partDef);
        
        PartTransform transform = new PartTransform(localX, localY, localZ, node.rotX(), node.rotY(), node.rotZ());
        defaultTransforms.put(node.name(), transform);
        if (!Objects.equals(mappedName, node.name())) defaultTransforms.put(mappedName, transform);
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
        return builder;
    }

    private static int[] readUv(Object uvObj) {
        if (uvObj instanceof List<?> list && list.size() >= 2) {
            return new int[] { ((Number) list.get(0)).intValue(), ((Number) list.get(1)).intValue() };
        } else if (uvObj instanceof Map<?, ?> map && map.get("uv") instanceof List<?> list && list.size() >= 2) {
            return new int[] { ((Number) list.get(0)).intValue(), ((Number) list.get(1)).intValue() };
        }
        return new int[] { 0, 0 };
    }

    private static float getListValue(List<Float> list, int index) {
        return (list != null && list.size() > index && list.get(index) != null) ? list.get(index) : 0f;
    }

    private static float toRadiansNeg(float degrees) {
        return (float) Math.toRadians(-degrees);
    }

    private static ModelPart resolvePart(ModelPart root, String boneName, Map<String, BoneNode> nodes) {
        String mapped = mapBoneName(boneName);
        if (root.hasChild(mapped)) return root.getChild(mapped);
        
        BoneNode current = nodes.get(boneName);
        if (current == null) return null;
        
        List<String> path = new ArrayList<>();
        while (current != null) {
            path.add(mapBoneName(current.name()));
            current = current.parent() != null ? nodes.get(current.parent()) : null;
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

    public void setBedrockPartVisible(String partName, boolean visible) {
        ModelPart part = resolvePart(partName);
        if (part != null) part.visible = visible;
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        
        if (animationArmsOutFront) {
            applyArmsOutFront(resolvePart("rightArm", PartNames.RIGHT_ARM), resolvePart("leftArm", PartNames.LEFT_ARM), state);
        } else if (animationSingleArmAnimation) {
            ModelPart leftArm = resolvePart("leftArm", PartNames.LEFT_ARM);
            if (leftArm != null) leftArm.xRot -= computeWalkSwing(state, false) * 2.0F;
        }
        
        if (animationStationaryLegs) {
            resetLegAngle("rightLeg", PartNames.RIGHT_LEG);
            resetLegAngle("leftLeg", PartNames.LEFT_LEG);
        } else if (animationSingleLegAnimation) {
            ModelPart rightLeg = resolvePart("rightLeg", PartNames.RIGHT_LEG);
            ModelPart leftLeg = resolvePart("leftLeg", PartNames.LEFT_LEG);
            if (rightLeg != null && leftLeg != null) {
                leftLeg.xRot = rightLeg.xRot;
                leftLeg.yRot = rightLeg.yRot;
                leftLeg.zRot = rightLeg.zRot;
            }
        }
    }

    private void applyArmsOutFront(ModelPart rightArm, ModelPart leftArm, AvatarRenderState state) {
        boolean attacking = state.attackTime > 0.0F;
        applyArmsOutFrontToArm(rightArm, state.rightArmPose, state.ageInTicks, 1.0F, computeWalkSwing(state, true), attacking);
        applyArmsOutFrontToArm(leftArm, state.leftArmPose, state.ageInTicks, -1.0F, computeWalkSwing(state, false), attacking);
    }

    private float computeWalkSwing(AvatarRenderState state, boolean rightArm) {
        float speedValue = state.speedValue != 0f ? state.speedValue : 1f;
        float walkSwingScale = 2.0F * state.walkAnimationSpeed * 0.5F / speedValue;
        float phase = rightArm ? (float) Math.PI : 0f;
        return (float) (Math.cos(state.walkAnimationPos * 0.6662F + phase) * walkSwingScale);
    }

    private void applyArmsOutFrontToArm(ModelPart arm, HumanoidModel.ArmPose pose, float ageInTicks, float bobDirection, float walkSwing, boolean attacking) {
        if (arm == null || (pose != null && pose != HumanoidModel.ArmPose.EMPTY && pose != HumanoidModel.ArmPose.ITEM)) return;

        // Remove idle arm bob
        arm.zRot -= (Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F) * bobDirection;
        arm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F * bobDirection;
        
        if (pose == HumanoidModel.ArmPose.ITEM && !attacking) {
            arm.xRot = (arm.xRot + ((float) Math.PI / 10.0F)) * 2.0F;
            arm.yRot = 0.0F;
            arm.zRot = 0.0F;
        }
        
        arm.xRot -= walkSwing;
        arm.xRot += -1.5707964f; // Arms out offset
    }

    private void resetLegAngle(String primaryKey, String fallbackKey) {
        ModelPart leg = resolvePart(primaryKey, fallbackKey);
        PartTransform def = defaultTransforms.getOrDefault(primaryKey, defaultTransforms.get(fallbackKey));
        if (leg != null && def != null) {
            leg.xRot = def.pitch();
            leg.yRot = def.yaw();
            leg.zRot = def.roll();
        }
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
    }

    public boolean shouldHideArmor() { return animationDontShowArmor; }

    private void copyRotation(String name, ModelPart source) {
        if (source == null) return;
        ModelPart dest = resolvePart(name);
        if (dest != null) {
            dest.xRot = source.xRot;
            dest.yRot = source.yRot;
            dest.zRot = source.zRot;
        }
    }

    private ModelPart resolvePart(String name) {
        return partsMap.getOrDefault(name, partsMap.get(mapBoneName(name)));
    }

    private ModelPart resolvePart(String primary, String fallback) {
        ModelPart part = resolvePart(primary);
        return part != null ? part : resolvePart(fallback);
    }
}