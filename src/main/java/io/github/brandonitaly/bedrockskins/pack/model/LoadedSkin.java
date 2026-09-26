package io.github.brandonitaly.bedrockskins.pack.model;

import io.github.brandonitaly.bedrockskins.pack.StringUtils;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public class LoadedSkin {
    public final String serializeName;
    public final String packDisplayName;
    public final String skinDisplayName;
    public final JsonObject geometryData;
    public final AssetSource texture;
    public final AssetSource cape;
    public final boolean upsideDown;
    public boolean unfair = false;

    public final SkinId skinId;
    public final String safePackName;
    public final String safeSkinName;
    public final String packId;

    public Identifier identifier;
    public Identifier capeIdentifier;
    public String hash = "";

    /** Network assets exist for rendering, never as locally installed wardrobe entries. */
    public boolean isRemote() {
        return texture instanceof AssetSource.Remote || cape instanceof AssetSource.Remote;
    }

    /** Remote pixel data cannot be reloaded from disk after its GPU texture is released. */
    public boolean hasAvailableContent(String expectedHash) {
        return expectedHash != null && expectedHash.equals(hash)
            && (!(texture instanceof AssetSource.Remote)
                || (identifier != null && (cape == null || capeIdentifier != null)));
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, JsonObject geometryData, AssetSource texture) {
        this(serializeName, packDisplayName, skinDisplayName, geometryData, texture, null, false);
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, JsonObject geometryData, AssetSource texture, AssetSource cape) {
        this(serializeName, packDisplayName, skinDisplayName, geometryData, texture, cape, false);
    }

    public LoadedSkin(String serializeName, String packDisplayName, String skinDisplayName, JsonObject geometryData, AssetSource texture, AssetSource cape, boolean upsideDown) {
        this(SkinId.of(serializeName, skinDisplayName), serializeName, packDisplayName, skinDisplayName,
            geometryData, texture, cape, upsideDown);
    }

    public LoadedSkin(SkinId skinId, String serializeName, String packDisplayName, String skinDisplayName,
                      JsonObject geometryData, AssetSource texture, AssetSource cape, boolean upsideDown) {
        this.serializeName = serializeName;
        this.packDisplayName = packDisplayName;
        this.skinDisplayName = skinDisplayName;
        this.geometryData = geometryData;
        this.texture = texture;
        this.cape = cape;
        this.upsideDown = upsideDown;

        this.skinId = skinId;
        this.safePackName = StringUtils.sanitize("skinpack." + packDisplayName);
        this.safeSkinName = StringUtils.sanitize("skin." + packDisplayName + "." + skinDisplayName);
        this.packId = "skinpack." + serializeName;
    }
}
