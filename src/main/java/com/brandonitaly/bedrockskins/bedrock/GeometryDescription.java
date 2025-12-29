package com.brandonitaly.bedrockskins.bedrock;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeometryDescription {
    private String identifier;
    @SerializedName("texture_width")
    private int textureWidth;
    @SerializedName("texture_height")
    private int textureHeight;
    @SerializedName("visible_bounds_width")
    private Float visibleBoundsWidth;
    @SerializedName("visible_bounds_height")
    private Float visibleBoundsHeight;
    @SerializedName("visible_bounds_offset")
    private List<Float> visibleBoundsOffset;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public int getTextureWidth() { return textureWidth; }
    public void setTextureWidth(int textureWidth) { this.textureWidth = textureWidth; }

    public int getTextureHeight() { return textureHeight; }
    public void setTextureHeight(int textureHeight) { this.textureHeight = textureHeight; }

    public Float getVisibleBoundsWidth() { return visibleBoundsWidth; }
    public void setVisibleBoundsWidth(Float visibleBoundsWidth) { this.visibleBoundsWidth = visibleBoundsWidth; }

    public Float getVisibleBoundsHeight() { return visibleBoundsHeight; }
    public void setVisibleBoundsHeight(Float visibleBoundsHeight) { this.visibleBoundsHeight = visibleBoundsHeight; }

    public List<Float> getVisibleBoundsOffset() { return visibleBoundsOffset; }
    public void setVisibleBoundsOffset(List<Float> visibleBoundsOffset) { this.visibleBoundsOffset = visibleBoundsOffset; }
}
