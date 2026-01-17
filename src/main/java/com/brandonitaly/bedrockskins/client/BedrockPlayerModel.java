package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.bedrock.BedrockBone;
import com.brandonitaly.bedrockskins.bedrock.BedrockGeometry;
import net.minecraft.client.model.*;
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
import java.util.*;

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
        public final float x, y, z, pitch, yaw, roll;
        public PartTransform(float x, float y, float z, float pitch, float yaw, float roll) { this.x = x; this.y = y; this.z = z; this.pitch = pitch; this.yaw = yaw; this.roll = roll; }
    }

    public static BedrockPlayerModel create(BedrockGeometry geometry, boolean thinArms) {
        validateAndPatchGeometry(geometry);
        BuildRootResult res = buildRoot(geometry);
        return new BedrockPlayerModel(res.root, thinArms, res.parts, res.defaults, geometry.getAnimationArmsOutFront() != null ? geometry.getAnimationArmsOutFront() : false, geometry.getAnimationStationaryLegs() != null ? geometry.getAnimationStationaryLegs() : false);
    }

    private static void validateAndPatchGeometry(BedrockGeometry geometry) {
        Map<String, String> requiredBones = new HashMap<>();
        requiredBones.put("head", "body");
        requiredBones.put("hat", "head");
        requiredBones.put("body", null);
        requiredBones.put("jacket", "body");
        requiredBones.put("leftArm", "body");
        requiredBones.put("leftSleeve", "leftArm");
        requiredBones.put("rightArm", "body");
        requiredBones.put("rightSleeve", "rightArm");
        requiredBones.put("leftLeg", "body");
        requiredBones.put("leftPants", "leftLeg");
        requiredBones.put("rightLeg", "body");
        requiredBones.put("rightPants", "rightLeg");

        List<BedrockBone> currentBones = geometry.getBones() != null ? new ArrayList<>(geometry.getBones()) : new ArrayList<>();
        Set<String> existingBoneNames = new HashSet<>();
        for (BedrockBone b : currentBones) if (b.getName() != null) existingBoneNames.add(b.getName().toLowerCase());

        List<BedrockBone> newBones = new ArrayList<>();
        for (Map.Entry<String, String> e : requiredBones.entrySet()) {
            String boneName = e.getKey();
            String parentName = e.getValue();
            boolean isMissing = !existingBoneNames.contains(boneName.toLowerCase()) && !existingBoneNames.contains(mapBoneName(boneName).toLowerCase());
            if (isMissing) {
                // System.out.println("BedrockPlayerModel: Patching geometry - adding missing bone: " + boneName);
                BedrockBone b = new BedrockBone();
                b.setName(boneName);
                b.setParent(parentName);
                b.setPivot(Arrays.asList(0f, 0f, 0f));
                b.setRotation(Arrays.asList(0f, 0f, 0f));
                b.setCubes(Collections.emptyList());
                b.setMirror(false);
                newBones.add(b);
            }
        }
        if (!newBones.isEmpty()) {
            List<BedrockBone> combined = new ArrayList<>(currentBones);
            combined.addAll(newBones);
            geometry.setBones(combined);
        }
    }

    private static class BuildRootResult {
        final ModelPart root;
        final Map<String, ModelPart> parts;
        final Map<String, PartTransform> defaults;
        BuildRootResult(ModelPart root, Map<String, ModelPart> parts, Map<String, PartTransform> defaults) { this.root = root; this.parts = parts; this.defaults = defaults; }
    }

    private static BuildRootResult buildRoot(BedrockGeometry geometry) {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition rootData = modelData.getRoot();

        Map<String, BedrockBone> boneMap = new HashMap<>();
        if (geometry.getBones() != null) for (BedrockBone b : geometry.getBones()) boneMap.put(b.getName(), b);
        Set<String> processedBones = new HashSet<>();

        if (geometry.getBones() != null) {
            for (BedrockBone b : geometry.getBones()) {
                addBoneRecursively(b.getName(), boneMap, processedBones);
            }
        }

        Map<String, PartDefinition> partDataMap = new HashMap<>();
        Set<String> vanillaRootParts = new HashSet<>(Arrays.asList(
                PartNames.HEAD,
                PartNames.BODY,
                PartNames.RIGHT_ARM,
                PartNames.LEFT_ARM,
                PartNames.RIGHT_LEG,
                PartNames.LEFT_LEG
        ));

        List<BedrockBone> bonesToProcess = geometry.getBones() != null ? new ArrayList<>(geometry.getBones()) : new ArrayList<>();
        Map<String, PartTransform> defaultTransforms = new HashMap<>();

        int stuckCounter = 0;

        while (!bonesToProcess.isEmpty()) {
            Iterator<BedrockBone> iterator = bonesToProcess.iterator();
            boolean processedAny = false;
            while (iterator.hasNext()) {
                BedrockBone bone = iterator.next();
                if (bone.getParent() == null || partDataMap.containsKey(bone.getParent())) {
                    PartDefinition parentData = bone.getParent() == null ? rootData : partDataMap.get(bone.getParent());
                    CubeListBuilder builder = CubeListBuilder.create();

                    if (bone.getCubes() != null) {
                        for (var cube : bone.getCubes()) {
                            int u = 0, v = 0;
                            Object uvObj = cube.getUv();
                            if (uvObj instanceof java.util.List) {
                                var list = (java.util.List<?>) uvObj;
                                if (list.size() >= 2) { u = ((Number) list.get(0)).intValue(); v = ((Number) list.get(1)).intValue(); }
                            } else if (uvObj instanceof java.util.Map) {
                                var map = (java.util.Map<?,?>) uvObj;
                                var uvList = (java.util.List<?>) map.get("uv");
                                if (uvList != null && uvList.size() >= 2) { u = ((Number) uvList.get(0)).intValue(); v = ((Number) uvList.get(1)).intValue(); }
                            }
                            float dilation = cube.getInflate() != null ? cube.getInflate() : 0f;
                            boolean isMirrored = cube.getMirror() != null ? cube.getMirror() : (bone.getMirror() != null ? bone.getMirror() : false);
                            float bPx = bone.getPivot() != null && bone.getPivot().size() > 0 ? bone.getPivot().get(0) : 0f;
                            float bPy = bone.getPivot() != null && bone.getPivot().size() > 1 ? bone.getPivot().get(1) : 0f;
                            float bPz = bone.getPivot() != null && bone.getPivot().size() > 2 ? bone.getPivot().get(2) : 0f;
                            float cOx = cube.getOrigin().get(0);
                            float cOy = cube.getOrigin().get(1);
                            float cOz = cube.getOrigin().get(2);
                            float offX = cOx - bPx;
                            float offY = bPy - cOy - cube.getSize().get(1);
                            float offZ = cOz - bPz;
                            builder.mirror(isMirrored).texOffs(u, v).addBox(offX, offY, offZ, cube.getSize().get(0), cube.getSize().get(1), cube.getSize().get(2), new CubeDeformation(dilation));
                        }
                    }

                    float bPx = bone.getPivot() != null && bone.getPivot().size() > 0 ? bone.getPivot().get(0) : 0f;
                    float bPy = bone.getPivot() != null && bone.getPivot().size() > 1 ? bone.getPivot().get(1) : 0f;
                    float bPz = bone.getPivot() != null && bone.getPivot().size() > 2 ? bone.getPivot().get(2) : 0f;
                    float pX = bPx;
                    float pY = 24f - bPy;
                    float pZ = bPz;
                    String vanillaName = mapBoneName(bone.getName());
                    PartDefinition parentForCreation = (bone.getParent() != null && vanillaRootParts.contains(vanillaName)) ? rootData : parentData;
                    if (bone.getParent() != null && parentForCreation != rootData) {
                        BedrockBone parentBone = boneMap.get(bone.getParent());
                        float ppX = parentBone.getPivot() != null && parentBone.getPivot().size() > 0 ? parentBone.getPivot().get(0) : 0f;
                        float ppY = parentBone.getPivot() != null && parentBone.getPivot().size() > 1 ? 24f - parentBone.getPivot().get(1) : 24f;
                        float ppZ = parentBone.getPivot() != null && parentBone.getPivot().size() > 2 ? parentBone.getPivot().get(2) : 0f;
                        pX -= ppX;
                        pY -= ppY;
                        pZ -= ppZ;
                    }
                    float rotX = (float)Math.toRadians(-(bone.getRotation() != null && bone.getRotation().size() > 0 ? bone.getRotation().get(0) : 0f));
                    float rotY = (float)Math.toRadians(-(bone.getRotation() != null && bone.getRotation().size() > 1 ? bone.getRotation().get(1) : 0f));
                    float rotZ = (float)Math.toRadians((bone.getRotation() != null && bone.getRotation().size() > 2 ? bone.getRotation().get(2) : 0f));
                    PartPose transform = PartPose.offsetAndRotation(pX, pY, pZ, rotX, rotY, rotZ);
                    PartDefinition partData = parentForCreation.addOrReplaceChild(vanillaName, builder, transform);
                    partDataMap.put(bone.getName(), partData);
                    defaultTransforms.put(bone.getName(), new PartTransform(pX, pY, pZ, rotX, rotY, rotZ));
                    if (!vanillaName.equals(bone.getName())) {
                        defaultTransforms.put(vanillaName, new PartTransform(pX, pY, pZ, rotX, rotY, rotZ));
                    }
                    iterator.remove();
                    processedAny = true;
                }
            }
            if (!processedAny) {
                stuckCounter++;
                if (stuckCounter > 5) break;
            }
        }

        LayerDefinition texturedModelData = LayerDefinition.create(modelData, geometry.getDescription().getTextureWidth(), geometry.getDescription().getTextureHeight());
        ModelPart rootPart = texturedModelData.bakeRoot();

        Map<String, ModelPart> finalParts = new HashMap<>();
        for (String name : boneMap.keySet()) {
            ModelPart found = findPart(rootPart, name, boneMap);
            if (found != null) finalParts.put(name, found);
        }

        return new BuildRootResult(rootPart, finalParts, defaultTransforms);
    }

    private static ModelPart findPart(ModelPart parent, String boneName, Map<String, BedrockBone> boneMap) {
        BedrockBone bone = boneMap.get(boneName);
        if (bone == null) return null;
        String mappedRootName = mapBoneName(bone.getName());
        if (parent.hasChild(mappedRootName)) return parent.getChild(mappedRootName);
        List<String> path = new ArrayList<>();
        BedrockBone curr = bone;
        while (curr != null) {
            path.add(mapBoneName(curr.getName()));
            String p = curr.getParent();
            curr = p != null ? boneMap.get(p) : null;
        }
        Collections.reverse(path);
        ModelPart currPart = parent;
        for (String segment : path) {
            if (currPart.hasChild(segment)) currPart = currPart.getChild(segment);
            else return null;
        }
        return currPart;
    }

    private static void addBoneRecursively(String boneName, Map<String, BedrockBone> boneMap, Set<String> processedBones) {
        if (!processedBones.add(boneName)) return;
        BedrockBone bone = boneMap.get(boneName);
        if (bone == null) return;
        if (bone.getParent() != null) addBoneRecursively(bone.getParent(), boneMap, processedBones);
    }

    public static String mapBoneName(String name) {
        String lower = name.toLowerCase();
        switch (lower) {
            case "head": return PartNames.HEAD;
            case "hat":
            case "headwear": return PartNames.HAT;
            case "body": return PartNames.BODY;
            case "jacket": return PartNames.JACKET;
            case "rightarm":
            case "right_arm": return PartNames.RIGHT_ARM;
            case "leftarm":
            case "left_arm": return PartNames.LEFT_ARM;
            case "rightleg":
            case "right_leg": return PartNames.RIGHT_LEG;
            case "leftleg":
            case "left_leg": return PartNames.LEFT_LEG;
            case "rightsleeve":
            case "right_sleeve": return "right_sleeve";
            case "leftsleeve":
            case "left_sleeve": return "left_sleeve";
            case "rightpants":
            case "right_pants": return "right_pants";
            case "leftpants":
            case "left_pants": return "left_pants";
            default: return name;
        }
    }

    public void setBedrockPartVisible(String partName, boolean visible) {
        ModelPart p = partsMap.get(partName);
        if (p != null) p.visible = visible;
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        if (animationArmsOutFront) {
            setArmAngle(partsMap.getOrDefault("rightArm", partsMap.get("right_arm")));
            setArmAngle(partsMap.getOrDefault("leftArm", partsMap.get("left_arm")));
        }
        if (animationStationaryLegs) {
            resetLegAngle("rightLeg", "right_leg");
            resetLegAngle("leftLeg", "left_leg");
        }
    }

    private void setArmAngle(ModelPart part) {
        if (part == null) return;
        part.xRot = -1.5707964f;
        part.yRot = 0f;
        part.zRot = 0f;
    }

    private void resetLegAngle(String key1, String key2) {
        ModelPart leg = partsMap.getOrDefault(key1, partsMap.get(key2));
        PartTransform def = defaultTransforms.getOrDefault(key1, defaultTransforms.get(key2));
        if (leg == null || def == null) return;
        leg.xRot = def.pitch;
        leg.yRot = def.yaw;
        leg.zRot = def.roll;
    }

    public void copyFromVanilla(PlayerModel vanillaModel) {
        // copyRot
        for (var pair : Arrays.asList(new Object[][]{
                {"head", vanillaModel.head},
                {"body", vanillaModel.body},
                {"hat", vanillaModel.hat}
        })) {
            String name = (String) pair[0];
            ModelPart part = (ModelPart) pair[1];
            ModelPart dest = partsMap.get(name);
            if (dest == null) dest = partsMap.get(mapBoneName(name));
            if (dest != null) {
                dest.xRot = part.xRot;
                dest.yRot = part.yRot;
                dest.zRot = part.zRot;
            }
        }

        if (!animationArmsOutFront) {
            ModelPart dest = partsMap.get("rightArm"); if (dest != null) { dest.xRot = vanillaModel.rightArm.xRot; dest.yRot = vanillaModel.rightArm.yRot; dest.zRot = vanillaModel.rightArm.zRot; }
            dest = partsMap.get("leftArm"); if (dest != null) { dest.xRot = vanillaModel.leftArm.xRot; dest.yRot = vanillaModel.leftArm.yRot; dest.zRot = vanillaModel.leftArm.zRot; }
        }
        if (!animationStationaryLegs) {
            ModelPart dest = partsMap.get("rightLeg"); if (dest != null) { dest.xRot = vanillaModel.rightLeg.xRot; dest.yRot = vanillaModel.rightLeg.yRot; dest.zRot = vanillaModel.rightLeg.zRot; }
            dest = partsMap.get("leftLeg"); if (dest != null) { dest.xRot = vanillaModel.leftLeg.xRot; dest.yRot = vanillaModel.leftLeg.yRot; dest.zRot = vanillaModel.leftLeg.zRot; }
        }

        // getPivotY via the ModelPart initial pose
        java.util.function.Function<ModelPart, Float> getPivotY = part -> {
            if (part == null) return 0f;
            try {
                PartPose init = part.getInitialPose();
                return init != null ? init.y() : 0f;
            } catch (Exception e) {
                return 0f;
            }
        };

        try {
            PartTransform bodyTransform = defaultTransforms.getOrDefault("body", defaultTransforms.get("BODY"));
            float bedrockBodyY = bodyTransform != null ? bodyTransform.y : 0f;
            float vanillaBodyPivotY = getPivotY.apply(vanillaModel.body);

            float bedrockHeadY = defaultTransforms.getOrDefault("head", bodyTransform) != null ? defaultTransforms.getOrDefault("head", bodyTransform).y : bedrockBodyY;
            float vanillaHeadPivotY = getPivotY.apply(vanillaModel.head);

            upperArmorYOffset = ((bedrockBodyY + bedrockHeadY) * 0.5f) - ((vanillaBodyPivotY + vanillaHeadPivotY) * 0.5f);
            armorYOffset = upperArmorYOffset;

            PartTransform capeTransform = defaultTransforms.get("cape");
            float bedrockCapeY = capeTransform != null ? capeTransform.y : bedrockBodyY;
            capeYOffset = bedrockCapeY - vanillaBodyPivotY;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
