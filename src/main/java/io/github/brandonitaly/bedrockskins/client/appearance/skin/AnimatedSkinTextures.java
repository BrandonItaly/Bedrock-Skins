package io.github.brandonitaly.bedrockskins.client.appearance.skin;

import io.github.brandonitaly.bedrockskins.pack.persona.PersonaImages;
import io.github.brandonitaly.bedrockskins.pack.persona.BlinkAnimation;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaTexturePayload;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaAnimationClock;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Remote skin strips share the lifetime of their registered skin texture. Client thread only. */
public final class AnimatedSkinTextures {
    private record Region(PersonaTexturePayload.AnimationRegion layout, BufferedImage strip) {}
    private record Animation(DynamicTexture texture, List<Region> regions, BlinkAnimation blink, int[] frames) {
        private Animation(DynamicTexture texture, List<Region> regions) {
            this(texture, List.copyOf(regions), new BlinkAnimation(), new int[regions.size()]);
            Arrays.fill(frames, -1);
        }
    }
    private static final Map<Identifier, Animation> ANIMATIONS = new HashMap<>();
    private static long lastFrame = Long.MIN_VALUE;
    private AnimatedSkinTextures() {}

    public static void register(Identifier id, DynamicTexture texture, PersonaTexturePayload payload) throws IOException {
        List<Region> regions = new ArrayList<>();
        var target = texture.getPixels();
        if (target == null) throw new IOException("Missing remote skin texture");
        for (var layout : payload.animations()) {
            var strip = PersonaImages.read(layout.textureStrip());
            if ((long) layout.atlasX() + layout.width() > target.getWidth()
                || (long) layout.atlasY() + layout.frameHeight() > target.getHeight()
                || strip.getWidth() != layout.width()
                || strip.getHeight() != (long) layout.frameHeight() * layout.frameCount()) {
                throw new IOException("Remote skin animation exceeds texture bounds");
            }
            regions.add(new Region(layout, strip));
        }
        if (!regions.isEmpty()) {
            Animation animation = new Animation(texture, regions);
            apply(animation, System.nanoTime());
            ANIMATIONS.put(id, animation);
        }
    }

    public static void remove(Identifier id) { ANIMATIONS.remove(id); }
    public static void clear() { ANIMATIONS.clear(); }

    public static void tick() {
        long now = System.nanoTime();
        long frame = now / 50_000_000L;
        if (lastFrame == frame) return;
        lastFrame = frame;
        ANIMATIONS.values().forEach(animation -> apply(animation, now));
    }

    private static void apply(Animation animation, long now) {
        var pixels = animation.texture.getPixels();
        if (pixels == null) return;
        boolean changed = false;
        long linearFrame = PersonaAnimationClock.frame(now);
        for (int i = 0; i < animation.regions.size(); i++) {
            var layout = animation.regions.get(i).layout;
            int frame = layout.expression() == 1 ? animation.blink.frame(now, layout.frameCount())
                : layout.frame(linearFrame);
            changed |= animation.frames[i] != frame;
            animation.frames[i] = frame;
        }
        if (!changed) return;
        // Preserve region order, including overlapping regions, whenever the atlas changes.
        for (int i = 0; i < animation.regions.size(); i++) {
            Region region = animation.regions.get(i);
            var layout = region.layout;
            int sourceY = animation.frames[i] * layout.frameHeight();
            for (int y = 0; y < layout.frameHeight(); y++) for (int x = 0; x < layout.width(); x++) {
                pixels.setPixel(layout.atlasX() + x, layout.atlasY() + y, region.strip.getRGB(x, sourceY + y));
            }
        }
        animation.texture.upload();
    }
}
