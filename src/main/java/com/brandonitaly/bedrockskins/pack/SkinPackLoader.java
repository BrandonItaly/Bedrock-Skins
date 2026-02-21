package com.brandonitaly.bedrockskins.pack;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class SkinPackLoader {
    public static final Map<String, String> packTypesByPackId = new HashMap<>();
    public static final Map<SkinId, LoadedSkin> loadedSkins = Collections.synchronizedMap(new LinkedHashMap<>());
    public static List<String> packOrder = Collections.emptyList();

    private static final Codec<List<String>> PACK_ORDER_CODEC = Codec.list(Codec.STRING);
    private static final File skinPacksDir = new File("skin_packs");
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    private static JsonObject vanillaGeometryJson = null;

    private SkinPackLoader() {}

    //? if >=1.21.11 {
    private static Identifier createIdentifier(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
    //?} else {
    /*private static ResourceLocation createIdentifier(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }*/
    //?}

    // --- Public API ---

    public static String getTranslation(String key) {
        String currentLang = getClientLanguage();
        
        Map<String, String> currentLangMap = translations.get(currentLang);
        if (currentLangMap != null && currentLangMap.containsKey(key)) return currentLangMap.get(key);

        for (Map<String, String> map : translations.values()) {
            if (map.containsKey(key)) return map.get(key);
        }

        Map<String, String> enMap = translations.get("en_us");
        return enMap != null ? enMap.get(key) : null;
    }

    public static void loadPacks() {
        loadedSkins.clear();
        translations.clear();
        packTypesByPackId.clear();

        // Load external skin packs from skin_packs directory
        if (skinPacksDir.exists()) {
            File[] children = skinPacksDir.listFiles(File::isDirectory);
            if (children != null) {
                for (File f : children) loadExternalPack(f);
            }
        }

        // Scan resourcepacks for skin packs
        if (BedrockSkinsConfig.isScanResourcePacksForSkinsEnabled()) {
            File resourcepacksDir = getResourcepacksDir();
            if (resourcepacksDir != null) {
                Set<String> enabledPacks = new HashSet<>();
                try {
                    Minecraft client = Minecraft.getInstance();
                    if (client != null && client.getResourcePackRepository() != null) {
                        client.getResourcePackRepository().getSelectedIds().forEach(id -> 
                            enabledPacks.add(id.startsWith("file/") ? id.substring(5) : id)
                        );
                    }
                } catch (Exception ignored) {}

                File[] packs = resourcepacksDir.listFiles();
                if (packs != null) {
                    for (File pack : packs) {
                        if (enabledPacks.contains(pack.getName())) continue;

                        if (pack.isDirectory()) {
                            File[] skinPackFolders = new File(pack, "assets/bedrockskins/skin_packs").listFiles(File::isDirectory);
                            if (skinPackFolders != null) {
                                for (File folder : skinPackFolders) loadExternalPack(folder);
                            }
                        } else if (pack.isFile()) {
                            String name = pack.getName().toLowerCase(Locale.ROOT);
                            if (name.endsWith(".zip") || name.endsWith(".mcpack")) {
                                loadSkinsFromResourcePackZip(pack);
                            }
                        }
                    }
                }
            }
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        ResourceManager manager = client.getResourceManager();
        loadVanillaGeometry(manager);
        
        if (BedrockSkinsConfig.isEnableBuiltInSkinPacksEnabled()) {
            loadInternalPacks(manager);
        }
        loadPackOrder(manager);
    }

    public static void registerTextures() {
        System.out.println("SkinPackLoader: Registering all textures...");
        for (LoadedSkin s : loadedSkins.values()) registerSkinAssets(s);
    }

    public static void releaseSkinAssets(SkinId id) {
        LoadedSkin skin = getLoadedSkin(id);
        if (skin == null) return;
        
        var tm = Minecraft.getInstance().getTextureManager();
        if (skin.identifier != null) {
            tm.release(skin.identifier);
            skin.identifier = null;
        }
        if (skin.capeIdentifier != null) {
            tm.release(skin.capeIdentifier);
            skin.capeIdentifier = null;
        }
    }

    //? if >=1.21.11 {
    public static Identifier registerTextureFor(SkinId id) {
    //?} else {
    /*public static ResourceLocation registerTextureFor(SkinId id) {*/
    //?}
        LoadedSkin skin = getLoadedSkin(id);
        if (skin == null) return null;
        if (skin.getIdentifier() != null) return skin.getIdentifier();
        
        registerSkinAssets(skin);
        return skin.getIdentifier();
    }

    public static LoadedSkin getLoadedSkin(SkinId id) { 
        return id == null ? null : loadedSkins.get(id); 
    }

    public static void registerRemoteSkinStatic(String key, String geometryJson, byte[] textureData) {
        registerRemoteSkin(key, geometryJson, textureData);
    }

    public static void registerRemoteSkin(String key, String geometryJson, byte[] textureData) {
        SkinId idKey = SkinId.parse(key);
        if (loadedSkins.containsKey(idKey)) return;
        
        try {
            if (!validateRemoteData(textureData, geometryJson)) return;

            NativeImage img = NativeImage.read(new ByteArrayInputStream(textureData));
            DynamicTexture texture = new DynamicTexture(() -> "bedrock_skin_remote", img);
            var id = createIdentifier("bedrockskins", "skins/remote/" + StringUtils.sanitize(key));

            Minecraft.getInstance().getTextureManager().register(id, texture);

            LoadedSkin ls = new LoadedSkin(
                "Remote", "Remote", key,
                JsonParser.parseString(geometryJson).getAsJsonObject(),
                AssetSource.Remote.INSTANCE
            );
            ls.identifier = id;
            loadedSkins.put(idKey, ls);
            System.out.println("Registered remote skin: " + key);
        } catch (Exception e) {
            System.err.println("Failed to register remote skin " + key + ": " + e.getMessage());
        }
    }

    // --- Loading Logic ---

    private static void loadVanillaGeometry(ResourceManager manager) {
        var id = createIdentifier("bedrockskins", "skin_packs/vanilla/geometry.json");
        manager.getResource(id).ifPresent(res -> {
            try (InputStream is = res.open(); InputStreamReader r = new InputStreamReader(is)) {
                vanillaGeometryJson = JsonParser.parseReader(r).getAsJsonObject();
                System.out.println("SkinPackLoader: Loaded vanilla geometry fallback.");
            } catch (Exception e) { 
                System.err.println("SkinPackLoader: ERROR loading vanilla geometry: " + e.getMessage()); 
            }
        });
    }

    private static void loadExternalPack(File packDir) {
        File skinsFile = new File(packDir, "skins.json");
        if (!skinsFile.exists()) return;

        try {
            JsonObject geometryJson = loadJsonOrNull(new File(packDir, "geometry.json"));
            SkinPackManifest manifest = decodeManifest(new FileReader(skinsFile), skinsFile.getAbsolutePath());
            if (manifest == null) return;

            loadExternalTranslations(packDir);
            registerPackType(manifest);

            for (SkinEntry entry : manifest.getSkins()) {
                JsonObject geometry = resolveGeometry(entry.getGeometry(), geometryJson);
                if (geometry == null) continue;

                File textureFile = new File(packDir, entry.getTexture());
                if (!textureFile.exists()) continue;

                File capeFile = entry.getCape() != null ? new File(packDir, entry.getCape()) : null;
                if (capeFile != null && !capeFile.exists()) capeFile = null;

                SkinId id = SkinId.of(manifest.getSerializeName(), entry.getLocalizationName());
                loadedSkins.put(id, new LoadedSkin(
                    manifest.getSerializeName(),
                    manifest.getLocalizationName(),
                    entry.getLocalizationName(),
                    geometry,
                    new AssetSource.File(textureFile.getAbsolutePath()),
                    capeFile != null ? new AssetSource.File(capeFile.getAbsolutePath()) : null,
                    hasUpsideDownAnimation(entry)
                ));
            }
        } catch (Exception e) {
            System.err.println("SkinPackLoader: Error loading external pack from " + packDir.getName() + ": " + e);
        }
    }

    private static void loadInternalPacks(ResourceManager manager) {
        System.out.println("SkinPackLoader: Scanning resources...");
        manager.listResources("skin_packs", idt -> idt.getPath().endsWith("skins.json")).forEach((id, resource) -> {
            try {
                String packPath = id.getPath().substring(0, id.getPath().lastIndexOf('/'));
                var geoId = createIdentifier(id.getNamespace(), packPath + "/geometry.json");
                
                JsonObject geoJson = manager.getResource(geoId).map(res -> {
                    try (InputStream is = res.open(); InputStreamReader r = new InputStreamReader(is)) {
                        return JsonParser.parseReader(r).getAsJsonObject();
                    } catch (Exception e) { return null; }
                }).orElse(null);

                SkinPackManifest manifest;
                try (InputStream ris = resource.open(); InputStreamReader rr = new InputStreamReader(ris)) {
                    manifest = decodeManifest(rr, id.toString());
                }
                
                if (manifest == null) return;
                
                registerPackType(manifest);
                loadInternalTranslations(manager, id.getNamespace(), packPath);

                for (SkinEntry entry : manifest.getSkins()) {
                    JsonObject geometry = resolveGeometry(entry.getGeometry(), geoJson);
                    if (geometry == null) continue;
                    
                    var textureId = createIdentifier(id.getNamespace(), (packPath + "/" + entry.getTexture()).toLowerCase(Locale.ROOT));
                    if (manager.getResource(textureId).isEmpty()) continue;

                    //? if >=1.21.11 {
                    Identifier capeId = null;
                    //?} else {
                    /*ResourceLocation capeId = null;*/
                    //?}
                    
                    if (entry.getCape() != null) {
                        var candidate = createIdentifier(id.getNamespace(), (packPath + "/" + entry.getCape()).toLowerCase(Locale.ROOT));
                        if (manager.getResource(candidate).isPresent()) capeId = candidate;
                    }

                    SkinId skinId = SkinId.of(manifest.getSerializeName(), entry.getLocalizationName());
                    loadedSkins.put(skinId, new LoadedSkin(
                        manifest.getSerializeName(),
                        manifest.getLocalizationName(),
                        entry.getLocalizationName(),
                        geometry,
                        new AssetSource.Resource(textureId),
                        capeId != null ? new AssetSource.Resource(capeId) : null,
                        hasUpsideDownAnimation(entry)
                    ));
                }
            } catch (Exception e) {
                System.err.println("SkinPackLoader: Error loading internal pack " + id + ": " + e);
            }
        });
    }

    private static void loadSkinsFromResourcePackZip(File pack) {
        try (ZipFile zf = new ZipFile(pack)) {
            Set<String> packDirs = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zf.entries();
            
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("assets/bedrockskins/skin_packs/") && name.endsWith("/skins.json")) {
                    packDirs.add(name.substring(0, name.lastIndexOf('/')));
                }
            }

            for (String dir : packDirs) {
                try {
                    ZipEntry skinsEntry = zf.getEntry(dir + "/skins.json");
                    if (skinsEntry == null) continue;

                    JsonObject geometryJson = null;
                    ZipEntry geoEntry = zf.getEntry(dir + "/geometry.json");
                    if (geoEntry != null) {
                        try (InputStream is = zf.getInputStream(geoEntry); InputStreamReader r = new InputStreamReader(is)) {
                            geometryJson = JsonParser.parseReader(r).getAsJsonObject();
                        }
                    }

                    SkinPackManifest manifest;
                    try (InputStream is = zf.getInputStream(skinsEntry); InputStreamReader r = new InputStreamReader(is)) {
                        manifest = decodeManifest(r, skinsEntry.getName());
                    }
                    
                    if (manifest == null) continue;

                    loadExternalTranslationsFromZip(zf, dir);
                    registerPackType(manifest);

                    for (SkinEntry entry : manifest.getSkins()) {
                        JsonObject geometry = resolveGeometry(entry.getGeometry(), geometryJson);
                        if (geometry == null) continue;

                        String texPath = (dir + "/" + entry.getTexture()).toLowerCase(Locale.ROOT);
                        if (zf.getEntry(texPath) == null) continue;

                        String capePath = entry.getCape() != null ? (dir + "/" + entry.getCape()).toLowerCase(Locale.ROOT) : null;
                        ZipEntry capeEntry = capePath != null ? zf.getEntry(capePath) : null;

                        SkinId id = SkinId.of(manifest.getSerializeName(), entry.getLocalizationName());
                        loadedSkins.put(id, new LoadedSkin(
                            manifest.getSerializeName(),
                            manifest.getLocalizationName(),
                            entry.getLocalizationName(),
                            geometry,
                            new AssetSource.Zip(pack.getAbsolutePath(), texPath),
                            capeEntry != null ? new AssetSource.Zip(pack.getAbsolutePath(), capePath) : null,
                            hasUpsideDownAnimation(entry)
                        ));
                    }
                } catch (Exception e) {
                    System.err.println("Error loading skin pack from zip directory " + dir + ": " + e);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to scan resource pack zip " + pack + ": " + e);
        }
    }

    // --- Helpers: Geometry & Assets ---

    private static JsonObject resolveGeometry(String name, JsonObject localGeo) {
        JsonObject raw = findGeometryNode(localGeo, name);
        if (raw == null) raw = findGeometryNode(vanillaGeometryJson, name);
        if (raw == null) return null;
        return wrapGeometry(raw.deepCopy(), name);
    }

    private static JsonObject findGeometryNode(JsonObject json, String name) {
        if (json == null) return null;
        if (json.has(name)) return json.getAsJsonObject(name);

        JsonArray arr = json.getAsJsonArray("minecraft:geometry");
        if (arr != null) {
            for (JsonElement el : arr) {
                try {
                    JsonObject geo = el.getAsJsonObject();
                    if (name.equals(geo.getAsJsonObject("description").get("identifier").getAsString())) return geo;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static JsonObject wrapGeometry(JsonObject content, String name) {
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("format_version", "1.12.0");

        if (!content.has("description")) {
            JsonObject desc = new JsonObject();
            desc.addProperty("identifier", name);
            int texW = content.has("texturewidth") ? content.get("texturewidth").getAsInt() : 
                       content.has("texture_width") ? content.get("texture_width").getAsInt() : 64;
            int texH = content.has("textureheight") ? content.get("textureheight").getAsInt() : 
                       content.has("texture_height") ? content.get("texture_height").getAsInt() : 64;
                       
            desc.addProperty("texture_width", texW);
            desc.addProperty("texture_height", texH);
            content.add("description", desc);
        }

        JsonArray arr = new JsonArray();
        arr.add(content);
        wrapper.add("minecraft:geometry", arr);
        return wrapper;
    }

    private static void registerSkinAssets(LoadedSkin skin) {
        if (skin.identifier != null) return;

        var tm = Minecraft.getInstance().getTextureManager();

        NativeImage img = loadNativeImage(skin.getTexture());
        if (img != null) {
            skin.identifier = createIdentifier("bedrockskins", "skins/" + skin.getSafePackName() + "/" + skin.getSafeSkinName());
            tm.register(skin.identifier, new DynamicTexture(() -> "bedrock_skin", img));
        }

        if (skin.capeIdentifier == null && skin.getCape() != null) {
            NativeImage capeImg = loadNativeImage(skin.getCape());
            if (capeImg != null) {
                skin.capeIdentifier = createIdentifier("bedrockskins", "capes/" + skin.getSafePackName() + "/" + skin.getSafeSkinName());
                tm.register(skin.capeIdentifier, new DynamicTexture(() -> "bedrock_cape", capeImg));
            }
        }
    }

    private static NativeImage loadNativeImage(AssetSource source) {
        try {
            if (source instanceof AssetSource.Resource resSource) {
                Resource res = Minecraft.getInstance().getResourceManager().getResource(resSource.getId()).orElse(null);
                if (res != null) {
                    try (InputStream is = res.open()) { return NativeImage.read(is); }
                }
            } else if (source instanceof AssetSource.File fileSource) {
                try (InputStream is = new FileInputStream(fileSource.getPath())) { return NativeImage.read(is); }
            } else if (source instanceof AssetSource.Zip zs) {
                try (ZipFile zf = new ZipFile(zs.getZipPath())) {
                    ZipEntry ze = zf.getEntry(zs.getInternalPath());
                    if (ze != null) {
                        try (InputStream is = zf.getInputStream(ze)) { return NativeImage.read(is); }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load image (" + source + "): " + e.getMessage());
        }
        return null;
    }

    // --- Helpers: Translations & Misc ---

    private static void loadExternalTranslations(File packDir) {
        File[] files = new File(packDir, "texts").listFiles((dir, name) -> name.endsWith(".lang"));
        if (files == null) return;
        
        for (File file : files) {
            try (InputStream is = new FileInputStream(file)) {
                String name = file.getName();
                String lang = name.substring(0, name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
                parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new HashMap<>()));
            } catch (Exception e) { 
                System.err.println("Error loading translation " + file.getName() + ": " + e.getMessage()); 
            }
        }
    }

    private static void loadInternalTranslations(ResourceManager manager, String namespace, String packPath) {
        String clientLang = getClientLanguage();
        List<String> langs = Arrays.asList(clientLang, "en_us");
        
        for (String lang : new LinkedHashSet<>(langs)) {
            var id = createIdentifier(namespace, packPath + "/texts/" + lang + ".lang");
            manager.getResource(id).ifPresent(res -> {
                try (InputStream is = res.open()) {
                    parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new HashMap<>()));
                } catch (Exception e) { 
                    System.err.println("Error loading internal translation " + id + ": " + e.getMessage()); 
                }
            });
        }
    }

    private static void loadExternalTranslationsFromZip(ZipFile zf, String dir) {
        try {
            ZipEntry te = zf.getEntry(dir + "/texts/en_us.lang");
            if (te != null) {
                try (InputStream is = zf.getInputStream(te)) {
                    parseTranslationStream(is, translations.computeIfAbsent("en_us", k -> new HashMap<>()));
                }
            }
        } catch (Exception e) { 
            System.err.println("Error loading translations from zip: " + e.getMessage()); 
        }
    }

    private static void parseTranslationStream(InputStream input, Map<String, String> map) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            reader.mark(1);
            if (reader.read() != 0xFEFF) reader.reset(); // Skip BOM if present

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    map.put(parts[0].trim().toLowerCase(Locale.ROOT), parts[1].split("\\t#")[0].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing translation stream: " + e.getMessage());
        }
    }

    private static void loadPackOrder(ResourceManager manager) {
        try {
            manager.getResource(createIdentifier("bedrockskins", "order_overrides.json")).ifPresent(res -> {
                try (InputStream is = res.open()) {
                    packOrder = PACK_ORDER_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(is)))
                        .resultOrPartial(msg -> System.err.println("SkinPackLoader: Failed decoding order_overrides.json: " + msg))
                        .orElse(Collections.emptyList());
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private static SkinPackManifest decodeManifest(Reader reader, String source) {
        try (reader) {
            return SkinPackManifest.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                .resultOrPartial(msg -> System.err.println("SkinPackLoader: Failed decoding manifest " + source + ": " + msg))
                .orElse(null);
        } catch (Exception e) {
            System.err.println("SkinPackLoader: Error reading manifest " + source + ": " + e.getMessage());
            return null;
        }
    }
    
    private static void registerPackType(SkinPackManifest manifest) {
        String serializeName = manifest.getSerializeName();
        if (serializeName == null) return;

        String packType = manifest.getPackType();
        if ((packType == null || packType.isEmpty()) && !"Favorites".equals(serializeName) && !"Standard".equals(serializeName)) {
            packType = "skin_pack";
        }
        
        if (packType != null && !packType.isEmpty()) {
            packTypesByPackId.put("skinpack." + serializeName, packType);
        }
    }

    private static File getResourcepacksDir() {
        try {
            File resourcepacks = new File(Minecraft.getInstance().gameDirectory, "resourcepacks");
            if (resourcepacks.exists()) return resourcepacks;
        } catch (Exception ignored) {}
        return null;
    }

    private static String getClientLanguage() {
        try {
            return Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {}
        return "en_us";
    }

    private static boolean validateRemoteData(byte[] data, String geo) {
        return data.length <= 512 * 1024 
            && data.length >= 4 
            && data[0] == (byte)0x89 && data[1] == (byte)0x50 && data[2] == (byte)0x4E 
            && geo.length() <= 100_000;
    }

    private static boolean hasUpsideDownAnimation(SkinEntry entry) {
        return entry != null && entry.getAnimations() != null 
            && "animation.player.base_pose.upside_down".equals(entry.getAnimations().get("humanoid_base_pose"));
    }
    
    private static JsonObject loadJsonOrNull(File file) {
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}