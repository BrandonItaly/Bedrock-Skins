package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
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
        if (SkinManager.getLocalSelectedKey() != null) {
            return;
        }
        
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
        if (SkinManager.getLocalSelectedKey() != null) {
            return;
        }
        
        instance.submitModelPart(modelPart, poseStack, renderType, light, overlay, sprite);
    }*/
    //?}
}