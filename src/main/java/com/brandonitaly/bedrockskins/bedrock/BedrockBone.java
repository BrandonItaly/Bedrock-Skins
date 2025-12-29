package com.brandonitaly.bedrockskins.bedrock;

import java.util.List;
import java.util.Map;

public class BedrockBone {
    private String name;
    private String parent;
    private List<Float> pivot;
    private List<Float> rotation;
    private List<BedrockCube> cubes;
    private Map<String, List<Float>> locators;
    private Float inflate;
    private Boolean mirror;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParent() { return parent; }
    public void setParent(String parent) { this.parent = parent; }

    public List<Float> getPivot() { return pivot; }
    public void setPivot(List<Float> pivot) { this.pivot = pivot; }

    public List<Float> getRotation() { return rotation; }
    public void setRotation(List<Float> rotation) { this.rotation = rotation; }

    public List<BedrockCube> getCubes() { return cubes; }
    public void setCubes(List<BedrockCube> cubes) { this.cubes = cubes; }

    public Map<String, List<Float>> getLocators() { return locators; }
    public void setLocators(Map<String, List<Float>> locators) { this.locators = locators; }

    public Float getInflate() { return inflate; }
    public void setInflate(Float inflate) { this.inflate = inflate; }

    public Boolean getMirror() { return mirror; }
    public void setMirror(Boolean mirror) { this.mirror = mirror; }
}
