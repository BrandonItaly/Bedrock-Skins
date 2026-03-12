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
    @SerializedName("animationSingleArmAnimation")
    private Boolean animationSingleArmAnimation = false;
    @SerializedName("animationDontShowArmor")
    private Boolean animationDontShowArmor = false;

    @SerializedName("animationHeadDisabled")
    private Boolean animationHeadDisabled = false;
    @SerializedName("animationBodyDisabled")
    private Boolean animationBodyDisabled = false;
    @SerializedName("animationRightArmDisabled")
    private Boolean animationRightArmDisabled = false;
    @SerializedName("animationLeftArmDisabled")
    private Boolean animationLeftArmDisabled = false;
    @SerializedName("animationRightLegDisabled")
    private Boolean animationRightLegDisabled = false;
    @SerializedName("animationLeftLegDisabled")
    private Boolean animationLeftLegDisabled = false;

    @SerializedName("animationForceHeadArmor")
    private Boolean animationForceHeadArmor = false;
    @SerializedName("animationForceBodyArmor")
    private Boolean animationForceBodyArmor = false;
    @SerializedName("animationForceRightArmArmor")
    private Boolean animationForceRightArmArmor = false;
    @SerializedName("animationForceLeftArmArmor")
    private Boolean animationForceLeftArmArmor = false;
    @SerializedName("animationForceRightLegArmor")
    private Boolean animationForceRightLegArmor = false;
    @SerializedName("animationForceLeftLegArmor")
    private Boolean animationForceLeftLegArmor = false;

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

    public Boolean getAnimationSingleArmAnimation() { return animationSingleArmAnimation; }
    public void setAnimationSingleArmAnimation(Boolean animationSingleArmAnimation) { this.animationSingleArmAnimation = animationSingleArmAnimation; }

    public Boolean getAnimationDontShowArmor() { return animationDontShowArmor; }
    public void setAnimationDontShowArmor(Boolean animationDontShowArmor) { this.animationDontShowArmor = animationDontShowArmor; }

    public Boolean getAnimationHeadDisabled() { return animationHeadDisabled; }
    public void setAnimationHeadDisabled(Boolean animationHeadDisabled) { this.animationHeadDisabled = animationHeadDisabled; }

    public Boolean getAnimationBodyDisabled() { return animationBodyDisabled; }
    public void setAnimationBodyDisabled(Boolean animationBodyDisabled) { this.animationBodyDisabled = animationBodyDisabled; }

    public Boolean getAnimationRightArmDisabled() { return animationRightArmDisabled; }
    public void setAnimationRightArmDisabled(Boolean animationRightArmDisabled) { this.animationRightArmDisabled = animationRightArmDisabled; }

    public Boolean getAnimationLeftArmDisabled() { return animationLeftArmDisabled; }
    public void setAnimationLeftArmDisabled(Boolean animationLeftArmDisabled) { this.animationLeftArmDisabled = animationLeftArmDisabled; }

    public Boolean getAnimationRightLegDisabled() { return animationRightLegDisabled; }
    public void setAnimationRightLegDisabled(Boolean animationRightLegDisabled) { this.animationRightLegDisabled = animationRightLegDisabled; }

    public Boolean getAnimationLeftLegDisabled() { return animationLeftLegDisabled; }
    public void setAnimationLeftLegDisabled(Boolean animationLeftLegDisabled) { this.animationLeftLegDisabled = animationLeftLegDisabled; }

    public Boolean getAnimationForceHeadArmor() { return animationForceHeadArmor; }
    public void setAnimationForceHeadArmor(Boolean animationForceHeadArmor) { this.animationForceHeadArmor = animationForceHeadArmor; }

    public Boolean getAnimationForceBodyArmor() { return animationForceBodyArmor; }
    public void setAnimationForceBodyArmor(Boolean animationForceBodyArmor) { this.animationForceBodyArmor = animationForceBodyArmor; }

    public Boolean getAnimationForceRightArmArmor() { return animationForceRightArmArmor; }
    public void setAnimationForceRightArmArmor(Boolean animationForceRightArmArmor) { this.animationForceRightArmArmor = animationForceRightArmArmor; }

    public Boolean getAnimationForceLeftArmArmor() { return animationForceLeftArmArmor; }
    public void setAnimationForceLeftArmArmor(Boolean animationForceLeftArmArmor) { this.animationForceLeftArmArmor = animationForceLeftArmArmor; }

    public Boolean getAnimationForceRightLegArmor() { return animationForceRightLegArmor; }
    public void setAnimationForceRightLegArmor(Boolean animationForceRightLegArmor) { this.animationForceRightLegArmor = animationForceRightLegArmor; }

    public Boolean getAnimationForceLeftLegArmor() { return animationForceLeftLegArmor; }
    public void setAnimationForceLeftLegArmor(Boolean animationForceLeftLegArmor) { this.animationForceLeftLegArmor = animationForceLeftLegArmor; }
}
