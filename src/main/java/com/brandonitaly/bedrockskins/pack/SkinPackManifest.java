package com.brandonitaly.bedrockskins.pack;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public class SkinPackManifest {
    public static final Codec<SkinPackManifest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(SkinEntry.CODEC).fieldOf("skins").forGetter(SkinPackManifest::getSkins),
        Codec.STRING.fieldOf("serialize_name").forGetter(SkinPackManifest::getSerializeName),
        Codec.STRING.fieldOf("localization_name").forGetter(SkinPackManifest::getLocalizationName),
        Codec.STRING.optionalFieldOf("pack_type").forGetter(manifest -> java.util.Optional.ofNullable(manifest.getPackType()))
    ).apply(instance, (skins, serializeName, localizationName, packType) -> {
        SkinPackManifest manifest = new SkinPackManifest();
        manifest.setSkins(skins);
        manifest.setSerializeName(serializeName);
        manifest.setLocalizationName(localizationName);
        manifest.setPackType(packType.orElse(null));
        return manifest;
    }));

    private List<SkinEntry> skins;

    @SerializedName("serialize_name")
    private String serializeName;

    @SerializedName("localization_name")
    private String localizationName;

    @SerializedName("pack_type")
    private String packType;

    public List<SkinEntry> getSkins() { return skins; }
    public void setSkins(List<SkinEntry> skins) { this.skins = skins; }

    public String getSerializeName() { return serializeName; }
    public void setSerializeName(String serializeName) { this.serializeName = serializeName; }

    public String getLocalizationName() { return localizationName; }
    public void setLocalizationName(String localizationName) { this.localizationName = localizationName; }

    public String getPackType() { return packType; }
    public void setPackType(String packType) { this.packType = packType; }
}
