package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;
import java.util.Arrays;
import java.util.Objects;

public class PlayerSkinData {
    public final SkinId skinId;
    public final String geometry;
    public final byte[] textureData;

    public PlayerSkinData(SkinId skinId, String geometry, byte[] textureData) {
        this.skinId = skinId;
        this.geometry = geometry;
        this.textureData = textureData == null ? new byte[0] : textureData;
    }

    public SkinId getSkinId() { return skinId; }
    public String getGeometry() { return geometry; }
    public byte[] getTextureData() { return textureData; }

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
