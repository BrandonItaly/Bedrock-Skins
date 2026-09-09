package io.github.brandonitaly.bedrockskins.client.appearance.persona;

import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One coherent snapshot of equipped Persona pieces and their per-piece customization. */
final class PersonaLoadout {
    private final Map<String, String> pieces;
    private final Map<String, Integer> colors;
    private final Map<String, PersonaManager.EquipSide> sides;

    private PersonaLoadout(Map<String, String> pieces, Map<String, Integer> colors,
                           Map<String, PersonaManager.EquipSide> sides) {
        this.pieces = Collections.unmodifiableMap(new LinkedHashMap<>(pieces));
        this.colors = Map.copyOf(colors);
        this.sides = Map.copyOf(sides);
    }

    static PersonaLoadout of(Map<String, String> pieces, Map<String, Integer> colors,
                             Map<String, PersonaManager.EquipSide> sides) {
        if (pieces == null || pieces.isEmpty()) return null;
        return new PersonaLoadout(pieces, colors == null ? Map.of() : colors, sides == null ? Map.of() : sides);
    }

    Map<String, String> pieces() { return pieces; }
    int color(LoadedCosmetic cosmetic) { return colors.getOrDefault(cosmetic.id, cosmetic.defaultTintColor); }
    PersonaManager.EquipSide side(LoadedCosmetic cosmetic) {
        return sides.getOrDefault(cosmetic.id, PersonaManager.EquipSide.BOTH);
    }

    PersonaLoadout withColor(String cosmeticId, int color) {
        Map<String, Integer> updated = new LinkedHashMap<>(colors);
        updated.put(cosmeticId, color);
        return new PersonaLoadout(pieces, updated, sides);
    }
}
