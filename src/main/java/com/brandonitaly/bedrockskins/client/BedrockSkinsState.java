package com.brandonitaly.bedrockskins.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BedrockSkinsState {
    public static final BedrockSkinsState DEFAULT = new BedrockSkinsState(Collections.emptyList(), null);

    public static final Codec<BedrockSkinsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(Codec.STRING).optionalFieldOf("favorites", Collections.emptyList()).forGetter(BedrockSkinsState::getFavorites),
        Codec.STRING.optionalFieldOf("selected").forGetter(state -> Optional.ofNullable(state.getSelected()))
    ).apply(instance, (favorites, selected) -> new BedrockSkinsState(favorites, selected.orElse(null))));

    private List<String> favorites;
    private String selected;

    public BedrockSkinsState() {
        this(Collections.emptyList(), null);
    }

    public BedrockSkinsState(List<String> favorites, String selected) {
        this.favorites = favorites == null ? new ArrayList<>() : new ArrayList<>(favorites);
        this.selected = selected;
    }

    public List<String> getFavorites() { return favorites == null ? Collections.emptyList() : favorites; }
    public void setFavorites(List<String> favorites) { this.favorites = favorites == null ? new ArrayList<>() : new ArrayList<>(favorites); }

    public String getSelected() { return selected; }
    public void setSelected(String selected) { this.selected = selected; }
}
