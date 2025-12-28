package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(CapeFeatureRenderer.class)
public abstract class MixinCapeFeatureRenderer {
    @Unique
    private final ThreadLocal<Boolean> pushed = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        UUID uuid = (state instanceof BedrockSkinState) ? ((BedrockSkinState) state).getUniqueId() : null;
        if (uuid == null) return;

        var model = BedrockModelManager.getModel(uuid);
        if (model != null && model.capeYOffset != 0f) {
            matrices.push();
            double translateY = model.capeYOffset * 0.0625f;
            matrices.translate(0.0, translateY, 0.0);
            pushed.set(true);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (Boolean.TRUE.equals(pushed.get())) {
            try { matrices.pop(); } catch (Exception ignored) { }
            pushed.remove();
        }
    }
}
