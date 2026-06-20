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
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.*;

public class BedrockPlayerModel extends PlayerModel {
    private static final float NINETY_DEGREES = 1.5707964f;
    private static final float ITEM_POSE_ROT = 0.31415927f;
    private static final float WALK_SWING_CONST = 0.6662f;

    public final Map<String, ModelPart> partsMap;
    public final Map<String, PartTransform> defaultTransforms;
    public final float heightMultiplier;
    public final BedrockAnimFlags animFlags;

    // Pre-resolved parts for zero-allocation rendering
    public final ModelPart customHead, customHat, customBody;
    public final ModelPart customRightArm, customLeftArm;
    public final ModelPart customRightLeg, customLeftLeg;

    private final List<ModelPart> helmetParts = new ArrayList<>();
    private final List<ModelPart> chestplateParts = new ArrayList<>();
    private final List<ModelPart> leggingsParts = new ArrayList<>();
    private final List<ModelPart> bootsParts = new ArrayList<>();

    public BedrockPlayerModel(ModelPart root, boolean thinArms, Map<String, ModelPart> partsMap, Map<String, PartTransform> defaultTransforms, float heightMultiplier, BedrockAnimFlags animFlags) {
        super(root, thinArms);
        this.partsMap = Map.copyOf(partsMap);
        this.defaultTransforms = Map.copyOf(defaultTransforms);
        this.heightMultiplier = heightMultiplier;
        this.animFlags = animFlags;

        this.customHead = resolvePart("head", PartNames.HEAD);
        this.customHat = resolvePart("hat", PartNames.HAT);
        this.customBody = resolvePart("body", PartNames.BODY);
        this.customRightArm = resolvePart("rightArm", PartNames.RIGHT_ARM);
        this.customLeftArm = resolvePart("leftArm", PartNames.LEFT_ARM);
        this.customRightLeg = resolvePart("rightLeg", PartNames.RIGHT_LEG);
        this.customLeftLeg = resolvePart("leftLeg", PartNames.LEFT_LEG);

        for (Map.Entry<String, ModelPart> entry : partsMap.entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            ModelPart part = entry.getValue();
            if (name.endsWith("helmet")) {
                helmetParts.add(part);
            } else if (name.equals("rightarmarmor") || name.equals("leftarmarmor") || name.endsWith("bodyarmor")) {
                chestplateParts.add(part);
            } else if (name.equals("rightlegarmor") || name.equals("leftlegarmor") || name.endsWith("leggings")) {
                leggingsParts.add(part);
            } else if (name.equals("rightbootarmor") || name.equals("leftbootarmor") || name.endsWith("boots")) {
                bootsParts.add(part);
            }
        }
    }

    public record PartTransform(float x, float y, float z, float pitch, float yaw, float roll) {}

    public static class BedrockAnimFlags {
        private final BedrockGeometry geometry;

        public BedrockAnimFlags(BedrockGeometry geometry) {
            this.geometry = geometry;
        }

        public static BedrockAnimFlags fromGeometry(BedrockGeometry g) {
            return new BedrockAnimFlags(g);
        }

        public boolean armsOutFront() { return isTrue(geometry.getAnimationArmsOutFront()); }
        public boolean singleArm() { return isTrue(geometry.getAnimationSingleArmAnimation()); }
        public boolean stationaryLegs() { return isTrue(geometry.getAnimationStationaryLegs()); }
        public boolean singleLeg() { return isTrue(geometry.getAnimationSingleLegAnimation()); }
        public boolean dontShowArmor() { return isTrue(geometry.getAnimationDontShowArmor()); }
        public boolean headDisabled() { return isTrue(geometry.getAnimationHeadDisabled()); }
        public boolean bodyDisabled() { return isTrue(geometry.getAnimationBodyDisabled()); }
        public boolean rightArmDisabled() { return isTrue(geometry.getAnimationRightArmDisabled()); }
        public boolean leftArmDisabled() { return isTrue(geometry.getAnimationLeftArmDisabled()); }
        public boolean rightLegDisabled() { return isTrue(geometry.getAnimationRightLegDisabled()); }
        public boolean leftLegDisabled() { return isTrue(geometry.getAnimationLeftLegDisabled()); }
        public boolean forceHeadArmor() { return isTrue(geometry.getAnimationForceHeadArmor()); }
        public boolean forceBodyArmor() { return isTrue(geometry.getAnimationForceBodyArmor()); }
        public boolean forceRightArmArmor() { return isTrue(geometry.getAnimationForceRightArmArmor()); }
        public boolean forceLeftArmArmor() { return isTrue(geometry.getAnimationForceLeftArmArmor()); }
        public boolean forceRightLegArmor() { return isTrue(geometry.getAnimationForceRightLegArmor()); }
        public boolean forceLeftLegArmor() { return isTrue(geometry.getAnimationForceLeftLegArmor()); }

        private static boolean isTrue(Boolean b) { return Boolean.TRUE.equals(b); }
    }

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
        splitArmorBones(normalized);
        ensureRequiredBones(normalized);
        BuildRootResult result = buildRoot(normalized);
        
        float headPivotY = (float) normalized.getBones().stream()
            .filter(b -> b.getName() != null && "head".equalsIgnoreCase(mapBoneName(b.getName())))
            .map(BedrockBone::getPivot)
            .filter(p -> p != null && p.size() >= 2)
            .mapToDouble(p -> p.get(1))
            .findFirst()
            .orElse(24.0);
        
        float heightMultiplier = Math.max(headPivotY / 24.0f, 0.001f);
        BedrockAnimFlags flags = BedrockAnimFlags.fromGeometry(normalized);

        BedrockPlayerModel model = new BedrockPlayerModel(result.root(), thinArms, result.parts(), result.defaults(), heightMultiplier, flags);

        return model;
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
        Set<String> existing = new HashSet<>();
        for (BedrockBone bone : geometry.getBones()) {
            if (bone.getName() != null) existing.add(bone.getName().toLowerCase(Locale.ROOT));
        }

        for (String[] req : REQUIRED_BONES) {
            String name = req[0];
            if (!existing.contains(name.toLowerCase(Locale.ROOT)) && !existing.contains(mapBoneName(name).toLowerCase(Locale.ROOT))) {
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
                 (float) Math.toRadians(getListValue(bone.getRotation(), 0)), 
                 (float) Math.toRadians(getListValue(bone.getRotation(), 1)), 
                 (float) Math.toRadians(getListValue(bone.getRotation(), 2)),
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

        // Recursively construct hierarchy ensuring parents build before children
        for (Map.Entry<String, BoneNode> entry : nodes.entrySet()) {
            getOrCreatePartDef(entry.getKey(), entry.getValue(), nodes, rootData, partDefs, defaultTransforms);
        }

        LayerDefinition layer = LayerDefinition.create(mesh, geometry.getDescription().getTextureWidth(), geometry.getDescription().getTextureHeight());
        ModelPart rootPart = layer.bakeRoot();

        Map<String, ModelPart> finalParts = new HashMap<>();
        for (String boneName : nodes.keySet()) {
            ModelPart part = resolveHierarchyPart(rootPart, boneName, nodes);
            if (part != null) {
                finalParts.put(boneName, part);
                String alias = mapBoneName(boneName);
                if (!Objects.equals(alias, boneName)) finalParts.put(alias, part);
            }
        }

        return new BuildRootResult(rootPart, finalParts, defaultTransforms);
    }

    private static PartDefinition getOrCreatePartDef(String name, BoneNode node, Map<String, BoneNode> nodes,
            PartDefinition rootData, Map<String, PartDefinition> partDefs, Map<String, PartTransform> defaultTransforms) {
        
        if (partDefs.containsKey(name)) return partDefs.get(name);

        String mappedName = mapBoneName(name);
        boolean forceRoot = VANILLA_ROOT_PARTS.contains(mappedName);
        PartDefinition parentDef = rootData;

        if (node.parent() != null && !forceRoot) {
            BoneNode parentNode = nodes.get(node.parent());
            if (parentNode != null) {
                parentDef = getOrCreatePartDef(node.parent(), parentNode, nodes, rootData, partDefs, defaultTransforms);
            }
        }

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

        PartDefinition partDef = parentDef.addOrReplaceChild(mappedName, buildCubeList(node), 
                PartPose.offsetAndRotation(localX, localY, localZ, node.rotX(), node.rotY(), node.rotZ()));
        
        partDefs.put(name, partDef);
        PartTransform transform = new PartTransform(localX, localY, localZ, node.rotX(), node.rotY(), node.rotZ());
        defaultTransforms.put(name, transform);
        if (!Objects.equals(mappedName, name)) defaultTransforms.put(mappedName, transform);

        return partDef;
    }

    private static void addVanillaScaffold(PartDefinition root) {
        root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create(), PartPose.ZERO).addOrReplaceChild(PartNames.HAT, CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create(), PartPose.ZERO).addOrReplaceChild(PartNames.JACKET, CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create(), PartPose.ZERO).addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create(), PartPose.ZERO).addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create(), PartPose.ZERO).addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create(), PartPose.ZERO).addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);
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

    private static ModelPart resolveHierarchyPart(ModelPart root, String boneName, Map<String, BoneNode> nodes) {
        String mapped = mapBoneName(boneName);
        if (root.hasChild(mapped)) return root.getChild(mapped);
        
        BoneNode node = nodes.get(boneName);
        if (node == null) return null;
        
        if (node.parent() != null) {
            ModelPart parentPart = resolveHierarchyPart(root, node.parent(), nodes);
            if (parentPart != null && parentPart.hasChild(mapped)) {
                return parentPart.getChild(mapped);
            }
        }
        return null;
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

        // Propagate to armor sub-bones
        String lower = partName.toLowerCase(Locale.ROOT);
        if ("bodyarmor".equals(lower)) {
            setChestplateVisible(visible);
        } else if ("helmet".equals(lower)) {
            setHelmetVisible(visible);
        } else if ("leggings".equals(lower)) {
            setLeggingsVisible(visible);
        } else if ("boots".equals(lower)) {
            setBootsVisible(visible);
        }
    }

    private static void setPartsVisible(List<ModelPart> parts, boolean visible) {
        for (ModelPart part : parts) part.visible = visible;
    }

    public void setHelmetVisible(boolean visible) { setPartsVisible(helmetParts, visible); }
    public void setChestplateVisible(boolean visible) { setPartsVisible(chestplateParts, visible); }
    public void setLeggingsVisible(boolean visible) { setPartsVisible(leggingsParts, visible); }
    public void setBootsVisible(boolean visible) { setPartsVisible(bootsParts, visible); }

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

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        if (!BedrockSkinsConfig.isSkinAnimationsEnabled() || state.isPassenger) return;

        // --- Arms Additive Animation ---
        if (animFlags.armsOutFront() && !state.isVisuallySwimming) {
            applyArmsOutFrontToArm(customRightArm, true, state);
            applyArmsOutFrontToArm(customLeftArm, false, state);
            if (customRightArm != null) customRightArm.xRot -= NINETY_DEGREES;
            if (customLeftArm != null) customLeftArm.xRot -= NINETY_DEGREES;
            
        } else if (animFlags.singleArm() && !state.isVisuallySwimming) {
            if (customLeftArm != null && customRightArm != null) {
                customLeftArm.xRot += computeArmWalkSwing(state, true) - computeArmWalkSwing(state, false);
            }
        }
        
        // --- Legs Additive Animation ---
        if (animFlags.stationaryLegs()) {
            if (customRightLeg != null) customRightLeg.xRot -= computeLegWalkSwing(state, true);
            if (customLeftLeg != null) customLeftLeg.xRot -= computeLegWalkSwing(state, false);
        } else if (animFlags.singleLeg()) {
            if (customRightLeg != null && customLeftLeg != null) customLeftLeg.xRot = customRightLeg.xRot;
        }
    }

    private void applyArmsOutFrontToArm(ModelPart arm, boolean rightArm, AvatarRenderState state) {
        if (arm == null) return;
        HumanoidModel.ArmPose pose = rightArm ? state.rightArmPose : state.leftArmPose;

        if (pose != ArmPose.EMPTY && pose != ArmPose.ITEM) return;
        
        if (pose == HumanoidModel.ArmPose.ITEM && state.attackTime <= 0.0F) {
            arm.xRot = (arm.xRot + ITEM_POSE_ROT) * 2.0F;
            arm.yRot = 0.0F;
            arm.zRot = 0.0F;
        }
        arm.xRot -= computeArmWalkSwing(state, rightArm);
    }

    private float computeArmWalkSwing(AvatarRenderState state, boolean rightArm) {
        float speedValue = state.speedValue != 0f ? state.speedValue : 1f;
        float phase = rightArm ? Mth.PI : 0f;
        return Mth.cos(state.walkAnimationPos * WALK_SWING_CONST + phase) * (state.walkAnimationSpeed / speedValue);
    }

    private float computeLegWalkSwing(AvatarRenderState state, boolean rightLeg) {
        float phase = rightLeg ? 0f : Mth.PI;
        return Mth.cos(state.walkAnimationPos * WALK_SWING_CONST + phase) * 1.4F * state.walkAnimationSpeed;
    }

    public void copyFromVanilla(PlayerModel vanillaModel) {
        copyRotation(customHead, vanillaModel.head);
        copyRotation(customBody, vanillaModel.body);
        copyRotation(customHat, vanillaModel.hat);

        boolean animsEnabled = BedrockSkinsConfig.isSkinAnimationsEnabled();

        if (!animFlags.armsOutFront() || !animsEnabled) {
            copyRotation(customRightArm, vanillaModel.rightArm);
            copyRotation(customLeftArm, vanillaModel.leftArm);
        }
        if (!animFlags.stationaryLegs() || !animsEnabled) {
            copyRotation(customRightLeg, vanillaModel.rightLeg);
            copyRotation(customLeftLeg, vanillaModel.leftLeg);
        }
    }

    public boolean shouldHideArmor() { return animFlags.dontShowArmor(); }

    public boolean applyArmorVisibility(HumanoidModel<?> armorModel, EquipmentSlot slot) {
        if (armorModel == null || slot == null) return true;

        // Global disable flag
        if (animFlags.dontShowArmor()) return false;

        return switch (slot) {
            case HEAD -> {
                // Show if the part is NOT disabled, OR if the force override is true
                boolean show = !animFlags.headDisabled() || animFlags.forceHeadArmor();
                armorModel.head.visible = show;
                armorModel.hat.visible = show;
                yield show;
            }
            case CHEST -> {
                boolean showBody = !animFlags.bodyDisabled() || animFlags.forceBodyArmor();
                boolean showRArm = !animFlags.rightArmDisabled() || animFlags.forceRightArmArmor();
                boolean showLArm = !animFlags.leftArmDisabled() || animFlags.forceLeftArmArmor();
                
                armorModel.body.visible = showBody;
                armorModel.rightArm.visible = showRArm;
                armorModel.leftArm.visible = showLArm;
                yield showBody || showRArm || showLArm;
            }
            case LEGS, FEET -> {
                boolean showRLeg = !animFlags.rightLegDisabled() || animFlags.forceRightLegArmor();
                boolean showLLeg = !animFlags.leftLegDisabled() || animFlags.forceLeftLegArmor();
                
                armorModel.rightLeg.visible = showRLeg;
                armorModel.leftLeg.visible = showLLeg;
                yield showRLeg || showLLeg;
            }
            default -> true;
        };
    }

    public boolean isStationaryLegs() { 
        return animFlags.stationaryLegs() && BedrockSkinsConfig.isSkinAnimationsEnabled(); 
    }

    private void copyRotation(ModelPart dest, ModelPart source) {
        if (source != null && dest != null) {
            dest.xRot = source.xRot;
            dest.yRot = source.yRot;
            dest.zRot = source.zRot;
        }
    }

    private ModelPart resolvePart(String name) {
        ModelPart part = partsMap.get(name);
        return part != null ? part : partsMap.get(mapBoneName(name));
    }

    private ModelPart resolvePart(String primary, String fallback) {
        ModelPart part = resolvePart(primary);
        return part != null ? part : resolvePart(fallback);
    }
}