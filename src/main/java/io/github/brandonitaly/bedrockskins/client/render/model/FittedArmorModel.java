package io.github.brandonitaly.bedrockskins.client.render.model;

import io.github.brandonitaly.bedrockskins.client.appearance.AppearanceResolver;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Used only by the armor layer; fits after the queued model's animation is prepared. */
public final class FittedArmorModel<S extends HumanoidRenderState> extends Model<S> {
    private final HumanoidModel<S> armor;

    public FittedArmorModel(HumanoidModel<S> armor) {
        super(armor.root(), armor.renderType());
        this.armor = armor;
    }

    @Override
    public void setupAnim(S state) {
        armor.setupAnim(state);
        var skinId = AppearanceResolver.skinId(state);
        if (skinId == null) return;
        var skin = BedrockModelManager.getModel(skinId);
        if (skin == null) return;
        if (!skin.usesCustomAnimations()
            && state instanceof AvatarRenderState avatarState) {
            var pose = BedrockRenderStateStore.animationPose(avatarState);
            if (pose != null) {
                EmoteManager.restorePoseDeltas(armor);
                pose.apply(armor, skin);
                EmoteManager.apply(armor, BedrockRenderStateStore.getUniqueId(state));
            }
        }
        skin.headArmorFit.applyPosition(armor.head);
        skin.bodyArmorFit.apply(armor.body);
        skin.rightArmArmorFit.apply(armor.rightArm);
        skin.leftArmArmorFit.apply(armor.leftArm);
        skin.rightLegArmorFit.apply(armor.rightLeg);
        skin.leftLegArmorFit.apply(armor.leftLeg);
    }
}
