package io.github.brandonitaly.bedrockskins.pack.model;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.Set;

/** A wearable Persona piece, kept separate from the player's selected skin. */
public final class LoadedCosmetic {
    public final String id;
    public final String displayName;
    public final String type;
    public final Set<String> zones;
    public final JsonObject geometryData;
    public final JsonObject slimGeometryData;
    public final AssetSource texture;
    public final boolean tintable;
    public final int defaultTintColor;
    public Identifier textureIdentifier;

    public LoadedCosmetic(String id, String displayName, String type, Set<String> zones,
                          JsonObject geometryData, AssetSource texture) {
        this(id, displayName, type, zones, geometryData, geometryData, texture);
    }

    public LoadedCosmetic(String id, String displayName, String type, Set<String> zones,
                          JsonObject geometryData, JsonObject slimGeometryData, AssetSource texture) {
        this(id, displayName, type, zones, geometryData, slimGeometryData, texture, false, 0xFFFFFF);
    }

    public LoadedCosmetic(String id, String displayName, String type, Set<String> zones,
                          JsonObject geometryData, JsonObject slimGeometryData, AssetSource texture,
                          boolean tintable, int defaultTintColor) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.zones = zones == null ? Set.of() : Set.copyOf(zones);
        this.geometryData = geometryData;
        this.slimGeometryData = slimGeometryData == null ? geometryData : slimGeometryData;
        this.texture = texture;
        this.tintable = tintable;
        this.defaultTintColor = defaultTintColor & 0xFFFFFF;
    }
}
