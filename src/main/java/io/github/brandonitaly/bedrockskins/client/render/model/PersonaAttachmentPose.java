package io.github.brandonitaly.bedrockskins.client.render.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** Moves cosmetic anchors by the base skin's departure from the standard skeleton. */
public final class PersonaAttachmentPose {
    private PersonaAttachmentPose() {}

    public static void apply(HumanoidModel<?> cosmetic, HumanoidModel<?> base, HumanoidModel<?> standard) {
        offset(cosmetic.head, base.head, standard.head);
        offset(cosmetic.body, base.body, standard.body);
        offset(cosmetic.rightArm, base.rightArm, standard.rightArm);
        offset(cosmetic.leftArm, base.leftArm, standard.leftArm);
        offset(cosmetic.rightLeg, base.rightLeg, standard.rightLeg);
        offset(cosmetic.leftLeg, base.leftLeg, standard.leftLeg);
    }

    private static void offset(ModelPart cosmetic, ModelPart base, ModelPart standard) {
        var target = base.getInitialPose();
        var reference = standard.getInitialPose();
        cosmetic.x += target.x() - reference.x();
        cosmetic.y += target.y() - reference.y();
        cosmetic.z += target.z() - reference.z();
    }
}
