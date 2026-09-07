package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.client.persistence.BedrockSkinsConfig;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true)
    private void bedrockskins$adjustEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!BedrockSkinsConfig.isAdjustCameraHeightEnabled()) {
            return;
        }

        if ((Object) this instanceof Player player && player.level() != null && player.level().isClientSide()) {
            SkinId skinId = SkinManager.getSkin(player.getUUID());
            if (skinId != null) {
                BedrockPlayerModel model = BedrockModelManager.getModel(skinId);

                if (model != null && model.heightMultiplier != 1.0f) {
                    cir.setReturnValue(cir.getReturnValue() * model.heightMultiplier);
                }
            }
        }
    }
}
