package io.github.brandonitaly.bedrockskins.mixin.render;

import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bedrockSkins$disableBobView(CallbackInfo ci) {
        if (!(Minecraft.getInstance().getCameraEntity() instanceof Player player)) return;
        
        SkinId skinId = SkinManager.getSkin(player.getUUID());
        if (skinId == null) return;
        
        BedrockPlayerModel model = BedrockModelManager.getModel(skinId);
        if (model != null && model.isStationaryLegs()) {
            ci.cancel();
        }
    }
}
