package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.SkinId;
import java.util.UUID;
import java.util.WeakHashMap;

public final class BedrockRenderStateStore {
    private static final WeakHashMap<Object, SkinId> SKIN_IDS = new WeakHashMap<>();
    private static final WeakHashMap<Object, UUID> UNIQUE_IDS = new WeakHashMap<>();

    private BedrockRenderStateStore() {}

    public static SkinId getSkinId(Object renderState) {
        if (renderState == null) return null;
        synchronized (SKIN_IDS) {
            return SKIN_IDS.get(renderState);
        }
    }

    public static void setSkinId(Object renderState, SkinId id) {
        if (renderState == null) return;
        synchronized (SKIN_IDS) {
            if (id == null) {
                SKIN_IDS.remove(renderState);
            } else {
                SKIN_IDS.put(renderState, id);
            }
        }
    }

    public static UUID getUniqueId(Object renderState) {
        if (renderState == null) return null;
        synchronized (UNIQUE_IDS) {
            return UNIQUE_IDS.get(renderState);
        }
    }

    public static void setUniqueId(Object renderState, UUID id) {
        if (renderState == null) return;
        synchronized (UNIQUE_IDS) {
            if (id == null) {
                UNIQUE_IDS.remove(renderState);
            } else {
                UNIQUE_IDS.put(renderState, id);
            }
        }
    }
}
