package com.brandonitaly.bedrockskins.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

public record LocalSkinConfig(List<String> favorites, String selected, String selectedCape) {
    public static final LocalSkinConfig DEFAULT = new LocalSkinConfig(List.of(), null, null);

    public static final Codec<LocalSkinConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(Codec.STRING).optionalFieldOf("favorites", List.of()).forGetter(LocalSkinConfig::favorites),
        Codec.STRING.optionalFieldOf("selected").forGetter(state -> Optional.ofNullable(state.selected())),
        Codec.STRING.optionalFieldOf("selectedCape").forGetter(state -> Optional.ofNullable(state.selectedCape()))
    ).apply(instance, (favorites, selected, selectedCape) -> new LocalSkinConfig(favorites, selected.orElse(null), selectedCape.orElse(null))));
}