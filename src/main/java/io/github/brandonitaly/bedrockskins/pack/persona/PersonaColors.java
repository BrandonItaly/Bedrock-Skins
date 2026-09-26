package io.github.brandonitaly.bedrockskins.pack.persona;

import java.awt.Color;

/** Same tint transfer on Java clients and in the server's Bedrock appearance compositor. */
public final class PersonaColors {
    private PersonaColors() {}

    public static int tint(int pixel, int amount, float[] base, float[] selected) {
        if (amount == 0) return pixel;
        int r = pixel >>> 16 & 255, g = pixel >>> 8 & 255, b = pixel & 255;
        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        float hue = (hsv[0] + selected[0] - base[0]) % 1;
        if (hue < 0) hue += 1;
        int shifted = Color.HSBtoRGB(hue, transfer(hsv[1], base[1], selected[1]), transfer(hsv[2], base[2], selected[2]));
        return (pixel & 0xFF000000) | blend(r, shifted >>> 16 & 255, amount) << 16
            | blend(g, shifted >>> 8 & 255, amount) << 8 | blend(b, shifted & 255, amount);
    }

    public static float[] hsv(int rgb) { return Color.RGBtoHSB(rgb >>> 16 & 255, rgb >>> 8 & 255, rgb & 255, null); }
    private static float transfer(float value, float base, float selected) { return base <= 1e-6f ? selected : Math.min(1, value * selected / base); }
    private static int blend(int from, int to, int amount) { return (from * (255 - amount) + to * amount + 127) / 255; }
}
