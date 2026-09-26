package io.github.brandonitaly.bedrockskins.client.render.model;

import java.util.*;
import java.util.function.IntBinaryOperator;

/** Orthographic, unlit projection with per-pixel depth ordering and alpha compositing. */
public final class HeadIconRasterizer {
    public record Vertex(float x, float y, float z, float u, float v) {}
    public record Triangle(Vertex a, Vertex b, Vertex c) {}
    public record Crop(float minX, float minY, float maxX, float maxY) {}
    private record Fragment(float depth, int color) {}
    private HeadIconRasterizer() {}

    public static int[] render(List<Triangle> triangles, int size, int width, int height, IntBinaryOperator texture) {
        return render(triangles, size, width, height, texture, null);
    }

    public static int[] render(List<Triangle> triangles, int size, int width, int height, IntBinaryOperator texture, Crop crop) {
        int[] pixels = new int[size * size];
        if (triangles.isEmpty()) return pixels;
        float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = -minX, maxY = -minX;
        for (var triangle : triangles) for (var v : List.of(triangle.a, triangle.b, triangle.c)) {
            minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x);
            minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
        }
        if (crop != null) {
            minX = crop.minX; minY = crop.minY; maxX = crop.maxX; maxY = crop.maxY;
        }
        float span = Math.max(maxX - minX, maxY - minY);
        if (!Float.isFinite(span) || span <= 0) return pixels;
        float scale = size / span, cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
        @SuppressWarnings("unchecked") List<Fragment>[] fragments = (List<Fragment>[]) new List<?>[pixels.length];
        for (var t : triangles) {
            Vertex a = project(t.a, scale, cx, cy, size), b = project(t.b, scale, cx, cy, size), c = project(t.c, scale, cx, cy, size);
            float area = edge(a, b, c.x, c.y);
            if (Math.abs(area) < 0.00001f) continue;
            int x0 = Math.max(0, (int)Math.floor(Math.min(a.x, Math.min(b.x, c.x))));
            int y0 = Math.max(0, (int)Math.floor(Math.min(a.y, Math.min(b.y, c.y))));
            int x1 = Math.min(size - 1, (int)Math.ceil(Math.max(a.x, Math.max(b.x, c.x))));
            int y1 = Math.min(size - 1, (int)Math.ceil(Math.max(a.y, Math.max(b.y, c.y))));
            for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                float wa = edge(b, c, x + .5f, y + .5f) / area;
                float wb = edge(c, a, x + .5f, y + .5f) / area, wc = 1 - wa - wb;
                if (wa < 0 || wb < 0 || wc < 0) continue;
                int u = Math.clamp((int)((a.u * wa + b.u * wb + c.u * wc) * width), 0, width - 1);
                int v = Math.clamp((int)((a.v * wa + b.v * wb + c.v * wc) * height), 0, height - 1);
                int color = texture.applyAsInt(u, v);
                if ((color >>> 24) == 0) continue;
                int index = y * size + x;
                if (fragments[index] == null) fragments[index] = new ArrayList<>();
                float depth = a.z * wa + b.z * wb + c.z * wc;
                // Shared triangle edges must not blend the same translucent surface twice.
                if (fragments[index].stream().noneMatch(f -> Math.abs(f.depth - depth) < .00001f && f.color == color))
                    fragments[index].add(new Fragment(depth, color));
            }
        }
        for (int i = 0; i < pixels.length; i++) if (fragments[i] != null) {
            fragments[i].sort(Comparator.comparingDouble(Fragment::depth).reversed());
            for (var fragment : fragments[i]) pixels[i] = over(fragment.color, pixels[i]);
        }
        return pixels;
    }
    private static Vertex project(Vertex v, float scale, float cx, float cy, int size) {
        return new Vertex((v.x - cx) * scale + size / 2f, (v.y - cy) * scale + size / 2f, v.z, v.u, v.v);
    }
    private static float edge(Vertex a, Vertex b, float x, float y) { return (b.x-a.x)*(y-a.y)-(b.y-a.y)*(x-a.x); }
    private static int over(int front, int back) {
        float a = (front >>> 24) / 255f, b = (back >>> 24) / 255f * (1-a), total = a+b;
        if (total == 0) return 0;
        int result = Math.round(total*255) << 24;
        for (int shift : new int[]{16,8,0}) result |= Math.round((((front >>> shift)&255)*a + ((back >>> shift)&255)*b)/total) << shift;
        return result;
    }
}
