package io.github.brandonitaly.bedrockskins.pack.persona;

import com.google.gson.JsonObject;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Allocates cosmetic textures and animation frames within a converted Persona atlas. */
final class PersonaAtlasPacker {
    private final BufferedImage image;
    private final BufferedImage tintMask;
    private final int atlasSize;
    private final Map<TileKey, Box> tiles = new HashMap<>();
    final List<AnimationTile> animations = new ArrayList<>();
    final List<JsonObject> extraBones = new ArrayList<>();
    private int x;
    private int y;
    private int rowHeight;
    private int usedWidth;
    private int usedHeight;
    int nextCubeId;
    boolean hasTint;

    PersonaAtlasPacker(BufferedImage image, BufferedImage tintMask, int x, int y) {
        this.image = image;
        this.tintMask = tintMask;
        this.atlasSize = Math.min(image.getWidth(), image.getHeight());
        this.x = x;
        this.y = y;
    }

    void include(int width, int height) {
        usedWidth = Math.max(usedWidth, width);
        usedHeight = Math.max(usedHeight, height);
    }

    int usedWidth() {
        return powerOfTwo(Math.max(1, usedWidth));
    }

    int usedHeight() {
        return powerOfTwo(Math.max(1, usedHeight));
    }

    Box tile(BufferedImage source, BufferedImage sourceTintMask, int frameHeight, int frameCount) {
        TileKey key = new TileKey(source, sourceTintMask, frameHeight, frameCount);
        return tiles.computeIfAbsent(key, ignored -> {
            int height = frameHeight > 0 ? frameHeight : source.getHeight();
            Box box = allocate(source.getWidth(), height);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Src);
                graphics.drawImage(source, box.x, box.y, box.x + source.getWidth(), box.y + height,
                    0, 0, source.getWidth(), height, null);
            } finally {
                graphics.dispose();
            }
            if (sourceTintMask != null) {
                hasTint = true;
                Graphics2D maskGraphics = tintMask.createGraphics();
                try {
                    maskGraphics.setComposite(AlphaComposite.Src);
                    maskGraphics.drawImage(sourceTintMask, box.x, box.y, box.x + source.getWidth(), box.y + height,
                        0, 0, sourceTintMask.getWidth(), Math.min(height, sourceTintMask.getHeight()), null);
                } finally {
                    maskGraphics.dispose();
                }
            }
            if (frameCount > 1) animations.add(new AnimationTile(source, sourceTintMask, box, height, frameCount));
            return box;
        });
    }

    private Box allocate(int width, int height) {
        if (x + width > atlasSize) {
            x = 0;
            y += rowHeight + 1;
            rowHeight = 0;
        }
        if (y + height > atlasSize) {
            throw new IllegalArgumentException("Persona texture exceeds converted atlas capacity");
        }
        Box result = new Box(x, y);
        include(x + width, y + height);
        x += width + 1;
        rowHeight = Math.max(rowHeight, height);
        return result;
    }

    private int powerOfTwo(int value) {
        int result = 1;
        while (result < value && result < atlasSize) result <<= 1;
        return Math.min(result, atlasSize);
    }

    record Box(int x, int y) {
    }

    private record TileKey(BufferedImage source, BufferedImage tintMask, int frameHeight, int frameCount) {
    }

    record AnimationTile(BufferedImage source, BufferedImage tintMask, Box box, int frameHeight, int frameCount) {
    }
}
