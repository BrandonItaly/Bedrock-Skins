package com.brandonitaly.bedrockskins.pack;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public class SkinEntry {
    public static final Codec<SkinEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("localization_name").forGetter(SkinEntry::getLocalizationName),
        Codec.STRING.fieldOf("geometry").forGetter(SkinEntry::getGeometry),
        Codec.STRING.fieldOf("texture").forGetter(SkinEntry::getTexture),
        Codec.STRING.optionalFieldOf("type", "free").forGetter(SkinEntry::getType),
        Codec.STRING.optionalFieldOf("cape").forGetter(entry -> java.util.Optional.ofNullable(entry.getCape())),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("animations", java.util.Map.of()).forGetter(entry -> entry.getAnimations() == null ? java.util.Map.of() : entry.getAnimations())
    ).apply(instance, (localizationName, geometry, texture, type, cape, animations) -> {
        SkinEntry entry = new SkinEntry();
        entry.setLocalizationName(localizationName);
        entry.setGeometry(geometry);
        entry.setTexture(texture);
        entry.setType(type);
        entry.setCape(cape.orElse(null));
        entry.setAnimations(animations);
        return entry;
    }));

    @SerializedName("localization_name")
    private String localizationName;
    private String geometry;
    private String texture;
    private String type;
    private String cape;
    private Map<String, String> animations;

    public String getLocalizationName() { return localizationName; }
    public void setLocalizationName(String localizationName) { this.localizationName = localizationName; }

    public String getGeometry() { return geometry; }
    public void setGeometry(String geometry) { this.geometry = geometry; }

    public String getTexture() { return texture; }
    public void setTexture(String texture) { this.texture = texture; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCape() { return cape; }
    public void setCape(String cape) { this.cape = cape; }

    public Map<String, String> getAnimations() { return animations; }
    public void setAnimations(Map<String, String> animations) { this.animations = animations; }
}
