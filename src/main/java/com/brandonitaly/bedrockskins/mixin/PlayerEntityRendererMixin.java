package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model./*? if <1.21.11 {*//**//*?} else {*/player./*?}*/PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer./*? if <1.21.11 {*//*RenderType*//*?} else {*/rendertype.RenderTypes/*?}*/;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Unique
    private boolean bedrockSkins$overrideActive;

    @Unique
    private SkinId bedrockSkins$overrideSkinId;

    @Unique
    private ModelPart bedrockSkins$overrideBedrockArm;

    @Unique
    private ModelPart bedrockSkins$overrideBedrockSleeve;

    @Unique
    private boolean bedrockSkins$overrideSleeveIsChild;

    @Unique
    private void bedrockSkins$clearOverride() {
        this.bedrockSkins$overrideActive = false;
        this.bedrockSkins$overrideSkinId = null;
        this.bedrockSkins$overrideBedrockArm = null;
        this.bedrockSkins$overrideBedrockSleeve = null;
        this.bedrockSkins$overrideSleeveIsChild = false;
    }

    @Unique
    private static ModelPart bedrockSkins$getVanillaSleeve(HumanoidModel<?> model, boolean isRightArm) {
        if (model instanceof PlayerModel playerModel) {
            return isRightArm ? playerModel.rightSleeve : playerModel.leftSleeve;
        }
        return null;
    }

    @Unique
    private boolean bedrockSkins$resolveArmParts(boolean isRightArm, SkinId skinId) {
        if (skinId == null) return false;
        var bedrockModel = BedrockModelManager.getModel(skinId);
        if (bedrockModel == null) return false;

        String side = isRightArm ? "right" : "left";
        var parts = bedrockModel.partsMap;

        var arm = parts.get(side + "_arm");
        if (arm == null) arm = parts.get(side + "Arm");
        if (arm == null) return false;

        var sleeve = parts.get(side + "_sleeve");
        if (sleeve == null) sleeve = parts.get(side + "Sleeve");

        this.bedrockSkins$overrideBedrockArm = arm;
        this.bedrockSkins$overrideBedrockSleeve = sleeve;
        this.bedrockSkins$overrideSleeveIsChild = arm.hasChild(side + "_sleeve") || arm.hasChild(side + "Sleeve");
        return true;
    }

    @Unique
    private void bedrockSkins$renderArm(PoseStack matrices, int light, Identifier skinTexture, boolean sleeveVisible, ModelPart vanillaArm, ModelPart vanillaSleeve, SkinId skinId, ModelPart bedrockArm, ModelPart bedrockSleeve, boolean sleeveIsChild, SubmitNodeCollector queue) {
        var bedrockSkin = SkinPackLoader.getLoadedSkin(skinId);
        var texture = (bedrockSkin != null && bedrockSkin.identifier != null) ? bedrockSkin.identifier : skinTexture;

        //? if <1.21.11 {
        /*var layer = RenderType.entityTranslucent(texture);*/
        //?} else {
        var layer = RenderTypes.entityTranslucent(texture);
        //?}

        final PartPose armPose = vanillaArm.storePose();

        queue.submitCustomGeometry(matrices, layer, (entry, consumer) -> {
            PoseStack ms = new PoseStack();
            ms.last().pose().set(entry.pose());
            ms.last().normal().set(entry.normal());

            bedrockArm.resetPose();
            bedrockArm.loadPose(armPose);
            bedrockArm.visible = true;
            if (bedrockSleeve != null) {
                if (!sleeveIsChild) {
                    bedrockSleeve.resetPose();
                    bedrockSleeve.loadPose(armPose);
                }
                bedrockSleeve.visible = sleeveVisible;
            }

            bedrockArm.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);

            if (bedrockSleeve != null && !sleeveIsChild && sleeveVisible) {
                bedrockSleeve.render(ms, consumer, light, OverlayTexture.NO_OVERLAY);
            }

            vanillaArm.skipDraw = false;
            if (vanillaSleeve != null) {
                vanillaSleeve.skipDraw = false;
            }
        });
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void updateRenderState(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer cp && state instanceof BedrockRenderStateAccessor skinState) {
            java.util.UUID uuid = cp.getUUID();
            skinState.bedrockSkins$setUniqueId(uuid);
            skinState.bedrockSkins$setBedrockSkinId(SkinManager.getSkin(uuid));
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void bedrockSkins$hideVanillaHand(PoseStack matrices, SubmitNodeCollector queue, int light, Identifier tex, ModelPart arm, boolean sleeve, CallbackInfo ci) {
        this.bedrockSkins$clearOverride();

        HumanoidModel<?> model = ((AvatarRenderer<?>)(Object)this).getModel();
        boolean isRightArm = (arm == model.rightArm);
        SkinId skinId = SkinManager.getLocalSelectedKey();

        arm.skipDraw = false;

        boolean hasParts = bedrockSkins$resolveArmParts(isRightArm, skinId);
        
        if (hasParts) {
            this.bedrockSkins$overrideActive = true;
            this.bedrockSkins$overrideSkinId = skinId;

            arm.skipDraw = true;

            ModelPart vanillaSleeve = bedrockSkins$getVanillaSleeve(model, isRightArm);
            if (vanillaSleeve != null) vanillaSleeve.skipDraw = true;
        }
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void bedrockSkins$renderBedrockHand(PoseStack matrices, SubmitNodeCollector queue, int light, Identifier tex, ModelPart arm, boolean sleeveVisible, CallbackInfo ci) {
        if (!this.bedrockSkins$overrideActive || this.bedrockSkins$overrideSkinId == null || this.bedrockSkins$overrideBedrockArm == null) {
            return;
        }

        HumanoidModel<?> model = ((AvatarRenderer<?>)(Object)this).getModel();
        boolean isRightArm = (arm == model.rightArm);
        ModelPart vanillaSleeve = bedrockSkins$getVanillaSleeve(model, isRightArm);

        try {
            bedrockSkins$renderArm(matrices, light, tex, sleeveVisible, arm, vanillaSleeve, this.bedrockSkins$overrideSkinId, this.bedrockSkins$overrideBedrockArm, this.bedrockSkins$overrideBedrockSleeve, this.bedrockSkins$overrideSleeveIsChild, queue);
        } finally {
            this.bedrockSkins$clearOverride();
        }
    }

    @Inject(method = "isEntityUpsideDown", at = @At("HEAD"), cancellable = true)
    private void isEntityUpsideDown(Avatar player, CallbackInfoReturnable<Boolean> ci) {
        if (player instanceof AbstractClientPlayer cp) {
            SkinId skinId = SkinManager.getSkin(cp.getUUID());
            if (skinId != null) {
                var skin = SkinPackLoader.getLoadedSkin(skinId);
                if (skin != null && skin.isUpsideDown()) ci.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void getTexture(AvatarRenderState state, CallbackInfoReturnable<Identifier> ci) {
        if (state instanceof BedrockRenderStateAccessor skinState) {
            java.util.UUID uuid = skinState.bedrockSkins$getUniqueId();
            if (uuid != null) {
                SkinId skinId = SkinManager.getSkin(uuid);
                if (skinId != null) {
                    var skin = SkinPackLoader.getLoadedSkin(skinId);
                    if (skin != null && skin.identifier != null) ci.setReturnValue(skin.identifier);
                }
            }
        }
    }
}