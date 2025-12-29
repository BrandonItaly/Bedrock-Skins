package com.brandonitaly.bedrockskins.bedrock;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BedrockCube {
    private List<Float> origin;
    private List<Float> size;
    private Object uv; // Can be [u,v] or { "uv": [u, v], "uv_size": [w,h] }
    private Float inflate;
    private Boolean mirror;
    private List<Float> rotation;

    public List<Float> getOrigin() { return origin; }
    public void setOrigin(List<Float> origin) { this.origin = origin; }

    public List<Float> getSize() { return size; }
    public void setSize(List<Float> size) { this.size = size; }

    public Object getUv() { return uv; }
    public void setUv(Object uv) { this.uv = uv; }

    public Float getInflate() { return inflate; }
    public void setInflate(Float inflate) { this.inflate = inflate; }

    public Boolean getMirror() { return mirror; }
    public void setMirror(Boolean mirror) { this.mirror = mirror; }

    public List<Float> getRotation() { return rotation; }
    public void setRotation(List<Float> rotation) { this.rotation = rotation; }
}
