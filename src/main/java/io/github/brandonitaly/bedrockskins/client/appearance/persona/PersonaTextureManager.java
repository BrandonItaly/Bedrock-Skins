package io.github.brandonitaly.bedrockskins.client.appearance.persona;

import io.github.brandonitaly.bedrockskins.pack.model.AssetSource;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaTexturePayload;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Owns independently tinted texture variants and animation frames for Persona pieces. */
final class PersonaTextureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, byte[]> PAYLOADS = new ConcurrentHashMap<>();
    private static final Map<VariantKey, ManagedTexture> TEXTURES = new ConcurrentHashMap<>();

    private PersonaTextureManager() {}

    static void register(LoadedCosmetic cosmetic) {
        if (!(cosmetic.texture instanceof AssetSource.Memory memory)) return;
        PAYLOADS.put(cosmetic.id, memory.data());
        cosmetic.textureIdentifier = texture(cosmetic, cosmetic.defaultTintColor);
    }

    static Identifier texture(LoadedCosmetic cosmetic, int color) {
        if (cosmetic == null) return null;
        int effectiveColor = cosmetic.tintable ? color & 0xFFFFFF : cosmetic.defaultTintColor;
        VariantKey key = new VariantKey(cosmetic.id, effectiveColor);
        ManagedTexture existing = TEXTURES.get(key);
        if (existing != null) return existing.id;
        byte[] payload = PAYLOADS.get(cosmetic.id);
        if (payload == null) return cosmetic.textureIdentifier;
        ManagedTexture created = create(cosmetic, key, payload);
        if (created == null) return cosmetic.textureIdentifier;
        ManagedTexture raced = TEXTURES.putIfAbsent(key, created);
        if (raced != null) {
            created.closeAndRelease();
            return raced.id;
        }
        return created.id;
    }

    static void prepare(LoadedCosmetic cosmetic, int color) {
        if (cosmetic == null) return;
        Identifier ignored = texture(cosmetic, color);
        ManagedTexture animation = TEXTURES.get(new VariantKey(cosmetic.id,
            cosmetic.tintable ? color & 0xFFFFFF : cosmetic.defaultTintColor));
        if (animation == null || animation.regions.isEmpty()) return;
        long animationTick = System.nanoTime() / 115_000_000L;
        if (animation.lastTick == animationTick) return;
        animation.lastTick = animationTick;
        NativeImage target = animation.texture.getPixels();
        if (target == null) return;
        for (AnimatedRegion region : animation.regions) {
            int frame = (int) (animationTick % region.frameCount);
            int sourceY = frame * region.frameHeight;
            if (sourceY + region.frameHeight > region.tinted.getHeight()) continue;
            region.tinted.copyRect(target, 0, sourceY,
                region.x, region.y, region.width, region.frameHeight, false, false);
        }
        animation.texture.upload();
    }

    static void remove(String cosmeticId) {
        PAYLOADS.remove(cosmeticId);
        List<VariantKey> keys = TEXTURES.keySet().stream().filter(key -> key.cosmeticId.equals(cosmeticId)).toList();
        for (VariantKey key : keys) {
            ManagedTexture texture = TEXTURES.remove(key);
            if (texture != null) texture.closeAndRelease();
        }
    }

    static void clear() {
        TEXTURES.values().forEach(ManagedTexture::closeAndRelease);
        TEXTURES.clear();
        PAYLOADS.clear();
    }

    private static ManagedTexture create(LoadedCosmetic cosmetic, VariantKey key, byte[] data) {
        try {
            PersonaTexturePayload payload = PersonaTexturePayload.decode(data);
            NativeImage base = read(payload.baseTexture());
            NativeImage target = read(payload.baseTexture());
            NativeImage mask = payload.tintMask().length == 0 ? null : read(payload.tintMask());
            if (mask != null && (mask.getWidth() != base.getWidth() || mask.getHeight() != base.getHeight())) {
                mask.close();
                mask = null;
            }
            Identifier id = Identifier.fromNamespaceAndPath("bedrockskins",
                "persona/" + sanitize(cosmetic.id) + "/" + String.format("%06x", key.color));
            DynamicTexture texture = new DynamicTexture(() -> "persona_cosmetic", target);
            Minecraft.getInstance().getTextureManager().register(id, texture);

            List<AnimatedRegion> regions = new ArrayList<>();
            for (PersonaTexturePayload.AnimationRegion region : payload.animations()) {
                NativeImage source = read(region.textureStrip());
                NativeImage tinted = read(region.textureStrip());
                NativeImage stripMask = region.tintMaskStrip().length == 0 ? null : read(region.tintMaskStrip());
                if (region.atlasX() + region.width() > target.getWidth()
                        || region.atlasY() + region.frameHeight() > target.getHeight()
                        || region.width() > source.getWidth()
                        || region.frameHeight() * region.frameCount() > source.getHeight()) {
                    source.close();
                    tinted.close();
                    if (stripMask != null) stripMask.close();
                    throw new IllegalArgumentException("Persona animation region exceeds its texture bounds");
                }
                regions.add(new AnimatedRegion(region.atlasX(), region.atlasY(), region.width(),
                    region.frameHeight(), region.frameCount(), source, stripMask, tinted));
            }

            ManagedTexture result = new ManagedTexture(id, texture, base, mask,
                payload.tintBaseColor(), regions);
            result.applyTint(key.color);
            return result;
        } catch (Exception e) {
            LOGGER.warn("Failed to register Persona texture variant {} #{}", cosmetic.displayName,
                String.format("%06X", key.color), e);
            return null;
        }
    }

    private static NativeImage read(byte[] data) throws Exception {
        return NativeImage.read(new ByteArrayInputStream(data));
    }

    private static void tint(NativeImage source, NativeImage mask, NativeImage target,
                             int baseColor, int selectedColor) {
        int width = Math.min(source.getWidth(), target.getWidth());
        int height = Math.min(source.getHeight(), target.getHeight());
        int baseR = (baseColor >> 16) & 0xFF;
        int baseG = (baseColor >> 8) & 0xFF;
        int baseB = baseColor & 0xFF;
        int selectedR = (selectedColor >> 16) & 0xFF;
        int selectedG = (selectedColor >> 8) & 0xFF;
        int selectedB = selectedColor & 0xFF;
        float[] baseHsb = Color.RGBtoHSB(baseR, baseG, baseB, null);
        float[] selectedHsb = Color.RGBtoHSB(selectedR, selectedG, selectedB, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                int amount = mask == null ? 0 : (mask.getPixel(x, y) >> 16) & 0xFF;
                if (amount != 0) {
                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;
                    int recolored = recolor(red, green, blue, baseHsb, selectedHsb);
                    red = blend(red, (recolored >> 16) & 0xFF, amount);
                    green = blend(green, (recolored >> 8) & 0xFF, amount);
                    blue = blend(blue, recolored & 0xFF, amount);
                    pixel = (pixel & 0xFF000000) | (red << 16) | (green << 8) | blue;
                }
                target.setPixel(x, y, pixel);
            }
        }
    }

    /**
     * Transfers the selected color from the authored base color in HSV space.
     * Persona textures are not neutral masks: their tinted pixels contain
     * shading and small color variations around tint_base_color. Applying an
     * independent RGB ratio to those pixels changes the requested hue (and is
     * especially unstable when a base channel is zero). HSV transfer preserves
     * those authored variations while moving the pixel to the selected hue.
     */
    private static int recolor(int red, int green, int blue, float[] baseHsb, float[] selectedHsb) {
        float[] pixelHsb = Color.RGBtoHSB(red, green, blue, null);
        float hue = wrapHue(pixelHsb[0] + selectedHsb[0] - baseHsb[0]);
        float saturation = transferComponent(pixelHsb[1], baseHsb[1], selectedHsb[1]);
        float brightness = transferComponent(pixelHsb[2], baseHsb[2], selectedHsb[2]);
        return Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }

    private static float transferComponent(float value, float base, float selected) {
        if (base <= 1.0e-6F) return selected;
        return Math.min(1.0F, value * selected / base);
    }

    private static float wrapHue(float hue) {
        hue %= 1.0F;
        return hue < 0.0F ? hue + 1.0F : hue;
    }

    private static int blend(int from, int to, int amount) {
        return (from * (255 - amount) + to * amount + 127) / 255;
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    private record VariantKey(String cosmeticId, int color) {}

    private record AnimatedRegion(int x, int y, int width, int frameHeight, int frameCount,
                                  NativeImage source, NativeImage mask, NativeImage tinted) implements AutoCloseable {
        private void applyTint(int baseColor, int selectedColor) {
            tint(source, mask, tinted, baseColor, selectedColor);
        }

        @Override public void close() {
            source.close();
            if (mask != null) mask.close();
            tinted.close();
        }
    }

    private static final class ManagedTexture {
        private final Identifier id;
        private final DynamicTexture texture;
        private final NativeImage base;
        private final NativeImage mask;
        private final int baseColor;
        private final List<AnimatedRegion> regions;
        private long lastTick = Long.MIN_VALUE;

        private ManagedTexture(Identifier id, DynamicTexture texture, NativeImage base, NativeImage mask,
                               int baseColor, List<AnimatedRegion> regions) {
            this.id = id;
            this.texture = texture;
            this.base = base;
            this.mask = mask;
            this.baseColor = baseColor;
            this.regions = List.copyOf(regions);
        }

        private void applyTint(int selectedColor) {
            NativeImage target = texture.getPixels();
            if (target == null) return;
            tint(base, mask, target, baseColor, selectedColor);
            for (AnimatedRegion region : regions) region.applyTint(baseColor, selectedColor);
            texture.upload();
        }

        private void closeAndRelease() {
            Minecraft.getInstance().getTextureManager().release(id);
            base.close();
            if (mask != null) mask.close();
            for (AnimatedRegion region : regions) region.close();
        }
    }
}
