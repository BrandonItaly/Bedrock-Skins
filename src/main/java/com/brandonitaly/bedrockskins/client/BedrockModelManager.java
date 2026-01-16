package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.bedrock.BedrockFile;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BedrockModelManager {
    private BedrockModelManager() {}

    private static final Map<UUID, BedrockPlayerModel> bedrockModels = new HashMap<>();
    private static final Map<UUID, SkinId> activeSkinKeys = new HashMap<>();
    private static final Gson gson = new Gson();

    public static BedrockPlayerModel getModel(UUID uuid) {
        SkinId skinKey = SkinManager.getSkin(uuid.toString());
        if (skinKey == null) return null;

        if (!java.util.Objects.equals(skinKey, activeSkinKeys.get(uuid))) {
            bedrockModels.remove(uuid);
            activeSkinKeys.put(uuid, skinKey);
        }

        if (bedrockModels.containsKey(uuid)) {
            return bedrockModels.get(uuid);
        }

        var skin = SkinPackLoader.getLoadedSkin(skinKey);
        if (skin == null) return null;

        try {
            SkinPackLoader.registerTextureFor(skinKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            BedrockFile bedrockFile = gson.fromJson(skin.geometryData, BedrockFile.class);
            var geometryList = bedrockFile.getGeometries();
            if (geometryList != null && !geometryList.isEmpty()) {
                var geometry = geometryList.get(0);
                var model = BedrockPlayerModel.create(geometry, false);
                bedrockModels.put(uuid, model);
                return model;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void clearAllModels() {
        bedrockModels.clear();
        activeSkinKeys.clear();
    }
}
