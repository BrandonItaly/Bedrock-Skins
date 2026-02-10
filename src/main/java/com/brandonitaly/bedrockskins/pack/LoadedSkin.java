package com.brandonitaly.bedrockskins.pack;

import com.google.gson.JsonObject;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

public class LoadedSkin {
    public final String serializeName;
    public final String packDisplayName;
    public final String skinDisplayName;
    public final com.google.gson.JsonObject geometryData;
    public final AssetSource texture;
    public final AssetSource cape; // nullable
    public final boolean upsideDown;

    //? if >=1.21.11 {
    public Identifier identifier;
    public Identifier capeIdentifier;
    //?} else {
    /*public ResourceLocation identifier;
    public ResourceLocation capeIdentifier;*/
    //?}

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, com.google.gson.JsonObject geometryData, AssetSource texture) {
        this(serializeName, packDisplayName, skinDisplayName, geometryData, texture, null, false);
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, com.google.gson.JsonObject geometryData, AssetSource texture, AssetSource cape) {
        this(serializeName, packDisplayName, skinDisplayName, geometryData, texture, cape, false);
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, com.google.gson.JsonObject geometryData, AssetSource texture, AssetSource cape, boolean upsideDown) {
        this.serializeName = serializeName;
        this.packDisplayName = packDisplayName;
        this.skinDisplayName = skinDisplayName;
        this.geometryData = geometryData;
        this.texture = texture;
        this.cape = cape;
        this.upsideDown = upsideDown;
    }

    public String getSerializeName() { return serializeName; }
    public String getPackDisplayName() { return packDisplayName; }
    public String getSkinDisplayName() { return skinDisplayName; }
    public com.google.gson.JsonObject getGeometryData() { return geometryData; }
    public AssetSource getTexture() { return texture; }
    public AssetSource getCape() { return cape; }
    public boolean isUpsideDown() { return upsideDown; }

    // Canonical identifier for a skin (uses serializeName for the pack id)
    public String getKey() { return serializeName + ":" + skinDisplayName; }
    public String getId() { return "skinpack." + serializeName; }

    public String getSafePackName() { return StringUtils.sanitize("skinpack." + packDisplayName); }
    public String getSafeSkinName() { return StringUtils.sanitize("skin." + packDisplayName + "." + skinDisplayName); }

    // New: strongly-typed ID (pack uses serializeName)
    public SkinId getSkinId() { return SkinId.of(serializeName, skinDisplayName); }

    public boolean isInternal() { return texture instanceof AssetSource.Resource; }

    // Backwards-compatible accessors used by generated mixins and original Kotlin code
    //? if >=1.21.11 {
    public Identifier getIdentifier() { return this.identifier; }
    public Identifier getCapeIdentifier() { return this.capeIdentifier; }
    //?} else {
    /*public ResourceLocation getIdentifier() { return this.identifier; }
    public ResourceLocation getCapeIdentifier() { return this.capeIdentifier; }*/
    //?}
}

