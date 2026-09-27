package io.github.brandonitaly.bedrockskins.bedrock;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BedrockGeometry {
    @SerializedName("bedrockskins_legacy_animation")
    private boolean legacyAnimation;
    public boolean isLegacyAnimation() { return legacyAnimation; }

    private GeometryDescription description;
    private List<BedrockBone> bones;

    @SerializedName("animationArmsDown")
    private Boolean animationArmsDown = false;
    @SerializedName("animationArmsOutFront")
    private Boolean animationArmsOutFront = false;
    @SerializedName("animationStatueOfLibertyArms")
    private Boolean animationStatueOfLibertyArms = false;
    @SerializedName("animationStationaryLegs")
    private Boolean animationStationaryLegs = false;
    @SerializedName("animationSingleLegAnimation")
    private Boolean animationSingleLegAnimation = false;
    @SerializedName("animationSingleArmAnimation")
    private Boolean animationSingleArmAnimation = false;
    @SerializedName("animationInvertedCrouch")
    private Boolean animationInvertedCrouch = false;
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

    public Boolean getAnimationArmsDown() { return animationArmsDown; }
    public Boolean getAnimationArmsOutFront() { return animationArmsOutFront; }
    public Boolean getAnimationStatueOfLibertyArms() { return animationStatueOfLibertyArms; }
    public Boolean getAnimationStationaryLegs() { return animationStationaryLegs; }
    public Boolean getAnimationSingleLegAnimation() { return animationSingleLegAnimation; }
    public Boolean getAnimationSingleArmAnimation() { return animationSingleArmAnimation; }
    public Boolean getAnimationInvertedCrouch() { return animationInvertedCrouch; }
    public Boolean getAnimationDontShowArmor() { return animationDontShowArmor; }
    public Boolean getAnimationHeadDisabled() { return animationHeadDisabled; }
    public Boolean getAnimationBodyDisabled() { return animationBodyDisabled; }
    public Boolean getAnimationRightArmDisabled() { return animationRightArmDisabled; }
    public Boolean getAnimationLeftArmDisabled() { return animationLeftArmDisabled; }
    public Boolean getAnimationRightLegDisabled() { return animationRightLegDisabled; }
    public Boolean getAnimationLeftLegDisabled() { return animationLeftLegDisabled; }
    public Boolean getAnimationForceHeadArmor() { return animationForceHeadArmor; }
    public Boolean getAnimationForceBodyArmor() { return animationForceBodyArmor; }
    public Boolean getAnimationForceRightArmArmor() { return animationForceRightArmArmor; }
    public Boolean getAnimationForceLeftArmArmor() { return animationForceLeftArmArmor; }
    public Boolean getAnimationForceRightLegArmor() { return animationForceRightLegArmor; }
    public Boolean getAnimationForceLeftLegArmor() { return animationForceLeftLegArmor; }}
