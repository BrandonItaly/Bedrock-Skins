package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;

import java.util.Objects;

public record PlayerSkinData(SkinId skinId, String geometry, byte[] textureData, byte[] capeData) {
    public PlayerSkinData {
        geometry = Objects.requireNonNullElse(geometry, "");
        textureData = Objects.requireNonNullElseGet(textureData, () -> new byte[0]);
        capeData = Objects.requireNonNullElseGet(capeData, () -> new byte[0]);
    }
}