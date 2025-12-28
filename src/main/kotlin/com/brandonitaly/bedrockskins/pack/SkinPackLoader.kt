package com.brandonitaly.bedrockskins.pack

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import java.io.*
import java.nio.charset.StandardCharsets

object SkinPackLoader {
    private var vanillaGeometryJson: JsonObject? = null
    private val gson = Gson()
    @JvmField
    val loadedSkins = mutableMapOf<String, LoadedSkin>()
    val skinPacksDir = File("skin_packs")
    val translations = mutableMapOf<String, MutableMap<String, String>>()
    var packOrder: List<String> = emptyList()

    // --- Public API ---

    @JvmStatic
    fun getTranslation(key: String): String? {
        val currentLang = try { MinecraftClient.getInstance().languageManager.language } catch (_: Exception) { "en_us" }
        return translations[currentLang]?.get(key)
            ?: translations.values.firstOrNull { it.containsKey(key) }?.get(key)
            ?: translations["en_us"]?.get(key)
    }

    fun loadPacks() {
        loadedSkins.clear()
        translations.clear()
        
        if (skinPacksDir.exists()) {
            skinPacksDir.listFiles()?.filter { it.isDirectory }?.forEach { loadExternalPack(it) }
        }

        val client = try { MinecraftClient.getInstance() } catch (e: Exception) { null } ?: return
        val manager = client.resourceManager

        loadVanillaGeometry(manager)
        loadInternalPacks(manager)
        loadPackOrder(manager)
    }

    @JvmStatic
    fun registerTextures() {
        println("SkinPackLoader: Registering all textures...")
        loadedSkins.values.forEach { registerSkinAssets(it) }
    }

    @JvmStatic
    fun registerTextureFor(key: String): Identifier? {
        val skin = loadedSkins[key] ?: return null
        registerSkinAssets(skin)
        return skin.identifier
    }

    @JvmStatic
    fun registerRemoteSkinStatic(key: String, geometryJson: String, textureData: ByteArray) {
        registerRemoteSkin(key, geometryJson, textureData)
    }

    fun registerRemoteSkin(key: String, geometryJson: String, textureData: ByteArray) {
        if (loadedSkins.containsKey(key)) return
        try {
            if (!validateRemoteData(key, textureData, geometryJson)) return

            val image = NativeImage.read(ByteArrayInputStream(textureData))
            val texture = NativeImageBackedTexture({ "bedrock_skin_remote" }, image)
            val safeKey = sanitize(key)
            val id = Identifier.of("bedrockskins", "skins/remote/$safeKey")

            MinecraftClient.getInstance().textureManager.registerTexture(id, texture)
            
            loadedSkins[key] = LoadedSkin(
                serializeName = "Remote", 
                packDisplayName = "Remote",
                skinDisplayName = key,
                geometryData = JsonParser.parseString(geometryJson).asJsonObject,
                texture = AssetSource.Remote,
                identifier = id
            )
            println("Registered remote skin: $key")
        } catch (e: Exception) {
            println("Failed to register remote skin $key: $e")
        }
    }

    // --- Loading Logic ---

    private fun loadVanillaGeometry(manager: ResourceManager) {
        try {
            val id = Identifier.of("bedrockskins", "skin_packs/vanilla/geometry.json")
            manager.getResource(id).ifPresent { res ->
                res.inputStream.use { vanillaGeometryJson = JsonParser.parseReader(InputStreamReader(it)).asJsonObject }
                println("SkinPackLoader: Loaded vanilla geometry fallback.")
            }
        } catch (e: Exception) {
            println("SkinPackLoader: ERROR loading vanilla geometry: ${e.message}")
        }
    }

    private fun loadExternalPack(packDir: File) {
        val skinsFile = File(packDir, "skins.json")
        if (!skinsFile.exists()) return

        try {
            val geometryJson = File(packDir, "geometry.json").takeIf { it.exists() }
                ?.let { JsonParser.parseReader(FileReader(it)).asJsonObject }
            
            val manifest = gson.fromJson(FileReader(skinsFile), SkinPackManifest::class.java)
            loadExternalTranslations(packDir)

            manifest.skins.forEach { entry ->
                val geometry = resolveGeometry(entry.geometry, geometryJson) ?: return@forEach
                val textureFile = File(packDir, entry.texture)
                val capeFile = entry.cape?.let { File(packDir, it) }?.takeIf { it.exists() }

                if (textureFile.exists()) {
                    val key = "${manifest.localizationName}:${entry.localizationName}"
                    loadedSkins[key] = LoadedSkin(
                        serializeName = manifest.serializeName,
                        packDisplayName = manifest.localizationName,
                        skinDisplayName = entry.localizationName,
                        geometryData = geometry,
                        texture = AssetSource.File(textureFile.absolutePath),
                        cape = capeFile?.let { AssetSource.File(it.absolutePath) }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadInternalPacks(manager: ResourceManager) {
        println("SkinPackLoader: Scanning resources...")
        manager.findResources("skin_packs") { it.path.endsWith("skins.json") }.forEach { (id, resource) ->
            try {
                val geoId = Identifier.of(id.namespace, id.path.replace("skins.json", "geometry.json"))
                val geoJson = manager.getResource(geoId).map { 
                    it.inputStream.use { stream -> JsonParser.parseReader(InputStreamReader(stream)).asJsonObject }
                }.orElse(null)

                val manifest = resource.inputStream.use { gson.fromJson(InputStreamReader(it), SkinPackManifest::class.java) }
                val packPath = id.path.substringBeforeLast("/")
                
                loadInternalTranslations(manager, id.namespace, packPath)

                manifest.skins.forEach { entry ->
                    val geometry = resolveGeometry(entry.geometry, geoJson) ?: return@forEach
                    val textureId = Identifier.of(id.namespace, "$packPath/${entry.texture}".lowercase())
                    
                    if (manager.getResource(textureId).isPresent) {
                        val capeId = entry.cape?.let { Identifier.of(id.namespace, "$packPath/$it".lowercase()) }
                            ?.takeIf { manager.getResource(it).isPresent }

                        val key = "${manifest.localizationName}:${entry.localizationName}"
                        loadedSkins[key] = LoadedSkin(
                            serializeName = manifest.serializeName,
                            packDisplayName = manifest.localizationName,
                            skinDisplayName = entry.localizationName,
                            geometryData = geometry,
                            texture = AssetSource.Resource(textureId),
                            cape = capeId?.let { AssetSource.Resource(it) }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Helpers: Geometry & Assets ---

    private fun resolveGeometry(name: String, localGeo: JsonObject?): JsonObject? {
        val raw = findGeometryNode(localGeo, name) 
            ?: findGeometryNode(vanillaGeometryJson, name) 
            ?: return null
        
        return wrapGeometry(raw, name)
    }

    private fun findGeometryNode(json: JsonObject?, name: String): JsonObject? {
        if (json == null) return null
        if (json.has(name)) return json.getAsJsonObject(name)
        
        json.getAsJsonArray("minecraft:geometry")?.forEach { 
            val geo = it.asJsonObject
            if (geo.getAsJsonObject("description")?.get("identifier")?.asString == name) return geo
        }
        return null
    }

    private fun wrapGeometry(data: JsonObject, name: String): JsonObject {
        val wrapper = JsonObject()
        wrapper.addProperty("format_version", "1.12.0")
        
        val content = data.deepCopy()
        if (!content.has("description")) {
            val desc = JsonObject()
            desc.addProperty("identifier", name)
            desc.addProperty("texture_width", content.get("texturewidth")?.asInt ?: content.get("texture_width")?.asInt ?: 64)
            desc.addProperty("texture_height", content.get("textureheight")?.asInt ?: content.get("texture_height")?.asInt ?: 64)
            content.add("description", desc)
        }
        
        val arr = com.google.gson.JsonArray()
        arr.add(content)
        wrapper.add("minecraft:geometry", arr)
        return wrapper
    }

    private fun registerSkinAssets(skin: LoadedSkin) {
        if (skin.identifier != null) return

        // Texture
        loadNativeImage(skin.texture)?.let { img ->
            val id = Identifier.of("bedrockskins", "skins/${skin.safePackName}/${skin.safeSkinName}")
            MinecraftClient.getInstance().textureManager.registerTexture(id, NativeImageBackedTexture({ "bedrock_skin" }, img))
            skin.identifier = id
            println("Registered texture: $id")
        }

        // Cape
        if (skin.capeIdentifier == null && skin.cape != null) {
            loadNativeImage(skin.cape)?.let { img ->
                val id = Identifier.of("bedrockskins", "capes/${skin.safePackName}/${skin.safeSkinName}")
                MinecraftClient.getInstance().textureManager.registerTexture(id, NativeImageBackedTexture({ "bedrock_cape" }, img))
                skin.capeIdentifier = id
            }
        }
    }

    private fun loadNativeImage(source: AssetSource): NativeImage? {
        return try {
            when (source) {
                is AssetSource.Resource -> 
                    MinecraftClient.getInstance().resourceManager.getResource(source.id)
                        .orElse(null)?.inputStream?.use { NativeImage.read(it) }
                is AssetSource.File -> 
                    FileInputStream(File(source.path)).use { NativeImage.read(it) }
                is AssetSource.Remote -> null // Remote skins are pre-loaded
            }
        } catch (e: Exception) {
            println("Failed to load image ($source): $e")
            null
        }
    }

    // --- Helpers: Translations & Misc ---

    private fun loadExternalTranslations(packDir: File) {
        packDir.resolve("texts").listFiles { _, name -> name.endsWith(".lang") }?.forEach { file ->
            try {
                FileInputStream(file).use { parseTranslationStream(it, translations.computeIfAbsent(file.nameWithoutExtension.lowercase()) { mutableMapOf() }) }
            } catch (e: Exception) { println("Error loading translation ${file.name}: $e") }
        }
    }

    private fun loadInternalTranslations(manager: ResourceManager, namespace: String, packPath: String) {
        val clientLang = try { MinecraftClient.getInstance().languageManager.language.lowercase() } catch (_:Exception) { "en_us" }
        listOf(clientLang, "en_us").distinct().forEach { lang ->
            val id = Identifier.of(namespace, "$packPath/texts/$lang.lang")
            manager.getResource(id).ifPresent { res ->
                try {
                    res.inputStream.use { parseTranslationStream(it, translations.computeIfAbsent(lang) { mutableMapOf() }) }
                } catch (e: Exception) { println("Error loading internal translation $id: $e") }
            }
        }
    }

    private fun parseTranslationStream(input: InputStream, map: MutableMap<String, String>) {
        BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
            reader.mark(1)
            if (reader.read() != 0xFEFF) reader.reset() // Skip BOM
            reader.forEachLine { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    map[parts[0].trim().lowercase()] = parts[1].substringBefore("\t#").trim()
                }
            }
        }
    }

    private fun loadPackOrder(manager: ResourceManager) {
        try {
            manager.getResource(Identifier.of("bedrockskins", "order_overrides.json")).ifPresent { 
                packOrder = gson.fromJson(InputStreamReader(it.inputStream), Array<String>::class.java).toList()
            }
        } catch (_: Exception) {}
    }

    private fun validateRemoteData(key: String, data: ByteArray, geo: String): Boolean {
        if (data.size > 512 * 1024) return false // 512KB Limit
        if (data.size < 4 || data[0] != 0x89.toByte() || data[1] != 0x50.toByte() || data[2] != 0x4E.toByte()) return false
        if (geo.length > 100_000) return false
        return true
    }
}