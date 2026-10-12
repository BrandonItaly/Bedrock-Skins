package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.render.model.ResourcePackAnimationPose;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.player.PlayerCapeModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.animation.state.EMFBipedPose", remap = false)
public abstract class EmfBipedPoseMixin {
    @Inject(target = @Desc(value = "applyTo", args = HumanoidModel.class),
        at = @At("HEAD"), cancellable = true, require = 0)
    private void bedrockskins$preservePlayerPose(HumanoidModel<?> model, CallbackInfo ci) {
        // These models prepare their own pose. Cached absolute poses lose Bedrock
        // pivots or carry an emote that would be applied a second time.
        if (model instanceof BedrockPlayerModel || ResourcePackAnimationPose.isAnimatingSource(model)
            || (model instanceof PlayerCapeModel && ResourcePackAnimationPose.hasAnimation(model))
            || (model instanceof PlayerModel && EmoteManager.hasEmotePose(model))) ci.cancel();
    }
}
