package io.github.brandonitaly.bedrockskins.client.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record LocalSkinConfig(List<String> favorites, String selected, String selectedCape,
                              List<String> selectedCosmetics, String selectedEmote,
                              List<String> emoteSlots, Map<String, Integer> personaColors) {
    public static final LocalSkinConfig DEFAULT = new LocalSkinConfig(
        List.of(), null, null, List.of(), null, List.of(), Map.of());

    public static final Codec<LocalSkinConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(Codec.STRING).optionalFieldOf("favorites", List.of()).forGetter(LocalSkinConfig::favorites),
        Codec.STRING.optionalFieldOf("selected").forGetter(state -> Optional.ofNullable(state.selected())),
        Codec.STRING.optionalFieldOf("selectedCape").forGetter(state -> Optional.ofNullable(state.selectedCape())),
        Codec.list(Codec.STRING).optionalFieldOf("selectedCosmetics", List.of()).forGetter(LocalSkinConfig::selectedCosmetics),
        Codec.STRING.optionalFieldOf("selectedEmote").forGetter(state -> Optional.ofNullable(state.selectedEmote())),
        Codec.list(Codec.STRING).optionalFieldOf("emoteSlots", List.of()).forGetter(LocalSkinConfig::emoteSlots),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("personaColors", Map.of()).forGetter(LocalSkinConfig::personaColors)
    ).apply(instance, (favorites, selected, selectedCape, cosmetics, emote, emoteSlots, personaColors) -> new LocalSkinConfig(
        favorites, selected.orElse(null), selectedCape.orElse(null), cosmetics, emote.orElse(null), emoteSlots,
        Map.copyOf(personaColors))));

    public LocalSkinConfig withFavorites(List<String> newFavorites) {
        return new LocalSkinConfig(List.copyOf(newFavorites), selected, selectedCape, selectedCosmetics, selectedEmote, emoteSlots, personaColors);
    }

    public LocalSkinConfig withSelectedSkin(String newSelected) {
        return new LocalSkinConfig(favorites, newSelected, selectedCape, selectedCosmetics, selectedEmote, emoteSlots, personaColors);
    }

    public LocalSkinConfig withSelection(String newSelected, String newSelectedCape) {
        return new LocalSkinConfig(favorites, newSelected, newSelectedCape, selectedCosmetics, selectedEmote, emoteSlots, personaColors);
    }

    public LocalSkinConfig withSelectedCosmetics(List<String> cosmetics) {
        return new LocalSkinConfig(favorites, selected, selectedCape, List.copyOf(cosmetics), selectedEmote, emoteSlots, personaColors);
    }

    public LocalSkinConfig withSelectedEmote(String emote) {
        return new LocalSkinConfig(favorites, selected, selectedCape, selectedCosmetics, emote, emoteSlots, personaColors);
    }

    public LocalSkinConfig withEmoteSlots(List<String> slots) {
        return new LocalSkinConfig(favorites, selected, selectedCape, selectedCosmetics, selectedEmote, List.copyOf(slots), personaColors);
    }

    public LocalSkinConfig withPersonaColors(Map<String, Integer> colors) {
        return new LocalSkinConfig(favorites, selected, selectedCape, selectedCosmetics, selectedEmote, emoteSlots,
            Map.copyOf(colors));
    }
}
