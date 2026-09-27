package io.github.brandonitaly.bedrockskins.pack.persona;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/** Local blink state using Mojang's persona blink timing ranges. */
public final class BlinkAnimation {
    private final DoubleSupplier random;
    private double lastBlink = Double.NaN;
    private double start = Double.NaN;
    private double duration;
    private long lastEvaluation = Long.MIN_VALUE;

    public BlinkAnimation() { this(() -> ThreadLocalRandom.current().nextDouble()); }
    public BlinkAnimation(DoubleSupplier random) { this.random = random; }

    public int frame(long nanos, int frames) {
        double now = nanos / 1_000_000_000.0;
        if (Double.isNaN(lastBlink)) lastBlink = now;
        if (!Double.isNaN(start)) {
            if (now < start + duration) {
                return Math.min(frames - 1, 1 + (int) ((now - start) / duration * (frames - 1)));
            }
            start = Double.NaN;
            lastBlink = now;
        }
        // Evaluate once per client tick, independent of how often a texture is drawn.
        long evaluation = Math.floorDiv(nanos, 50_000_000L);
        if (lastEvaluation == evaluation || frames <= 1) return 0;
        lastEvaluation = evaluation;
        double window = random.getAsDouble() * 0.2;
        if (now > lastBlink + 3 + random.getAsDouble() * 37) {
            start = now;
            // One client tick is the shortest blink Java can display.
            duration = Math.max(0.05, window);
            return 1;
        }
        return 0;
    }
}
