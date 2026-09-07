package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.appearance.AppearanceResolver;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.render.model.PersonaVisibilityModel;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Shadow
    protected EntityModel<?> model;

    @Shadow
    protected List<?> layers;

    @Unique
    private Object originalModel;

    @Unique
    private CapeLayer bedrockSkins$capturedCapeLayer;

    // null = unchecked, true = suppressed (no CapeLayer in layers), false = present
    @Unique
    @Nullable
    private Boolean bedrockSkins$capeLayerSuppressed;

    @Inject(method = "addLayer", at = @At("HEAD"))
    private void onAddLayerHead(RenderLayer<?, ?> layer, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) layer instanceof CapeLayer capeLayer) {
            this.bedrockSkins$capturedCapeLayer = capeLayer;
            this.bedrockSkins$capeLayerSuppressed = null;
        }
    }

    // --- Shared Logic ---

    @Unique
    private void bedrockSkins$swapModel(LivingEntityRenderState state) {
        if (!(state instanceof AvatarRenderState))
            return;

        BedrockPlayerModel bedrockModel = AppearanceResolver.baseModel((AvatarRenderState) state);
        if (bedrockModel == null)
            return;

        this.originalModel = this.model;
        this.model = bedrockModel;

        if (this.originalModel instanceof net.minecraft.client.model.player.PlayerModel playerModel) {
            EmoteManager.restorePoseDeltas(bedrockModel);
            bedrockModel.copyFromVanilla(playerModel);
        }
    }

    @Unique
    private void bedrockSkins$restoreModel() {
        if (this.originalModel != null) {
            this.model = (EntityModel<?>) this.originalModel;
            this.originalModel = null;
        }
    }

    // --- Injectors ---

    @Inject(method = "submit", at = @At("HEAD"))
    private void onRenderHead(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue,
            CameraRenderState camera, CallbackInfo ci) {
        this.bedrockSkins$swapModel(state);
    }

    @Inject(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
        )
    )
    private void onRenderBeforePopPose(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue,
            CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatarState && !avatarState.isInvisible) {
            java.util.UUID uuid = BedrockRenderStateStore.getUniqueId(avatarState);
            if (uuid != null) {
                SkinId skinId = AppearanceResolver.skinId(avatarState);
                boolean slim = AppearanceResolver.isSlim(avatarState, skinId);

                for (var cosmetic : PersonaManager.equipped(uuid)) {
                    BedrockPlayerModel cosmeticModel = PersonaManager.model(cosmetic, slim);
                    var cosmeticTexture = PersonaManager.texture(uuid, cosmetic);
                    if (cosmeticModel == null || cosmeticTexture == null) continue;
                    PersonaManager.prepareTexture(uuid, cosmetic);
                    //? if <1.21.11 {
                    /*var renderType = net.minecraft.client.renderer.RenderType.entityTranslucent(cosmeticTexture);*/
                    //?} else {
                    var renderType = net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(cosmeticTexture);
                    //?}
                    //? if <26.3-snapshot-5 {
                    queue.submitModel(cosmeticModel, avatarState, matrices, renderType,
                        avatarState.lightCoords, OverlayTexture.NO_OVERLAY, avatarState.outlineColor, null);
                    //?} else {
                    /*queue.submitModel(cosmeticModel, avatarState, matrices, renderType,
                        avatarState.lightCoords, OverlayTexture.NO_OVERLAY, avatarState.outlineColor);*/
                    //?}
                }
            }
        }

        if (this.bedrockSkins$capturedCapeLayer == null) return;
        if (!(state instanceof AvatarRenderState avatarState)) return;
        if (!BedrockRenderStateStore.isGuiRender(avatarState)) return;
        if (avatarState.skin == null || avatarState.skin.cape() == null) return;
        if (!avatarState.showCape || avatarState.isInvisible) return;

        // Lazily scan layers once to check if CapeLayer was suppressed by another mod
        if (this.bedrockSkins$capeLayerSuppressed == null) {
            boolean found = false;
            if (this.layers != null) {
                for (Object layer : this.layers) {
                    if (layer instanceof CapeLayer) { found = true; break; }
                }
            }
            this.bedrockSkins$capeLayerSuppressed = !found;
        }

        if (this.bedrockSkins$capeLayerSuppressed) {
            this.bedrockSkins$capturedCapeLayer.submit(
                matrices, queue, avatarState.lightCoords, avatarState,
                avatarState.bodyRot, avatarState.xRot
            );
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void onRenderReturn(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue,
            CameraRenderState camera, CallbackInfo ci) {
        if (this.model instanceof PersonaVisibilityModel visibilityModel) {
            visibilityModel.bedrockSkins$restorePersonaVisibility();
        }
        this.bedrockSkins$restoreModel();
    }
}
