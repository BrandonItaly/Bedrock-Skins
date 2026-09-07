package io.github.brandonitaly.bedrockskins.client.render.state;

import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import java.util.UUID;
import java.util.WeakHashMap;

public final class BedrockRenderStateStore {
    private static final WeakHashMap<Object, RenderContext> CONTEXTS = new WeakHashMap<>();

    private static final class RenderContext {
        private SkinId skinId;
        private UUID uniqueId;
        private boolean guiRender;
    }

    private BedrockRenderStateStore() {}

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
