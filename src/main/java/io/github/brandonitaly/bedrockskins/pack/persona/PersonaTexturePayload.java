package io.github.brandonitaly.bedrockskins.pack.persona;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Compact transport for a Persona atlas plus the source strips copied into it at runtime. */
public record PersonaTexturePayload(byte[] baseTexture, byte[] tintMask, int tintBaseColor,
                                    List<AnimationRegion> animations) {
    private static final int MAGIC = 0x42535041; // BSPA
    private static final int VERSION = 3;
    private static final int MAX_REGIONS = 32;
    private static final int MAX_IMAGE_BYTES = 1_048_576;

    public PersonaTexturePayload {
        baseTexture = baseTexture == null ? new byte[0] : baseTexture;
        tintMask = tintMask == null ? new byte[0] : tintMask;
        tintBaseColor &= 0xFFFFFF;
        animations = animations == null ? List.of() : List.copyOf(animations);
    }

    public PersonaTexturePayload(byte[] baseTexture, List<AnimationRegion> animations) {
        this(baseTexture, new byte[0], 0xFFFFFF, animations);
    }

    public static byte[] encode(byte[] baseTexture, List<AnimationRegion> animations) throws IOException {
        return encode(baseTexture, new byte[0], 0xFFFFFF, animations);
    }

    public static byte[] encode(byte[] baseTexture, byte[] tintMask, int tintBaseColor,
                                List<AnimationRegion> animations) throws IOException {
        List<AnimationRegion> safeAnimations = animations == null ? List.of() : animations;
        if (safeAnimations.size() > MAX_REGIONS) throw new IOException("Too many Persona animation regions");
        if (safeAnimations.isEmpty() && (tintMask == null || tintMask.length == 0)) return baseTexture;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeByte(VERSION);
            writeBytes(output, baseTexture);
            writeBytes(output, tintMask);
            output.writeInt(tintBaseColor & 0xFFFFFF);
            output.writeInt(safeAnimations.size());
            for (AnimationRegion region : safeAnimations) {
                output.writeInt(region.atlasX());
                output.writeInt(region.atlasY());
                output.writeInt(region.width());
                output.writeInt(region.frameHeight());
                output.writeInt(region.frameCount());
                writeBytes(output, region.textureStrip());
                writeBytes(output, region.tintMaskStrip());
                output.writeInt(region.expression());
            }
        }
        return bytes.toByteArray();
    }

    public static PersonaTexturePayload decode(byte[] data) throws IOException {
        if (data == null || data.length < 5) return new PersonaTexturePayload(data, List.of());
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            if (input.readInt() != MAGIC) return new PersonaTexturePayload(data, List.of());
            int version = input.readUnsignedByte();
            if (version < 1 || version > VERSION) throw new IOException("Unsupported Persona texture animation version " + version);
            byte[] base = readBytes(input);
            byte[] tintMask = version >= 2 ? readBytes(input) : new byte[0];
            int tintBaseColor = version >= 2 ? input.readInt() : 0xFFFFFF;
            int count = input.readInt();
            if (count < 0 || count > MAX_REGIONS) throw new IOException("Invalid Persona animation region count " + count);
            List<AnimationRegion> regions = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int x = input.readInt();
                int y = input.readInt();
                int width = input.readInt();
                int height = input.readInt();
                int frames = input.readInt();
                if (x < 0 || y < 0 || width <= 0 || height <= 0 || frames < 2 || frames > 256) {
                    throw new IOException("Invalid Persona animation region");
                }
                byte[] strip = readBytes(input);
                byte[] stripMask = version >= 2 ? readBytes(input) : new byte[0];
                int expression = version >= 3 ? input.readInt() : 0;
                if (expression < 0 || expression > 1) throw new IOException("Invalid animation expression");
                regions.add(new AnimationRegion(x, y, width, height, frames, strip, stripMask, expression));
            }
            return new PersonaTexturePayload(base, tintMask, tintBaseColor, regions);
        }
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        byte[] safe = value == null ? new byte[0] : value;
        if (safe.length > MAX_IMAGE_BYTES) throw new IOException("Persona texture data too large");
        output.writeInt(safe.length);
        output.write(safe);
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_IMAGE_BYTES) throw new IOException("Invalid Persona texture data length " + length);
        byte[] result = input.readNBytes(length);
        if (result.length != length) throw new IOException("Truncated Persona texture data");
        return result;
    }

    public record AnimationRegion(int atlasX, int atlasY, int width, int frameHeight,
                                  int frameCount, byte[] textureStrip, byte[] tintMaskStrip, int expression) {
        public AnimationRegion(int atlasX, int atlasY, int width, int frameHeight, int frameCount, byte[] textureStrip, byte[] tintMaskStrip) {
            this(atlasX, atlasY, width, frameHeight, frameCount, textureStrip, tintMaskStrip, 0);
        }
        public int frame(long tick) {
            if (expression == 0) return (int) Math.floorMod(tick, frameCount);
            // Keep the neutral frame between blinks; Bedrock chooses its blink timing locally.
            int phase = (int) Math.floorMod(tick, frameCount + 26);
            return phase < 26 ? 0 : phase - 26;
        }
        public AnimationRegion(int atlasX, int atlasY, int width, int frameHeight,
                               int frameCount, byte[] textureStrip) {
            this(atlasX, atlasY, width, frameHeight, frameCount, textureStrip, new byte[0]);
        }
    }
}
