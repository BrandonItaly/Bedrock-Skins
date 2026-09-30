package io.github.brandonitaly.bedrockskins.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brandonitaly.bedrockskins.client.appearance.AppearanceResolver;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import io.github.brandonitaly.bedrockskins.mixin.render.ModelPartAccessor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import java.util.ArrayList;
import java.util.List;

public final class ElytraAttachmentPose {
    private ElytraAttachmentPose() {}

    public static boolean applies(HumanoidRenderState state) {
        return state instanceof AvatarRenderState && (AppearanceResolver.skinId(state) != null
            || EmoteManager.isPlaying(BedrockRenderStateStore.getUniqueId(state)));
    }

    /** Apply the torso's complete ancestry, including animated root and waist bones. */
    public static void apply(PoseStack matrices, HumanoidModel<?> model) {
        ModelPart body = model instanceof BedrockPlayerModel bedrock ? bedrock.customBody : model.body;
        List<ModelPart> path = new ArrayList<>(4);
        if (!findPath(model.root(), body, path)) return;
        for (ModelPart part : path) {
            part.translateAndRotate(matrices);
        }
    }

    private static boolean findPath(ModelPart part, ModelPart target, List<ModelPart> path) {
        path.add(part);
        if (part == target) return true;
        for (ModelPart child : ((ModelPartAccessor) (Object) part).bedrockSkins$getChildren().values()) {
            if (findPath(child, target, path)) return true;
        }
        path.remove(path.size() - 1);
        return false;
    }
}
