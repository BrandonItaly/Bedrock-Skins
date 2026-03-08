package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true)
    private void bedrockskins$adjustEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!com.brandonitaly.bedrockskins.client.BedrockSkinsConfig.isAdjustCameraHeightEnabled()) {
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