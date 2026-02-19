package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.Objects;

public final class PlayerSkinData {
    private final SkinId skinId;
    private final String geometry;
    private final byte[] textureData;
    private final JsonObject geometryJson;

    public PlayerSkinData(SkinId skinId, String geometry, byte[] textureData) {
        this.skinId = skinId;
        this.geometry = geometry == null ? "" : geometry;
        this.textureData = textureData == null ? new byte[0] : textureData;
        this.geometryJson = parseGeometry(this.geometry);
    }

    public SkinId getSkinId() {
        return skinId;
    }

    public String getGeometry() {
        return geometry;
    }

    public JsonObject getGeometryJson() {
        return geometryJson;
    }

    public byte[] getTextureData() {
        return textureData;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerSkinData that = (PlayerSkinData) o;
        if (!Objects.equals(skinId, that.skinId)) return false;
        if (!Objects.equals(geometry, that.geometry)) return false;
        return Arrays.equals(textureData, that.textureData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(skinId, geometry);
        result = 31 * result + Arrays.hashCode(textureData);
        return result;
    }
}
