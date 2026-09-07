package io.github.brandonitaly.bedrockskins.bedrock;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Legacy Bedrock 1.8 polygon mesh embedded in a geometry bone. */
public class BedrockPolyMesh {
    @SerializedName("normalized_uvs")
    private Boolean normalizedUvs;
    private List<List<Float>> positions;
    private List<List<Float>> normals;
    private List<List<Float>> uvs;
    private List<List<List<Integer>>> polys;

    public Boolean getNormalizedUvs() { return normalizedUvs; }
    public List<List<Float>> getPositions() { return positions; }
    public List<List<Float>> getNormals() { return normals; }
    public List<List<Float>> getUvs() { return uvs; }
    public List<List<List<Integer>>> getPolys() { return polys; }
}
