package io.github.brandonitaly.bedrockskins.client.appearance.persona;

import io.github.brandonitaly.bedrockskins.pack.model.AssetSource;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaTexturePayload;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaAnimationClock;
import io.github.brandonitaly.bedrockskins.pack.persona.BlinkAnimation;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Owns independently tinted texture variants and animation frames for Persona pieces. */
final class PersonaTextureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<VariantKey, ManagedTexture> TEXTURES = new ConcurrentHashMap<>();

    private PersonaTextureManager() {}

    static void register(LoadedCosmetic cosmetic) {
        if (!(cosmetic.texture instanceof AssetSource.Memory)) return;
        cosmetic.textureIdentifier = textureId(cosmetic, cosmetic.defaultTintColor);
    }

    static Identifier texture(LoadedCosmetic cosmetic, int color) {
        if (cosmetic == null) return null;
        int effectiveColor = cosmetic.tintable ? color & 0xFFFFFF : cosmetic.defaultTintColor;
        VariantKey key = new VariantKey(cosmetic.id, effectiveColor);
        ManagedTexture existing = TEXTURES.get(key);
        if (existing != null) return existing.id;
        if (!(cosmetic.texture instanceof AssetSource.Memory memory)) return cosmetic.textureIdentifier;
        ManagedTexture created = create(cosmetic, key, memory.data());
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
        long now = System.nanoTime();
        long animationTick = now / 50_000_000L;
        if (animation.lastTick == animationTick) return;
        animation.lastTick = animationTick;
        NativeImage target = animation.texture.getPixels();
        if (target == null) return;
        boolean changed = false;
        long linearFrame = PersonaAnimationClock.frame(now);
        for (int i = 0; i < animation.regions.size(); i++) {
            AnimatedRegion region = animation.regions.get(i);
            int frame = region.expression == 1 ? animation.blink.frame(now, region.frameCount)
                : (int) Math.floorMod(linearFrame, region.frameCount);
            changed |= animation.frames[i] != frame;
            animation.frames[i] = frame;
        }
        if (!changed) return;
        // Reapply every region in order so overlapping strips retain the same result.
        for (int i = 0; i < animation.regions.size(); i++) {
            AnimatedRegion region = animation.regions.get(i);
            int sourceY = animation.frames[i] * region.frameHeight;
            if (sourceY + region.frameHeight > region.tinted.getHeight()) continue;
            region.tinted.copyRect(target, 0, sourceY,
                region.x, region.y, region.width, region.frameHeight, false, false);
        }
        animation.texture.upload();
    }

    static void remove(String cosmeticId) {
        List<VariantKey> keys = TEXTURES.keySet().stream().filter(key -> key.cosmeticId.equals(cosmeticId)).toList();
        for (VariantKey key : keys) {
            ManagedTexture texture = TEXTURES.remove(key);
            if (texture != null) texture.closeAndRelease();
        }
    }

    static void clear() {
        TEXTURES.values().forEach(ManagedTexture::closeAndRelease);
        TEXTURES.clear();
    }

    private static ManagedTexture create(LoadedCosmetic cosmetic, VariantKey key, byte[] data) {
        try {
            PersonaTexturePayload payload = PersonaTexturePayload.decode(data);
            NativeImage base = read(payload.baseTexture());
            NativeImage target = new NativeImage(base.getWidth(), base.getHeight(), false);
            NativeImage mask = payload.tintMask().length == 0 ? null : read(payload.tintMask());
            if (mask != null && (mask.getWidth() != base.getWidth() || mask.getHeight() != base.getHeight())) {
                mask.close();
                mask = null;
            }
            Identifier id = textureId(cosmetic, key.color);
            DynamicTexture texture = new DynamicTexture(() -> "persona_cosmetic", target);
            Minecraft.getInstance().getTextureManager().register(id, texture);

            List<AnimatedRegion> regions = new ArrayList<>();
            for (PersonaTexturePayload.AnimationRegion region : payload.animations()) {
                NativeImage source = read(region.textureStrip());
                NativeImage tinted = new NativeImage(source.getWidth(), source.getHeight(), false);
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
                    region.frameHeight(), region.frameCount(), region.expression(), source, stripMask, tinted));
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
        float[] baseHsb = io.github.brandonitaly.bedrockskins.pack.persona.PersonaColors.hsv(baseColor);
        float[] selectedHsb = io.github.brandonitaly.bedrockskins.pack.persona.PersonaColors.hsv(selectedColor);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int pixel = source.getPixel(x, y);
            int amount = mask == null ? 0 : (mask.getPixel(x, y) >> 16) & 0xFF;
            target.setPixel(x, y, io.github.brandonitaly.bedrockskins.pack.persona.PersonaColors.tint(pixel, amount, baseHsb, selectedHsb));
        }
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    private static Identifier textureId(LoadedCosmetic cosmetic, int color) {
        return Identifier.fromNamespaceAndPath("bedrockskins",
            "persona/" + sanitize(cosmetic.id) + "/" + String.format("%06x", color & 0xFFFFFF));
    }

    private record VariantKey(String cosmeticId, int color) {}

    private record AnimatedRegion(int x, int y, int width, int frameHeight, int frameCount, int expression,
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
        private final int[] frames;
        private long lastTick = Long.MIN_VALUE;
        private final BlinkAnimation blink = new BlinkAnimation();

        private ManagedTexture(Identifier id, DynamicTexture texture, NativeImage base, NativeImage mask,
                               int baseColor, List<AnimatedRegion> regions) {
            this.id = id;
            this.texture = texture;
            this.base = base;
            this.mask = mask;
            this.baseColor = baseColor;
            this.regions = List.copyOf(regions);
            this.frames = new int[regions.size()];
            Arrays.fill(this.frames, -1);
        }

        private void applyTint(int selectedColor) {
            NativeImage target = texture.getPixels();
            if (target == null) return;
            tint(base, mask, target, baseColor, selectedColor);
            for (AnimatedRegion region : regions) region.applyTint(baseColor, selectedColor);
            Arrays.fill(frames, -1);
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
