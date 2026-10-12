package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.appearance.AppearanceResolver;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.render.model.ResourcePackAnimatedModel;
import io.github.brandonitaly.bedrockskins.client.render.model.ResourcePackAnimationPose;
import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Run after EMF's player-model callback, which may copy a cached pose.
@Mixin(value = PlayerModel.class, priority = 900)
public abstract class PlayerModelAnimationMixin {
    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void bedrockskins$finishPlayerPose(AvatarRenderState state, CallbackInfo ci) {
        var model = (PlayerModel) (Object) this;
        if (model instanceof BedrockPlayerModel || ResourcePackAnimationPose.isAnimatingSource(model)) return;
        if (model instanceof PlayerCapeModel) {
            if (ResourcePackAnimationPose.hasAnimation(model)) return;
            var base = AppearanceResolver.baseModel(state);
            if (base != null && base.usesCustomAnimations()) return;
            var pose = BedrockRenderStateStore.animationPose(state);
            if (pose == null) return;
            EmoteManager.restorePoseDeltas(model);
            pose.apply(model, base != null ? base : model);
        } else {
            if (!((Object) model.root() instanceof ResourcePackAnimatedModel animated)
                || !animated.hasAnimation()) return;
            EmoteManager.restorePoseDeltas(model);
            // Animate now so the emote is last.
            animated.animate();
            BedrockRenderStateStore.setAnimationPose(state, ResourcePackAnimationPose.capture(model));
        }
        EmoteManager.apply(model, BedrockRenderStateStore.getUniqueId(state));
    }
}
