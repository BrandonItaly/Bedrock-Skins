package com.brandonitaly.bedrockskins.pack;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SkinPackManifest {
    private List<SkinEntry> skins;

    @SerializedName("serialize_name")
    private String serializeName;

    @SerializedName("localization_name")
    private String localizationName;

    public List<SkinEntry> getSkins() { return skins; }
    public void setSkins(List<SkinEntry> skins) { this.skins = skins; }

    public String getSerializeName() { return serializeName; }
    public void setSerializeName(String serializeName) { this.serializeName = serializeName; }

    public String getLocalizationName() { return localizationName; }
    public void setLocalizationName(String localizationName) { this.localizationName = localizationName; }
}
