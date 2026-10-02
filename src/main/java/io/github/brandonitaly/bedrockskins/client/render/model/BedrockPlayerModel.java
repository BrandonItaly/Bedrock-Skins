package io.github.brandonitaly.bedrockskins.client.render.model;

import io.github.brandonitaly.bedrockskins.client.persistence.BedrockSkinsConfig;

import io.github.brandonitaly.bedrockskins.bedrock.BedrockGeometry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Map;

public class BedrockPlayerModel extends PlayerModel {

    public final Map<String, ModelPart> partsMap;
    public final float heightMultiplier;
    public final BedrockAnimFlags animFlags;
    public final boolean personaCosmetic;
    public ArmorPartFit headArmorFit = ArmorPartFit.STANDARD;
    public ArmorPartFit bodyArmorFit = ArmorPartFit.STANDARD;
    public ArmorPartFit rightArmArmorFit = ArmorPartFit.STANDARD;
    public ArmorPartFit leftArmArmorFit = ArmorPartFit.STANDARD;
    public ArmorPartFit rightLegArmorFit = ArmorPartFit.STANDARD;
    public ArmorPartFit leftLegArmorFit = ArmorPartFit.STANDARD;

    // Pre-resolved parts for zero-allocation rendering
    public final ModelPart customHead, customHat, customBody;
    public final ModelPart customRightArm, customLeftArm;
    public final ModelPart customRightLeg, customLeftLeg;

    private final BedrockArmorParts armorParts;

    public BedrockPlayerModel(ModelPart root, boolean thinArms, Map<String, ModelPart> partsMap,
                              float heightMultiplier,
                              BedrockAnimFlags animFlags, boolean personaCosmetic) {
        super(root, thinArms);
        this.partsMap = Map.copyOf(partsMap);
        this.heightMultiplier = heightMultiplier;
        this.animFlags = animFlags;
        this.personaCosmetic = personaCosmetic;

        this.customHead = resolvePart("head");
        this.customHat = resolvePart("hat");
        this.customBody = resolvePart("body");
        this.customRightArm = resolvePart("rightArm");
        this.customLeftArm = resolvePart("leftArm");
        this.customRightLeg = resolvePart("rightLeg");
        this.customLeftLeg = resolvePart("leftLeg");

        this.armorParts = new BedrockArmorParts(partsMap);
    }

    public static class BedrockAnimFlags {
        private final BedrockGeometry geometry;

        public BedrockAnimFlags(BedrockGeometry geometry) {
            this.geometry = geometry;
        }

        public static BedrockAnimFlags fromGeometry(BedrockGeometry g) {
            return new BedrockAnimFlags(g);
        }

        public boolean legacy() { return geometry.isLegacyAnimation(); }
        public boolean invertedCrouch() { return isTrue(geometry.getAnimationInvertedCrouch()); }
        public boolean armsDown() { return isTrue(geometry.getAnimationArmsDown()); }
        public boolean statueArms() { return isTrue(geometry.getAnimationStatueOfLibertyArms()); }
        public boolean overridesArms() { return armsDown() || armsOutFront() || singleArm() || statueArms(); }
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

    public static BedrockPlayerModel create(BedrockGeometry geometry, boolean thinArms) {
        return create(geometry, thinArms, false);
    }

    public static BedrockPlayerModel create(BedrockGeometry geometry, boolean thinArms, boolean personaCosmetic) {
        var built = BedrockModelBuilder.build(geometry);
        BedrockGeometryBaker.apply(geometry.getBones(), built.parts(),
            geometry.getDescription().getTextureWidth(), geometry.getDescription().getTextureHeight());
        BedrockPlayerModel model = new BedrockPlayerModel(built.root(), thinArms, built.parts(),
            built.heightMultiplier(), BedrockAnimFlags.fromGeometry(geometry), personaCosmetic);
        BedrockArmorParts.fit(model, geometry.getBones());
        return model;
    }

    public static String mapBoneName(String name) {
        return BedrockModelBuilder.mapBoneName(name);
    }

    public void setHelmetVisible(boolean visible) { armorParts.setVisible(EquipmentSlot.HEAD, visible); }
    public void setChestplateVisible(boolean visible) { armorParts.setVisible(EquipmentSlot.CHEST, visible); }
    public void setLeggingsVisible(boolean visible) { armorParts.setVisible(EquipmentSlot.LEGS, visible); }
    public void setBootsVisible(boolean visible) { armorParts.setVisible(EquipmentSlot.FEET, visible); }

    /** Invoked during vanilla setup, after walking but before riding, items and attacks. */
    public void applySkinBasePose(net.minecraft.client.renderer.entity.state.HumanoidRenderState state) {
        if (!BedrockSkinsConfig.isSkinAnimationsEnabled()) return;
        SkinAnimationPose.apply(this, animFlags, state);
    }

    public void copyFromVanilla(PlayerModel vanillaModel) {
        if ((animFlags.legacy() || animFlags.invertedCrouch()) && BedrockSkinsConfig.isSkinAnimationsEnabled()) return;
        copyRotation(customHead, vanillaModel.head);
        copyRotation(customBody, vanillaModel.body);
        copyRotation(customHat, vanillaModel.hat);

        boolean animsEnabled = BedrockSkinsConfig.isSkinAnimationsEnabled();

        if (!animFlags.overridesArms() || !animsEnabled) {
            copyRotation(customRightArm, vanillaModel.rightArm);
            copyRotation(customLeftArm, vanillaModel.leftArm);
        }
        if (!(animFlags.stationaryLegs() || animFlags.singleLeg()) || !animsEnabled) {
            copyRotation(customRightLeg, vanillaModel.rightLeg);
            copyRotation(customLeftLeg, vanillaModel.leftLeg);
        }
    }

    public boolean shouldHideArmor() { return animFlags.dontShowArmor(); }

    public boolean applyArmorVisibility(HumanoidModel<?> armorModel, EquipmentSlot slot) {
        return BedrockArmorParts.applyVisibility(armorModel, slot, animFlags);
    }

    public boolean isStationaryLegs() {
        return animFlags.stationaryLegs() && BedrockSkinsConfig.isSkinAnimationsEnabled();
    }

    private static void copyRotation(ModelPart dest, ModelPart source) {
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
}
