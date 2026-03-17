package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public record PlayerSkinData(SkinId skinId, String geometry, byte[] textureData) {
    public PlayerSkinData {
        if (geometry == null) geometry = "";
        if (textureData == null) textureData = new byte[0];
    }
}