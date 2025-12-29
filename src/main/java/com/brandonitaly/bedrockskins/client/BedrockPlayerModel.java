package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.bedrock.BedrockBone;
import com.brandonitaly.bedrockskins.bedrock.BedrockGeometry;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

import java.util.*;

public class BedrockPlayerModel extends PlayerEntityModel {
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
        ModelData modelData = new ModelData();
        ModelPartData rootData = modelData.getRoot();

        Map<String, BedrockBone> boneMap = new HashMap<>();
        if (geometry.getBones() != null) for (BedrockBone b : geometry.getBones()) boneMap.put(b.getName(), b);
        Set<String> processedBones = new HashSet<>();

        if (geometry.getBones() != null) {
            for (BedrockBone b : geometry.getBones()) {
                addBoneRecursively(b.getName(), boneMap, processedBones);
            }
        }

        Map<String, ModelPartData> partDataMap = new HashMap<>();
        Set<String> vanillaRootParts = new HashSet<>(Arrays.asList(
                EntityModelPartNames.HEAD,
                EntityModelPartNames.BODY,
                EntityModelPartNames.RIGHT_ARM,
                EntityModelPartNames.LEFT_ARM,
                EntityModelPartNames.RIGHT_LEG,
                EntityModelPartNames.LEFT_LEG
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
                    ModelPartData parentData = bone.getParent() == null ? rootData : partDataMap.get(bone.getParent());
                    ModelPartBuilder builder = ModelPartBuilder.create();

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
                            builder.mirrored(isMirrored).uv(u, v).cuboid(offX, offY, offZ, cube.getSize().get(0), cube.getSize().get(1), cube.getSize().get(2), new Dilation(dilation));
                        }
                    }

                    float bPx = bone.getPivot() != null && bone.getPivot().size() > 0 ? bone.getPivot().get(0) : 0f;
                    float bPy = bone.getPivot() != null && bone.getPivot().size() > 1 ? bone.getPivot().get(1) : 0f;
                    float bPz = bone.getPivot() != null && bone.getPivot().size() > 2 ? bone.getPivot().get(2) : 0f;
                    float pX = bPx;
                    float pY = 24f - bPy;
                    float pZ = bPz;
                    String vanillaName = mapBoneName(bone.getName());
                    ModelPartData parentForCreation = (bone.getParent() != null && vanillaRootParts.contains(vanillaName)) ? rootData : parentData;
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
                    ModelTransform transform = ModelTransform.of(pX, pY, pZ, rotX, rotY, rotZ);
                    ModelPartData partData = parentForCreation.addChild(vanillaName, builder, transform);
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

        TexturedModelData texturedModelData = TexturedModelData.of(modelData, geometry.getDescription().getTextureWidth(), geometry.getDescription().getTextureHeight());
        ModelPart rootPart = texturedModelData.createModel();

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
            case "head": return EntityModelPartNames.HEAD;
            case "hat":
            case "headwear": return EntityModelPartNames.HAT;
            case "body": return EntityModelPartNames.BODY;
            case "jacket": return EntityModelPartNames.JACKET;
            case "rightarm":
            case "right_arm": return EntityModelPartNames.RIGHT_ARM;
            case "leftarm":
            case "left_arm": return EntityModelPartNames.LEFT_ARM;
            case "rightleg":
            case "right_leg": return EntityModelPartNames.RIGHT_LEG;
            case "leftleg":
            case "left_leg": return EntityModelPartNames.LEFT_LEG;
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
    public void setAngles(PlayerEntityRenderState state) {
        super.setAngles(state);
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
        part.pitch = -1.5707964f;
        part.yaw = 0f;
        part.roll = 0f;
    }

    private void resetLegAngle(String key1, String key2) {
        ModelPart leg = partsMap.getOrDefault(key1, partsMap.get(key2));
        PartTransform def = defaultTransforms.getOrDefault(key1, defaultTransforms.get(key2));
        if (leg == null || def == null) return;
        leg.pitch = def.pitch;
        leg.yaw = def.yaw;
        leg.roll = def.roll;
    }

    public void copyFromVanilla(PlayerEntityModel vanillaModel) {
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
                dest.pitch = part.pitch;
                dest.yaw = part.yaw;
                dest.roll = part.roll;
            }
        }

        if (!animationArmsOutFront) {
            ModelPart dest = partsMap.get("rightArm"); if (dest != null) { dest.pitch = vanillaModel.rightArm.pitch; dest.yaw = vanillaModel.rightArm.yaw; dest.roll = vanillaModel.rightArm.roll; }
            dest = partsMap.get("leftArm"); if (dest != null) { dest.pitch = vanillaModel.leftArm.pitch; dest.yaw = vanillaModel.leftArm.yaw; dest.roll = vanillaModel.leftArm.roll; }
        }
        if (!animationStationaryLegs) {
            ModelPart dest = partsMap.get("rightLeg"); if (dest != null) { dest.pitch = vanillaModel.rightLeg.pitch; dest.yaw = vanillaModel.rightLeg.yaw; dest.roll = vanillaModel.rightLeg.roll; }
            dest = partsMap.get("leftLeg"); if (dest != null) { dest.pitch = vanillaModel.leftLeg.pitch; dest.yaw = vanillaModel.leftLeg.yaw; dest.roll = vanillaModel.leftLeg.roll; }
        }

        // getPivotY reflectively
        java.util.function.Function<ModelPart, Float> getPivotY = part -> {
            try {
                var field = part.getClass().getDeclaredField("pivotY");
                field.setAccessible(true);
                Object f = field.get(part);
                if (f instanceof Number) return ((Number)f).floatValue();
                return 0f;
            } catch (Exception ex) {
                try {
                    var field = net.minecraft.client.model.ModelPart.class.getDeclaredField("pivotY");
                    field.setAccessible(true);
                    return field.getFloat(part);
                } catch (Exception e) {
                    return 0f;
                }
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
