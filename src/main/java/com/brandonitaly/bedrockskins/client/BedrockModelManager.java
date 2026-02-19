package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.bedrock.BedrockFile;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public final class BedrockModelManager {
    private BedrockModelManager() {}

    private static final Map<SkinId, BedrockPlayerModel> bedrockModels = new HashMap<>();
    private static final Gson gson = new Gson();

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
                var geometry = geometryList.get(0);
                var model = BedrockPlayerModel.create(geometry, false);
                bedrockModels.put(skinId, model);
                return model;
            }
        } catch (Exception e) {
            BedrockSkinsLog.error("BedrockModelManager: failed to build model for " + skinId, e);
        }

        return null;
    }

    private static void ensureTextureRegistered(SkinId skinId, com.brandonitaly.bedrockskins.pack.LoadedSkin skin) {
        if (skin.identifier != null) return;
        try {
            SkinPackLoader.registerTextureFor(skinId);
        } catch (Exception e) {
            BedrockSkinsLog.error("BedrockModelManager: failed to register texture for " + skinId, e);
        }
    }

    public static void clearAllModels() {
        bedrockModels.clear();
    }
}
