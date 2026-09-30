package io.github.brandonitaly.bedrockskins.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.resources.Identifier;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Mixin(CapeLayer.class)
public abstract class CapeFeatureRendererMixin {

    // Allow capes to use the translucent render layer instead of solid
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
}
