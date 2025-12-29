package com.brandonitaly.bedrockskins.bedrock;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BedrockFile {
    @SerializedName("format_version")
    private String formatVersion;

    @SerializedName("minecraft:geometry")
    private List<BedrockGeometry> geometries;

    public String getFormatVersion() { return formatVersion; }
    public void setFormatVersion(String formatVersion) { this.formatVersion = formatVersion; }

    public List<BedrockGeometry> getGeometries() { return geometries; }
    public void setGeometries(List<BedrockGeometry> geometries) { this.geometries = geometries; }
}
