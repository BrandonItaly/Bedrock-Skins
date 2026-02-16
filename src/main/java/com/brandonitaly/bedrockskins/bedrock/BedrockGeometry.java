package com.brandonitaly.bedrockskins.bedrock;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class BedrockGeometry {
    private GeometryDescription description;
    private List<BedrockBone> bones;

    @SerializedName("animationArmsOutFront")
    private Boolean animationArmsOutFront = false;

    @SerializedName("animationStationaryLegs")
    private Boolean animationStationaryLegs = false;

    @SerializedName("animationSingleLegAnimation")
    private Boolean animationSingleLegAnimation = false;

    public GeometryDescription getDescription() { return description; }
    public void setDescription(GeometryDescription description) { this.description = description; }

    public List<BedrockBone> getBones() { return bones; }
    public void setBones(List<BedrockBone> bones) { this.bones = bones; }

    public Boolean getAnimationArmsOutFront() { return animationArmsOutFront; }
    public void setAnimationArmsOutFront(Boolean animationArmsOutFront) { this.animationArmsOutFront = animationArmsOutFront; }

    public Boolean getAnimationStationaryLegs() { return animationStationaryLegs; }
    public void setAnimationStationaryLegs(Boolean animationStationaryLegs) { this.animationStationaryLegs = animationStationaryLegs; }

    public Boolean getAnimationSingleLegAnimation() { return animationSingleLegAnimation; }
    public void setAnimationSingleLegAnimation(Boolean animationSingleLegAnimation) { this.animationSingleLegAnimation = animationSingleLegAnimation; }
}
