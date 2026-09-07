package io.github.brandonitaly.bedrockskins.client.render.model;

import io.github.brandonitaly.bedrockskins.bedrock.BedrockFile;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import com.mojang.logging.LogUtils;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class BedrockModelManager {
    private BedrockModelManager() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<SkinId, BedrockPlayerModel> bedrockModels = new HashMap<>();
    private static final Gson gson = new Gson();
    private static BedrockPlayerModel vanillaWideModel;
    private static BedrockPlayerModel vanillaSlimModel;

    public static BedrockPlayerModel getModel(SkinId skinId) {
        if (skinId == null) return null;

        var skin = SkinPackLoader.getLoadedSkin(skinId);
        if (skin == null) return null;

        if (bedrockModels.containsKey(skinId)) {
            var cached = bedrockModels.get(skinId);
            ensureTextureRegistered(skinId, skin);
            return cached;
        }

        ensureTextureRegistered(skinId, skin);

        try {
            BedrockFile bedrockFile = gson.fromJson(skin.geometryData, BedrockFile.class);
            var geometryList = bedrockFile.getGeometries();
            if (geometryList != null && !geometryList.isEmpty()) {
                var geometry = geometryList.getFirst();
                var model = BedrockPlayerModel.create(geometry, false);
                bedrockModels.put(skinId, model);
                return model;
            }
        } catch (Exception e) {
            LOGGER.error("BedrockModelManager: failed to build model for " + skinId, e);
        }

        return null;
    }

    /**
     * Returns the Bedrock vanilla player scaffold used beneath Persona pieces
     * when no Bedrock skin-pack skin is selected. Its UVs match the Mojang skin
     * texture that the renderer continues to use.
     */
    public static BedrockPlayerModel getVanillaPlayerModel(boolean slim) {
        BedrockPlayerModel cached = slim ? vanillaSlimModel : vanillaWideModel;
        if (cached != null) return cached;

        String geometryName = slim ? "geometry.humanoid.customSlim" : "geometry.humanoid.custom";
        try {
            var geometryData = SkinPackLoader.resolveGeometry(geometryName, null);
            if (geometryData == null) return null;
            BedrockFile bedrockFile = gson.fromJson(geometryData, BedrockFile.class);
            var geometries = bedrockFile.getGeometries();
            if (geometries == null || geometries.isEmpty()) return null;

            BedrockPlayerModel model = BedrockPlayerModel.create(geometries.getFirst(), slim);
            if (slim) vanillaSlimModel = model;
            else vanillaWideModel = model;
            return model;
        } catch (Exception e) {
            LOGGER.error("BedrockModelManager: failed to build vanilla {} model", slim ? "slim" : "wide", e);
            return null;
        }
    }

    private static void ensureTextureRegistered(SkinId skinId, LoadedSkin skin) {
        if (skin.identifier != null) return;
        try {
            SkinPackLoader.registerTextureFor(skinId);
        } catch (Exception e) {
            LOGGER.error("BedrockModelManager: failed to register texture for " + skinId, e);
        }
    }

    public static void clearAllModels() {
        bedrockModels.clear();
        vanillaWideModel = null;
        vanillaSlimModel = null;
    }
}
