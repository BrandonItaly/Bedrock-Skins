package com.brandonitaly.bedrockskins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.resources.Identifier;

//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.RenderType;*/
//?}

@Mixin(CapeLayer.class)
public abstract class CapeFeatureRendererMixin {

    // Allow capes to use the translucent render layer instead of solid
    @WrapOperation(
        method = "submit",
        at = @At(
            value = "INVOKE",
            //? if >=1.21.11 {
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            //?} else {
            /*target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"*/
            //?}
        )
    )
    private RenderType useTranslucentLayer(Identifier texture, Operation<RenderType> original) {
        //~ if <1.21.11 'RenderTypes' -> 'RenderType' {
        return RenderTypes.entityTranslucent(texture);
        //~}
    }
}