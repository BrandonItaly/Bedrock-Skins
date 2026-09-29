package io.github.brandonitaly.bedrockskins.client.render.model;

import com.google.gson.Gson;
import io.github.brandonitaly.bedrockskins.bedrock.BedrockBone;
import net.minecraft.client.model.geom.ModelPart;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArmorPartFitTest {
    @Test void fitsHeadTorsoArmsAndStrangerThingsLegs() {
        // pivot Y, cube bottom, height, standard local top, standard height
        float[][] cases = {{22,22,8,-8,8}, {22,11,11,0,12}, {21,11,11,-2,12}, {10,0,11,0,12}};
        for (float[] values : cases) {
            var bone = new Gson().fromJson("{\"pivot\":[0," + values[0]
                + ",0],\"cubes\":[{\"origin\":[0," + values[1]
                + ",0],\"size\":[4," + values[2] + ",4]}]}", BedrockBone.class);
            var fit = ArmorPartFit.from(bone, values[3], values[4]);
            assertEquals(values[0] - values[1] - values[2], fit.offset() + values[3] * fit.scale(), 0.00001f);
            assertEquals(values[0] - values[1], fit.offset() + (values[3] + values[4]) * fit.scale(), 0.00001f);
        }
    }

    @Test void fitRespectsAnimatedScaleAndResetsBetweenEntities() {
        var part = new ModelPart(List.of(), Map.of());
        part.yScale = 2;
        part.xRot = (float) Math.PI / 2;
        new ArmorPartFit(-1, 11f / 12).apply(part);
        assertEquals(-2, part.z, 0.00001f);
        assertEquals(22f / 12, part.yScale, 0.00001f);
        part.resetPose();
        ArmorPartFit.STANDARD.apply(part);
        assertEquals(1, part.yScale);
        assertEquals(0, part.z);
    }
}
