package com.brandonitaly.bedrockskins.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

public record SkinPackManifest(
    List<SkinEntry> skins,
    String serializeName,
    String localizationName,
    String packType
) {
    public static final Codec<SkinPackManifest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(SkinEntry.CODEC).fieldOf("skins").forGetter(SkinPackManifest::skins),
        Codec.STRING.fieldOf("serialize_name").forGetter(SkinPackManifest::serializeName),
        Codec.STRING.fieldOf("localization_name").forGetter(SkinPackManifest::localizationName),
        Codec.STRING.optionalFieldOf("pack_type").forGetter(manifest -> Optional.ofNullable(manifest.packType()))
    ).apply(instance, (skins, serializeName, localizationName, packType) -> 
        new SkinPackManifest(skins, serializeName, localizationName, packType.orElse(null))
    ));
}