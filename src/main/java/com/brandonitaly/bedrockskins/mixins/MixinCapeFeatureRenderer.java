package com.brandonitaly.bedrockskins.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.resources./*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.RenderType;*/
//?}

@Mixin(CapeLayer.class)
public abstract class MixinCapeFeatureRenderer {

    // Allow capes to use the translucent render layer instead of solid
    //? if >=1.21.11 {
    @WrapOperation(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
        )
    )
    private RenderType useTranslucentLayer(Identifier texture, Operation<RenderType> original) {
        return RenderTypes.entityTranslucent(texture);
    }
    //?} else {
    /*@WrapOperation(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
        )
    )
    private RenderType useTranslucentLayer(ResourceLocation texture, Operation<RenderType> original) {
        return RenderType.entityTranslucent(texture);
    }*/
    //?}
}