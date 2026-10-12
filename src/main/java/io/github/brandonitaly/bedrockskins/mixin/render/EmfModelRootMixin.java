package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.model.ResourcePackAnimatedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPartRoot", remap = false)
public abstract class EmfModelRootMixin implements ResourcePackAnimatedModel {
    @Shadow @Override public abstract boolean hasAnimation();
    @Shadow @Override public abstract void animate();
}
