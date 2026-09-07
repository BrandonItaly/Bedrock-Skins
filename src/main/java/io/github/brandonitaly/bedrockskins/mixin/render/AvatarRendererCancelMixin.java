package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
//? if <1.21.11 {
/*import net.minecraft.client.renderer.RenderType;*/
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
//? if <=26.2 {
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//?} else {
// import net.minecraft.client.renderer.texture.UvMapping;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AvatarRenderer.class)
public class AvatarRendererCancelMixin {

    //? if <=26.2 {
    @Redirect(
        method = "renderHand",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
        )
    )
    private void bedrockSkins$swallowVanillaArm(SubmitNodeCollector instance, ModelPart modelPart, PoseStack poseStack, RenderType renderType, int light, int overlay, TextureAtlasSprite sprite) {
        if (bedrockSkins$shouldSwallowArmPart(modelPart)) return;
        
        instance.submitModelPart(modelPart, poseStack, renderType, light, overlay, sprite);
    }
    //?} else {
    /*@Redirect(
        method = "renderHand",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/UvMapping;)V"
        )
    )
    private void bedrockSkins$swallowVanillaArm(SubmitNodeCollector instance, ModelPart modelPart, PoseStack poseStack, RenderType renderType, int light, int overlay, UvMapping sprite) {
        if (bedrockSkins$shouldSwallowArmPart(modelPart)) return;
        
        instance.submitModelPart(modelPart, poseStack, renderType, light, overlay, sprite);
    }*/
    //?}

    @Unique
    private boolean bedrockSkins$shouldSwallowArmPart(ModelPart part) {
        if (SkinManager.getLocalSelectedKey() != null) return true;
        var player = net.minecraft.client.Minecraft.getInstance().player;
        PersonaManager.Occlusion hidden = PersonaManager.occlusion(player == null ? null : player.getUUID());
        HumanoidModel<?> model = ((AvatarRenderer<?>)(Object)this).getModel();
        if (part == model.rightArm) return hidden.rightArm();
        if (part == model.leftArm) return hidden.leftArm();
        if (model instanceof PlayerModel playerModel) {
            if (part == playerModel.rightSleeve) return hidden.rightSleeve();
            if (part == playerModel.leftSleeve) return hidden.leftSleeve();
        }
        return false;
    }
}
