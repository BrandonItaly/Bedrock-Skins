package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(ElytraFeatureRenderer.class)
public abstract class MixinElytraFeatureRenderer {
    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState) ? ((BedrockSkinState) state).getUniqueId() : null;
        if (uuid != null) {
            var model = BedrockModelManager.getModel(uuid);
            if (model != null && model.capeYOffset != 0f) {
                matrices.push();
                double translateY = model.capeYOffset * 0.0625f;
                matrices.translate(0.0, translateY, 0.0);
                pushed.set(true);
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.pop(); } catch (Exception ignored) { }
            pushed.remove();
        }
    }
}
