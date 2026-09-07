package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.appearance.AppearanceResolver;
import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.render.model.PersonaVisibilityModel;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends HumanoidRenderState> implements PersonaVisibilityModel {

    @Shadow public ModelPart head;
    @Shadow public ModelPart hat;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    @Unique private boolean bedrockSkins$personaVisibilitySaved;
    @Unique private boolean bedrockSkins$headVisible, bedrockSkins$hatVisible, bedrockSkins$bodyVisible;
    @Unique private boolean bedrockSkins$rightArmVisible, bedrockSkins$leftArmVisible;
    @Unique private boolean bedrockSkins$rightLegVisible, bedrockSkins$leftLegVisible;
    @Unique private boolean bedrockSkins$jacketVisible, bedrockSkins$rightSleeveVisible, bedrockSkins$leftSleeveVisible;
    @Unique private boolean bedrockSkins$rightPantsVisible, bedrockSkins$leftPantsVisible;

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void bedrockSkins$restoreBeforeAnimation(T state, CallbackInfo ci) {
        // Emote transforms are additive. Remove the previous entity/frame's
        // contribution before vanilla resets and poses this shared model.
        // Restoring at RETURN is too late: it subtracts the old delta from the
        // freshly reset pose, leaving attachments with only frame-to-frame motion.
        EmoteManager.restorePoseDeltas((HumanoidModel<?>) (Object) this);
        bedrockSkins$restorePersonaVisibility();
    }

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void bedrockSkins$syncToBedrockModel(T state, CallbackInfo ci) {
        if ((Object) this instanceof BedrockPlayerModel bedrockPlayerModel) {
            applyBedrockPartVisibility(bedrockPlayerModel, state);
            if (bedrockPlayerModel.personaCosmetic) {
                PersonaManager.applySideVisibility(bedrockPlayerModel,
                    BedrockRenderStateStore.getUniqueId(state));
            } else {
                applyPersonaOcclusion(state);
            }
            EmoteManager.apply((HumanoidModel<?>) (Object) this, BedrockRenderStateStore.getUniqueId(state));
            return;
        }

        SkinId skinId = AppearanceResolver.skinId(state);
        if (skinId != null) {
            BedrockPlayerModel bedrockModel = BedrockModelManager.getModel(skinId);
            if (bedrockModel != null) {
                copyPose(bedrockModel.head, this.head);
                copyPose(bedrockModel.hat, this.hat);
                copyPose(bedrockModel.body, this.body);
                copyPose(bedrockModel.rightArm, this.rightArm);
                copyPose(bedrockModel.leftArm, this.leftArm);
                copyPose(bedrockModel.rightLeg, this.rightLeg);
                copyPose(bedrockModel.leftLeg, this.leftLeg);
            }
        }
        applyPersonaOcclusion(state);
        EmoteManager.apply((HumanoidModel<?>) (Object) this, BedrockRenderStateStore.getUniqueId(state));
    }

    @Unique
    private void applyPersonaOcclusion(T state) {
        UUID uuid = BedrockRenderStateStore.getUniqueId(state);
        PersonaManager.Occlusion hidden = PersonaManager.occlusion(uuid);
        if (hidden.isEmpty()) return;

        bedrockSkins$headVisible = head.visible;
        bedrockSkins$hatVisible = hat.visible;
        bedrockSkins$bodyVisible = body.visible;
        bedrockSkins$rightArmVisible = rightArm.visible;
        bedrockSkins$leftArmVisible = leftArm.visible;
        bedrockSkins$rightLegVisible = rightLeg.visible;
        bedrockSkins$leftLegVisible = leftLeg.visible;
        if ((Object) this instanceof PlayerModel playerModel) {
            bedrockSkins$jacketVisible = playerModel.jacket.visible;
            bedrockSkins$rightSleeveVisible = playerModel.rightSleeve.visible;
            bedrockSkins$leftSleeveVisible = playerModel.leftSleeve.visible;
            bedrockSkins$rightPantsVisible = playerModel.rightPants.visible;
            bedrockSkins$leftPantsVisible = playerModel.leftPants.visible;
        }
        bedrockSkins$personaVisibilitySaved = true;

        if (hidden.head()) head.visible = false;
        if (hidden.hat()) hat.visible = false;
        if (hidden.body()) body.visible = false;
        if (hidden.rightArm()) rightArm.visible = false;
        if (hidden.leftArm()) leftArm.visible = false;
        if (hidden.rightLeg()) rightLeg.visible = false;
        if (hidden.leftLeg()) leftLeg.visible = false;
        if ((Object) this instanceof PlayerModel playerModel) {
            if (hidden.jacket()) playerModel.jacket.visible = false;
            if (hidden.rightSleeve()) playerModel.rightSleeve.visible = false;
            if (hidden.leftSleeve()) playerModel.leftSleeve.visible = false;
            if (hidden.rightPants()) playerModel.rightPants.visible = false;
            if (hidden.leftPants()) playerModel.leftPants.visible = false;
        }
    }

    @Override
    public void bedrockSkins$restorePersonaVisibility() {
        if (!bedrockSkins$personaVisibilitySaved) return;
        head.visible = bedrockSkins$headVisible;
        hat.visible = bedrockSkins$hatVisible;
        body.visible = bedrockSkins$bodyVisible;
        rightArm.visible = bedrockSkins$rightArmVisible;
        leftArm.visible = bedrockSkins$leftArmVisible;
        rightLeg.visible = bedrockSkins$rightLegVisible;
        leftLeg.visible = bedrockSkins$leftLegVisible;
        if ((Object) this instanceof PlayerModel playerModel) {
            playerModel.jacket.visible = bedrockSkins$jacketVisible;
            playerModel.rightSleeve.visible = bedrockSkins$rightSleeveVisible;
            playerModel.leftSleeve.visible = bedrockSkins$leftSleeveVisible;
            playerModel.rightPants.visible = bedrockSkins$rightPantsVisible;
            playerModel.leftPants.visible = bedrockSkins$leftPantsVisible;
        }
        bedrockSkins$personaVisibilitySaved = false;
    }

    @Unique
    private void applyBedrockPartVisibility(BedrockPlayerModel bedrockModel, T state) {
        bedrockModel.setChestplateVisible(true);
        bedrockModel.setHelmetVisible(true);
        bedrockModel.setLeggingsVisible(true);
        bedrockModel.setBootsVisible(true);

        boolean capeVisible = state instanceof AvatarRenderState avatarState && avatarState.showCape;
        if (capeVisible && hasActualCape(state)) {
            bedrockModel.setChestplateVisible(false);
        }

        if (!state.chestEquipment.isEmpty()) {
            bedrockModel.setChestplateVisible(false);
        }
        if (!state.headEquipment.isEmpty()) {
            bedrockModel.setHelmetVisible(false);
        }
        if (!state.legsEquipment.isEmpty()) {
            bedrockModel.setLeggingsVisible(false);
        }
        if (!state.feetEquipment.isEmpty()) {
            bedrockModel.setBootsVisible(false);
        }
    }

    @Unique
    private boolean hasActualCape(T state) {
        UUID uuid = BedrockRenderStateStore.getUniqueId(state);
        if (uuid == null) return false;

        try {
            var client = Minecraft.getInstance();
            if (client.getConnection() == null) return false;
            var entry = client.getConnection().getPlayerInfo(uuid);
            if (entry == null) return false;
            var textures = entry.getSkin();
            return textures.cape() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private static void copyPose(ModelPart from, ModelPart to) {
        if (from == null || to == null) return;
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
    }
}
