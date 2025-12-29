package com.brandonitaly.bedrockskins;

import java.util.Arrays;
import java.util.Objects;

public class PlayerSkinData {
    public final String skinKey;
    public final String geometry;
    public final byte[] textureData;

    public PlayerSkinData(String skinKey, String geometry, byte[] textureData) {
        this.skinKey = skinKey;
        this.geometry = geometry;
        this.textureData = textureData == null ? new byte[0] : textureData;
    }

    public String getSkinKey() { return skinKey; }
    public String getGeometry() { return geometry; }
    public byte[] getTextureData() { return textureData; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerSkinData that = (PlayerSkinData) o;
        if (!Objects.equals(skinKey, that.skinKey)) return false;
        if (!Objects.equals(geometry, that.geometry)) return false;
        return Arrays.equals(textureData, that.textureData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(skinKey, geometry);
        result = 31 * result + Arrays.hashCode(textureData);
        return result;
    }
}
