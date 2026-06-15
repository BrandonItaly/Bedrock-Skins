package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;

import java.util.Objects;

public record PlayerSkinData(SkinId skinId, String geometry, byte[] textureData) {
    public PlayerSkinData {
        geometry = Objects.requireNonNullElse(geometry, "");
        textureData = Objects.requireNonNullElseGet(textureData, () -> new byte[0]);
    }
}