package io.github.brandonitaly.bedrockskins.pack.persona;

/** Linear Persona textures use floor(life_time * 7) in Mojang's persona.render_controllers.json. */
public final class PersonaAnimationClock {
    private PersonaAnimationClock() {}

    public static long frame(long nanos) {
        // Split seconds and remainder to avoid overflow and rounded frame-duration drift.
        return Math.floorDiv(nanos, 1_000_000_000L) * 7
            + Math.floorMod(nanos, 1_000_000_000L) * 7 / 1_000_000_000L;
    }
}
