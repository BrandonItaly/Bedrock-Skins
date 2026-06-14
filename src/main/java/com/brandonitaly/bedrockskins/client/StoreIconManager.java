package com.brandonitaly.bedrockskins.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public final class StoreIconManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Identifier> cachedIcons = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> downloading = new ConcurrentHashMap<>();

    public static Identifier getIcon(String packId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        
        Identifier texIdx = Identifier.fromNamespaceAndPath("bedrockskins", "store_icon/" + packId.toLowerCase().replace("-", "_").replace(" ", "_"));
        
        if (cachedIcons.containsKey(packId)) {
            return cachedIcons.get(packId);
        }

        if (!downloading.containsKey(packId)) {
            downloading.put(packId, true);
            CompletableFuture.runAsync(() -> {
                try {
                    NativeImage img;
                    try (InputStream stream = URI.create(imageUrl).toURL().openStream()) {
                        img = NativeImage.read(stream);
                    }
                    if (img != null) {
                        Minecraft.getInstance().execute(() -> {
                            Minecraft.getInstance().getTextureManager().register(texIdx, new DynamicTexture(() -> "store_icon", img));
                            cachedIcons.put(packId, texIdx);
                        });
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to download store icon for " + packId + " from " + imageUrl + ": " + e.getMessage());
                }
            });
        }

        return null;
    }
}
