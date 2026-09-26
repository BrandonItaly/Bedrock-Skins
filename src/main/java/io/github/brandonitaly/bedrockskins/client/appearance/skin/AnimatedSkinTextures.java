package io.github.brandonitaly.bedrockskins.client.appearance.skin;

import io.github.brandonitaly.bedrockskins.pack.persona.PersonaImages;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaTexturePayload;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Remote skin strips share the lifetime of their registered skin texture. Client thread only. */
public final class AnimatedSkinTextures {
    private record Region(PersonaTexturePayload.AnimationRegion layout, BufferedImage strip) {}
    private record Animation(DynamicTexture texture, List<Region> regions) {}
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
            Animation animation = new Animation(texture, List.copyOf(regions));
            apply(animation, System.nanoTime() / 115_000_000L);
            ANIMATIONS.put(id, animation);
        }
    }

    public static void remove(Identifier id) { ANIMATIONS.remove(id); }
    public static boolean isAnimated(Identifier id) { return ANIMATIONS.containsKey(id); }
    public static void clear() { ANIMATIONS.clear(); }

    public static void tick() {
        long frame = System.nanoTime() / 115_000_000L;
        if (lastFrame == frame) return;
        lastFrame = frame;
        ANIMATIONS.values().forEach(animation -> apply(animation, frame));
    }

    private static void apply(Animation animation, long frame) {
        var pixels = animation.texture.getPixels();
        if (pixels == null) return;
        for (Region region : animation.regions) {
            var layout = region.layout;
            int sourceY = layout.frame(frame) * layout.frameHeight();
            for (int y = 0; y < layout.frameHeight(); y++) for (int x = 0; x < layout.width(); x++) {
                pixels.setPixel(layout.atlasX() + x, layout.atlasY() + y, region.strip.getRGB(x, sourceY + y));
            }
        }
        animation.texture.upload();
    }
}
