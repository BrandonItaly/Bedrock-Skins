package io.github.brandonitaly.bedrockskins.client.render.model;

import io.github.brandonitaly.bedrockskins.bedrock.BedrockBone;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel.BedrockAnimFlags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Groups authored armor parts and applies skin-specific armor fitting and visibility. */
final class BedrockArmorParts {
    private final Map<EquipmentSlot, List<ModelPart>> parts = new EnumMap<>(EquipmentSlot.class);

    BedrockArmorParts(Map<String, ModelPart> modelParts) {
        modelParts.forEach((name, part) -> {
            EquipmentSlot slot = slotFor(name.toLowerCase(Locale.ROOT));
            if (slot != null) parts.computeIfAbsent(slot, ignored -> new ArrayList<>()).add(part);
        });
    }

    private static EquipmentSlot slotFor(String name) {
        if (name.endsWith("helmet")) return EquipmentSlot.HEAD;
        if (name.equals("rightarmarmor") || name.equals("leftarmarmor") || name.endsWith("bodyarmor")) {
            return EquipmentSlot.CHEST;
        }
        if (name.equals("rightlegarmor") || name.equals("leftlegarmor") || name.endsWith("leggings")) {
            return EquipmentSlot.LEGS;
        }
        if (name.equals("rightbootarmor") || name.equals("leftbootarmor") || name.endsWith("boots")) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    void setVisible(EquipmentSlot slot, boolean visible) {
        for (ModelPart part : parts.getOrDefault(slot, List.of())) part.visible = visible;
    }

    static void fit(BedrockPlayerModel model, List<BedrockBone> bones) {
        for (BedrockBone bone : bones) {
            switch (BedrockPlayerModel.mapBoneName(bone.getName())) {
                case PartNames.HEAD -> model.headArmorFit = ArmorPartFit.from(bone, -8, 8);
                case PartNames.BODY -> model.bodyArmorFit = ArmorPartFit.from(bone, 0, 12);
                case PartNames.RIGHT_ARM -> model.rightArmArmorFit = ArmorPartFit.from(bone, -2, 12);
                case PartNames.LEFT_ARM -> model.leftArmArmorFit = ArmorPartFit.from(bone, -2, 12);
                case PartNames.RIGHT_LEG -> model.rightLegArmorFit = ArmorPartFit.from(bone, 0, 12);
                case PartNames.LEFT_LEG -> model.leftLegArmorFit = ArmorPartFit.from(bone, 0, 12);
            }
        }
    }

    static boolean applyVisibility(HumanoidModel<?> armor, EquipmentSlot slot, BedrockAnimFlags flags) {
        if (armor == null || slot == null) return true;
        if (flags.dontShowArmor()) return false;
        return switch (slot) {
            case HEAD -> {
                boolean show = !flags.headDisabled() || flags.forceHeadArmor();
                armor.head.visible = armor.hat.visible = show;
                yield show;
            }
            case CHEST -> {
                armor.body.visible = !flags.bodyDisabled() || flags.forceBodyArmor();
                armor.rightArm.visible = !flags.rightArmDisabled() || flags.forceRightArmArmor();
                armor.leftArm.visible = !flags.leftArmDisabled() || flags.forceLeftArmArmor();
                yield armor.body.visible || armor.rightArm.visible || armor.leftArm.visible;
            }
            case LEGS, FEET -> {
                armor.rightLeg.visible = !flags.rightLegDisabled() || flags.forceRightLegArmor();
                armor.leftLeg.visible = !flags.leftLegDisabled() || flags.forceLeftLegArmor();
                yield armor.rightLeg.visible || armor.leftLeg.visible;
            }
            default -> true;
        };
    }
}
