package com.brandonitaly.bedrockskins.pack;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public class LoadedSkin {
    public final String serializeName;
    public final String packDisplayName;
    public final String skinDisplayName;
    public final JsonObject geometryData;
    public final AssetSource texture;
    public final AssetSource cape; // nullable
    public final boolean upsideDown;

    public final SkinId skinId;
    public final String safePackName;
    public final String safeSkinName;
    public final String packId;

    public Identifier identifier;
    public Identifier capeIdentifier;

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, JsonObject geometryData, AssetSource texture) {
        this(serializeName, packDisplayName, skinDisplayName, geometryData, texture, null, false);
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, JsonObject geometryData, AssetSource texture, AssetSource cape) {
        this(serializeName, packDisplayName, skinDisplayName, geometryData, texture, cape, false);
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, JsonObject geometryData, AssetSource texture, AssetSource cape, boolean upsideDown) {
        this.serializeName = serializeName;
        this.packDisplayName = packDisplayName;
        this.skinDisplayName = skinDisplayName;
        this.geometryData = geometryData;
        this.texture = texture;
        this.cape = cape;
        this.upsideDown = upsideDown;

        this.skinId = SkinId.of(serializeName, skinDisplayName);
        this.safePackName = StringUtils.sanitize("skinpack." + packDisplayName);
        this.safeSkinName = StringUtils.sanitize("skin." + packDisplayName + "." + skinDisplayName);
        this.packId = "skinpack." + serializeName;
    }
}