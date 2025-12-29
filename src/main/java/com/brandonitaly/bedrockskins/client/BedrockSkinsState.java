package com.brandonitaly.bedrockskins.client;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BedrockSkinsState {
    @SerializedName("favorites")
    private List<String> favorites;

    @SerializedName("selected")
    private String selected;

    public BedrockSkinsState() {}
    public BedrockSkinsState(List<String> favorites, String selected) {
        this.favorites = favorites;
        this.selected = selected;
    }

    public List<String> getFavorites() { return favorites; }
    public void setFavorites(List<String> favorites) { this.favorites = favorites; }

    public String getSelected() { return selected; }
    public void setSelected(String selected) { this.selected = selected; }
}
