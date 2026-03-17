package com.brandonitaly.bedrockskins.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record LocalSkinConfig(List<String> favorites, String selected) {
    public static final LocalSkinConfig DEFAULT = new LocalSkinConfig(Collections.emptyList(), null);

    public static final Codec<LocalSkinConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(Codec.STRING).optionalFieldOf("favorites", Collections.emptyList()).forGetter(LocalSkinConfig::favorites),
        Codec.STRING.optionalFieldOf("selected").forGetter(state -> Optional.ofNullable(state.selected()))
    ).apply(instance, (favorites, selected) -> new LocalSkinConfig(favorites, selected.orElse(null))));
}