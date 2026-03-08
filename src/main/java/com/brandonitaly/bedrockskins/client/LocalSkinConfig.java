package com.brandonitaly.bedrockskins.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LocalSkinConfig {
    public static final LocalSkinConfig DEFAULT = new LocalSkinConfig(Collections.emptyList(), null);

    public static final Codec<LocalSkinConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(Codec.STRING).optionalFieldOf("favorites", Collections.emptyList()).forGetter(LocalSkinConfig::getFavorites),
        Codec.STRING.optionalFieldOf("selected").forGetter(state -> Optional.ofNullable(state.getSelected()))
    ).apply(instance, (favorites, selected) -> new LocalSkinConfig(favorites, selected.orElse(null))));

    private final List<String> favorites;
    private String selected;

    public LocalSkinConfig(List<String> favorites, String selected) {
        this.favorites = favorites == null ? new ArrayList<>() : new ArrayList<>(favorites);
        this.selected = selected;
    }

    public List<String> getFavorites() { return favorites; }

    public String getSelected() { return selected; }
    public void setSelected(String selected) { this.selected = selected; }
}
