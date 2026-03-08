package com.brandonitaly.bedrockskins.pack;

import com.google.gson.JsonObject;
import net.minecraft.resources./*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/;

public class LoadedSkin {
    public final String serializeName;
    public final String packDisplayName;
    public final String skinDisplayName;
    public final JsonObject geometryData;
    public final AssetSource texture;
    public final AssetSource cape; // nullable
    public final boolean upsideDown;

    private final SkinId skinId;
    private final String safePackName;
    private final String safeSkinName;
    private final String packId;

    public /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ identifier;
    public /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ capeIdentifier;

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

        // Pre-calculate
        this.skinId = SkinId.of(serializeName, skinDisplayName);
        this.safePackName = StringUtils.sanitize("skinpack." + packDisplayName);
        this.safeSkinName = StringUtils.sanitize("skin." + packDisplayName + "." + skinDisplayName);
        this.packId = "skinpack." + serializeName;
    }

    public String getSerializeName() { return serializeName; }
    public String getPackDisplayName() { return packDisplayName; }
    public String getSkinDisplayName() { return skinDisplayName; }
    public JsonObject getGeometryData() { return geometryData; }
    public AssetSource getTexture() { return texture; }
    public AssetSource getCape() { return cape; }
    public boolean isUpsideDown() { return upsideDown; }

    public String getId() { return packId; }
    public String getSafePackName() { return safePackName; }
    public String getSafeSkinName() { return safeSkinName; }
    public SkinId getSkinId() { return skinId; }

    public /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ getIdentifier() { return identifier; }
}