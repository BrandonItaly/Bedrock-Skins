package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public record PlayerSkinData(SkinId skinId, String geometry, byte[] textureData, JsonObject geometryJson) {
    public PlayerSkinData(SkinId skinId, String geometry, byte[] textureData) {
        this(
            skinId,
            geometry == null ? "" : geometry,
            textureData == null ? new byte[0] : textureData,
            parseGeometry(geometry == null ? "" : geometry)
        );
    }

    private static JsonObject parseGeometry(String geometry) {
        try {
            var element = JsonParser.parseString(geometry);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Geometry must be a JSON object");
            }
            return element.getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid geometry JSON", e);
        }
    }

    // Backwards-compatible getters
    public SkinId getSkinId() { return skinId; }
    public String getGeometry() { return geometry; }
}