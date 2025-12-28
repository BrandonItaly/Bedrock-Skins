package com.brandonitaly.bedrockskins.mixins;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void updateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayerEntity) {
            if (state instanceof BedrockSkinState) {
                ((BedrockSkinState) state).setUniqueId(((AbstractClientPlayerEntity) player).getUuid());
            }
        }
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void getTexture(PlayerEntityRenderState state, CallbackInfoReturnable<Identifier> ci) {
        java.util.UUID uuid = null;
        if (state instanceof BedrockSkinState) {
            uuid = ((BedrockSkinState) state).getUniqueId();
        }
        if (uuid != null) {
            String skinKey = SkinManager.getSkin(uuid.toString());
            if (skinKey != null) {
                var skin = SkinPackLoader.loadedSkins.get(skinKey);
                if (skin != null && skin.identifier != null) {
                    ci.setReturnValue(skin.identifier);
                }
            }
        }
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void renderRightArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        var uuid = player.getUuid();
        var bedrockModel = BedrockModelManager.getModel(uuid);
        if (bedrockModel != null) {
            String partKey = "right_arm";
            var part = bedrockModel.partsMap.get(partKey);
            if (part == null) {
                partKey = "rightArm";
                part = bedrockModel.partsMap.get(partKey);
            }
            String sleeveKey = "right_sleeve";
            var sleeve = bedrockModel.partsMap.get(sleeveKey);
            if (sleeve == null) {
                sleeveKey = "rightSleeve";
                sleeve = bedrockModel.partsMap.get(sleeveKey);
            }
            if (part != null) {
                String skinKey = SkinManager.getSkin(uuid.toString());
                var bedrockSkin = (skinKey != null) ? SkinPackLoader.loadedSkins.get(skinKey) : null;
                Identifier texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;
                var layer = RenderLayers.entityTranslucent(texture);
                final var finalPart = part;
                final var finalSleeve = sleeve;
                queue.submitCustom(matrices, layer, (entry, consumer) -> {
                    MatrixStack ms = new MatrixStack();
                    ms.peek().getPositionMatrix().set(entry.getPositionMatrix());
                    ms.peek().getNormalMatrix().set(entry.getNormalMatrix());
                    finalPart.resetTransform();
                    if (finalSleeve != null) finalSleeve.resetTransform();
                    finalPart.render(ms, consumer, light, OverlayTexture.DEFAULT_UV);
                    boolean sleeveIsChild = finalPart.hasChild("right_sleeve") || finalPart.hasChild("rightSleeve");
                    if (finalSleeve != null && !sleeveIsChild) {
                        finalSleeve.render(ms, consumer, light, OverlayTexture.DEFAULT_UV);
                    }
                });
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void renderLeftArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        var uuid = player.getUuid();
        var bedrockModel = BedrockModelManager.getModel(uuid);
        if (bedrockModel != null) {
            String partKey = "left_arm";
            var part = bedrockModel.partsMap.get(partKey);
            if (part == null) {
                partKey = "leftArm";
                part = bedrockModel.partsMap.get(partKey);
            }
            String sleeveKey = "left_sleeve";
            var sleeve = bedrockModel.partsMap.get(sleeveKey);
            if (sleeve == null) {
                sleeveKey = "leftSleeve";
                sleeve = bedrockModel.partsMap.get(sleeveKey);
            }
            if (part != null) {
                String skinKey = SkinManager.getSkin(uuid.toString());
                var bedrockSkin = (skinKey != null) ? SkinPackLoader.loadedSkins.get(skinKey) : null;
                Identifier texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;
                var layer = RenderLayers.entityTranslucent(texture);
                final var finalPart = part;
                final var finalSleeve = sleeve;
                queue.submitCustom(matrices, layer, (entry, consumer) -> {
                    MatrixStack ms = new MatrixStack();
                    ms.peek().getPositionMatrix().set(entry.getPositionMatrix());
                    ms.peek().getNormalMatrix().set(entry.getNormalMatrix());
                    finalPart.resetTransform();
                    if (finalSleeve != null) finalSleeve.resetTransform();
                    finalPart.render(ms, consumer, light, OverlayTexture.DEFAULT_UV);
                    boolean sleeveIsChild = finalPart.hasChild("left_sleeve") || finalPart.hasChild("leftSleeve");
                    if (finalSleeve != null && !sleeveIsChild) {
                        finalSleeve.render(ms, consumer, light, OverlayTexture.DEFAULT_UV);
                    }
                });
                ci.cancel();
            }
        }
    }
}
