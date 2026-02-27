package com.brandonitaly.bedrockskins.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Optional;

public record SkinEntry(
    String localizationName,
    String geometry,
    String texture,
    String type,
    String cape,
    Map<String, String> animations
) {
    public static final Codec<SkinEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("localization_name").forGetter(SkinEntry::localizationName),
        Codec.STRING.fieldOf("geometry").forGetter(SkinEntry::geometry),
        Codec.STRING.fieldOf("texture").forGetter(SkinEntry::texture),
        Codec.STRING.optionalFieldOf("type", "free").forGetter(SkinEntry::type),
        Codec.STRING.optionalFieldOf("cape").forGetter(entry -> Optional.ofNullable(entry.cape())),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("animations", Map.of()).forGetter(SkinEntry::animations)
    ).apply(instance, (localizationName, geometry, texture, type, cape, animations) -> new SkinEntry(
        localizationName, 
        geometry, 
        texture, 
        type, 
        cape.orElse(null), 
        animations == null ? Map.of() : Map.copyOf(animations)
    )));

    // Backwards-compatible getters
    public String getLocalizationName() { return localizationName; }
    public String getGeometry() { return geometry; }
    public String getTexture() { return texture; }
    public String getType() { return type; }
    public String getCape() { return cape; }
    public Map<String, String> getAnimations() { return animations; }
}