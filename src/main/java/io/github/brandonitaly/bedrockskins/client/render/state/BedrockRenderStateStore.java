package io.github.brandonitaly.bedrockskins.client.render.state;

import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.client.render.model.ResourcePackAnimationPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import java.util.UUID;
import java.util.WeakHashMap;

public final class BedrockRenderStateStore {
    private static final WeakHashMap<Object, RenderContext> CONTEXTS = new WeakHashMap<>();

    private static final class RenderContext {
        private SkinId skinId;
        private UUID uniqueId;
        private boolean guiRender;
        private PlayerModel animationSource;
        private ResourcePackAnimationPose animationPose;
    }

    private BedrockRenderStateStore() {}

    public static void setAnimationSource(Object state, PlayerModel source) {
        synchronized (CONTEXTS) {
            RenderContext context = CONTEXTS.computeIfAbsent(state, ignored -> new RenderContext());
            context.animationSource = source;
            context.animationPose = null;
        }
    }

    public static ResourcePackAnimationPose animationPose(AvatarRenderState state) {
        synchronized (CONTEXTS) {
            RenderContext context = CONTEXTS.get(state);
            if (context == null) return null;
            if (context.animationPose == null && context.animationSource != null) {
                context.animationPose = ResourcePackAnimationPose.animate(context.animationSource, state);
            }
            return context.animationPose;
        }
    }

    public static void setAnimationPose(AvatarRenderState state, ResourcePackAnimationPose pose) {
        synchronized (CONTEXTS) {
            CONTEXTS.computeIfAbsent(state, ignored -> new RenderContext()).animationPose = pose;
        }
    }

    public static boolean isGuiRender(Object renderState) {
        if (renderState == null) return false;
        synchronized (CONTEXTS) {
            RenderContext context = CONTEXTS.get(renderState);
            return context != null && context.guiRender;
        }
    }

    public static void setGuiRender(Object renderState, boolean isGui) {
        if (renderState == null) return;
        synchronized (CONTEXTS) {
            CONTEXTS.computeIfAbsent(renderState, ignored -> new RenderContext()).guiRender = isGui;
        }
    }

    public static SkinId getSkinId(Object renderState) {
        if (renderState == null) return null;
        synchronized (CONTEXTS) {
            RenderContext context = CONTEXTS.get(renderState);
            return context != null ? context.skinId : null;
        }
    }

    public static void setSkinId(Object renderState, SkinId id) {
        if (renderState == null) return;
        synchronized (CONTEXTS) {
            CONTEXTS.computeIfAbsent(renderState, ignored -> new RenderContext()).skinId = id;
        }
    }

    public static UUID getUniqueId(Object renderState) {
        if (renderState == null) return null;
        synchronized (CONTEXTS) {
            RenderContext context = CONTEXTS.get(renderState);
            return context != null ? context.uniqueId : null;
        }
    }

    public static void setUniqueId(Object renderState, UUID id) {
        if (renderState == null) return;
        synchronized (CONTEXTS) {
            CONTEXTS.computeIfAbsent(renderState, ignored -> new RenderContext()).uniqueId = id;
        }
    }

}
