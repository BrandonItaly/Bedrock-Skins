package io.github.brandonitaly.bedrockskins.client.appearance.emote;

import com.google.gson.JsonParser;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import java.util.UUID;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmoteAttachmentTest {
    private static HumanoidModel<HumanoidRenderState> model() {
        return new HumanoidModel<>(LayerDefinition.create(
            HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 64).bakeRoot());
    }

    @Test void attachmentReceivesEmoteOnceWithoutChangingTheSourcePose() {
        var source = model();
        var attachment = model();
        UUID id = UUID.randomUUID();
        var animation = JsonParser.parseString("""
            {"bones":{"body":{"position":[2,3,4],"rotation":[25,30,10]},
            "rightarm":{"rotation":[40,0,0]}}}
            """).getAsJsonObject();
        EmoteManager.play(id, new LoadedEmote("test", "Test", "test", animation, 60));
        try {
            EmoteManager.apply(source, id);
            float sourceBodyX = source.body.x;
            float sourceBodyRotation = source.body.xRot;
            var sourceParts = new net.minecraft.client.model.geom.ModelPart[]{source.head, source.body, source.rightArm, source.leftArm, source.rightLeg, source.leftLeg};
            var targetParts = new net.minecraft.client.model.geom.ModelPart[]{attachment.head, attachment.body, attachment.rightArm, attachment.leftArm, attachment.rightLeg, attachment.leftLeg};
            for (int i = 0; i < sourceParts.length; i++) EmoteManager.copyBasePose(sourceParts[i], targetParts[i]);
            assertEquals(sourceBodyX, source.body.x);
            assertEquals(sourceBodyRotation, source.body.xRot);
            EmoteManager.apply(attachment, id);
            for (int i = 0; i < sourceParts.length; i++) {
                assertEquals(sourceParts[i].x, targetParts[i].x, 0.0001f);
                assertEquals(sourceParts[i].y, targetParts[i].y, 0.0001f);
                assertEquals(sourceParts[i].z, targetParts[i].z, 0.0001f);
                assertEquals(sourceParts[i].xRot, targetParts[i].xRot, 0.0001f);
                assertEquals(sourceParts[i].yRot, targetParts[i].yRot, 0.0001f);
                assertEquals(sourceParts[i].zRot, targetParts[i].zRot, 0.0001f);
            }
        } finally {
            EmoteManager.stop(id);
            EmoteManager.restorePoseDeltas(source);
            EmoteManager.restorePoseDeltas(attachment);
        }
    }
}
