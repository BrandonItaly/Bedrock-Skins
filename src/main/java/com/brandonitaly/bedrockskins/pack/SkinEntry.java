package com.brandonitaly.bedrockskins.pack;

import com.google.gson.annotations.SerializedName;

public class SkinEntry {
    @SerializedName("localization_name")
    private String localizationName;
    private String geometry;
    private String texture;
    private String type;
    private String cape;

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
}
