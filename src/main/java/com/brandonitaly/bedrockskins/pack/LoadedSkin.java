package com.brandonitaly.bedrockskins.pack;

import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

public class LoadedSkin {
    public final String serializeName;
    public final String packDisplayName;
    public final String skinDisplayName;
    public final com.google.gson.JsonObject geometryData;
    public final AssetSource texture;
    public final AssetSource cape; // nullable

    public net.minecraft.util.Identifier identifier;
    public net.minecraft.util.Identifier capeIdentifier;

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, com.google.gson.JsonObject geometryData, AssetSource texture) {
        this(serializeName, packDisplayName, skinDisplayName, geometryData, texture, null);
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, com.google.gson.JsonObject geometryData, AssetSource texture, AssetSource cape) {
        this.serializeName = serializeName;
        this.packDisplayName = packDisplayName;
        this.skinDisplayName = skinDisplayName;
        this.geometryData = geometryData;
        this.texture = texture;
        this.cape = cape;
    }

    public String getSerializeName() { return serializeName; }
    public String getPackDisplayName() { return packDisplayName; }
    public String getSkinDisplayName() { return skinDisplayName; }
    public com.google.gson.JsonObject getGeometryData() { return geometryData; }
    public AssetSource getTexture() { return texture; }
    public AssetSource getCape() { return cape; }

    public String getKey() { return packDisplayName + ":" + skinDisplayName; }
    public String getId() { return "skinpack." + serializeName; }

    public String getSafePackName() { return StringUtils.sanitize("skinpack." + packDisplayName); }
    public String getSafeSkinName() { return StringUtils.sanitize("skin." + packDisplayName + "." + skinDisplayName); }

    public boolean isInternal() { return texture instanceof AssetSource.Resource; }

    // Backwards-compatible accessors used by generated mixins and original Kotlin code
    public net.minecraft.util.Identifier getIdentifier() { return this.identifier; }
    public net.minecraft.util.Identifier getCapeIdentifier() { return this.capeIdentifier; }
}

