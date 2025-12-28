package com.brandonitaly.bedrockskins.pack

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.minecraft.util.Identifier

data class SkinPackManifest(
    val skins: List<SkinEntry>,
    @SerializedName("serialize_name") val serializeName: String,
    @SerializedName("localization_name") val localizationName: String
)

data class SkinEntry(
    @SerializedName("localization_name") val localizationName: String,
    val geometry: String,
    val texture: String,
    val type: String,
    val cape: String? = null
)

data class LoadedSkin(
    val serializeName: String,
    val packDisplayName: String,
    val skinDisplayName: String,
    val geometryData: JsonObject,
    val texture: AssetSource,
    val cape: AssetSource? = null,
    
    // Runtime Mutables
    @JvmField
    var identifier: Identifier? = null,
    @JvmField
    var capeIdentifier: Identifier? = null
) {
    val key: String 
        get() = "$packDisplayName:$skinDisplayName"

    val id: String
        get() = "skinpack.$serializeName"

    val safePackName: String 
        by lazy { sanitize("skinpack.$packDisplayName") }

    val safeSkinName: String 
        by lazy { sanitize("skin.$packDisplayName.$skinDisplayName") }
        
    val isInternal: Boolean 
        get() = texture is AssetSource.Resource
}

// --- Helpers ---

sealed class AssetSource {
    data class File(val path: String) : AssetSource()
    data class Resource(val id: Identifier) : AssetSource()
    data object Remote : AssetSource()
}

fun sanitize(name: String): String = name.lowercase().replace(Regex("[^a-z0-9/._-]"), "_")