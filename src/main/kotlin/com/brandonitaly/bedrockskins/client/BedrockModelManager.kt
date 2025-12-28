package com.brandonitaly.bedrockskins.client

import com.brandonitaly.bedrockskins.bedrock.BedrockFile
import com.brandonitaly.bedrockskins.pack.SkinPackLoader
import com.google.gson.Gson
import java.util.UUID

object BedrockModelManager {
    private val bedrockModels = mutableMapOf<UUID, BedrockPlayerModel>()
    private val activeSkinKeys = mutableMapOf<UUID, String>()
    private val gson = Gson()

    @JvmStatic
    fun getModel(uuid: UUID): BedrockPlayerModel? {
        val skinKey = SkinManager.getSkin(uuid.toString()) ?: return null
        
        if (activeSkinKeys[uuid] != skinKey) {
            bedrockModels.remove(uuid)
            activeSkinKeys[uuid] = skinKey
        }

        if (bedrockModels.containsKey(uuid)) {
            return bedrockModels[uuid]
        }

        val skin = SkinPackLoader.loadedSkins[skinKey] ?: return null
        // Ensure the texture is registered on-demand so we don't eagerly load all textures
        try {
            SkinPackLoader.registerTextureFor(skinKey)
        } catch (e: Exception) {
            // Non-fatal: texture registration failure shouldn't prevent model creation
            e.printStackTrace()
        }
        
        try {
            val bedrockFile = gson.fromJson(skin.geometryData, BedrockFile::class.java)
            val geometry = bedrockFile.geometries?.firstOrNull()
            if (geometry != null) {
                val model = BedrockPlayerModel.create(geometry, false)
                bedrockModels[uuid] = model
                return model
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }

    /**
     * Clears all cached BedrockPlayerModels and active skin keys.
     */
    @JvmStatic
    fun clearAllModels() {
        bedrockModels.clear()
        activeSkinKeys.clear()
    }
}
