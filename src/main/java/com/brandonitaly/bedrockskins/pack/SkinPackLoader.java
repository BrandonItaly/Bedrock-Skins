package com.brandonitaly.bedrockskins.pack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    // Map to store packType by packId
    public static final Map<String, String> packTypesByPackId = new HashMap<>();
    private SkinPackLoader() {}

    private static JsonObject vanillaGeometryJson = null;
    private static final Gson gson = new Gson();

    public static final Map<SkinId, LoadedSkin> loadedSkins = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final File skinPacksDir = new File("skin_packs");
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    public static List<String> packOrder = Collections.emptyList();

    // Helper method for version-specific identifier creation
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
        String currentLang = "en_us";
        try {
            currentLang = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception ignored) {}

        Map<String, String> map = translations.get(currentLang);
        if (map != null && map.containsKey(key)) return map.get(key);

        for (Map<String, String> m : translations.values()) {
            if (m.containsKey(key)) return m.get(key);
        }

        Map<String, String> en = translations.get("en_us");
        return en != null ? en.get(key) : null;
    }

    public static void loadPacks() {
        loadedSkins.clear();
        translations.clear();
        packTypesByPackId.clear();

        // PMO was here
        Minecraft client = null;
        try { client = Minecraft.getInstance(); } catch (Exception e) { /* continue anyway */ }
        if (client != null) {
            ResourceManager manager = client.getResourceManager();
            loadVanillaGeometry(manager);
        }

        // Load external skin packs from skin_packs directory
        if (skinPacksDir.exists()) {
            File[] children = skinPacksDir.listFiles();
            if (children != null) {
                for (File f : children) {
                    if (f.isDirectory()) loadExternalPack(f);
                }
            }
        }

        // Optionally scan resourcepacks for skin packs
        if (com.brandonitaly.bedrockskins.client.BedrockSkinsConfig.isScanResourcePacksForSkinsEnabled()) {
            File resourcepacksDir = getResourcepacksDir();
            if (resourcepacksDir != null && resourcepacksDir.exists()) {
                File[] packs = resourcepacksDir.listFiles();
                if (packs != null) {
                    for (File pack : packs) {
                        if (pack.isDirectory()) {
                            // Look for skin packs inside resource pack folders
                            File assetsDir = new File(pack, "assets");
                            if (assetsDir.exists()) {
                                File bedrockskinsDir = new File(assetsDir, "bedrockskins");
                                File skinPacksDir = new File(bedrockskinsDir, "skin_packs");
                                if (skinPacksDir.exists()) {
                                    File[] skinPackFolders = skinPacksDir.listFiles();
                                    if (skinPackFolders != null) {
                                        for (File skinPackFolder : skinPackFolders) {
                                            if (skinPackFolder.isDirectory()) {
                                                loadExternalPack(skinPackFolder);
                                            }
                                        }
                                    }
                                }
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

        // Continue with remaining loading if we have a client
        if (client != null) {
            ResourceManager manager = client.getResourceManager();
            loadInternalPacks(manager);
            loadPackOrder(manager);
        }
    }

    private static File getResourcepacksDir() {
        try {
            File gameDir = Minecraft.getInstance().gameDirectory;
            File resourcepacks = new File(gameDir, "resourcepacks");
            if (resourcepacks.exists()) return resourcepacks;
        } catch (Exception ignored) {}
        return null;
    }

    public static void registerTextures() {
        System.out.println("SkinPackLoader: Registering all textures...");
        for (LoadedSkin s : loadedSkins.values()) registerSkinAssets(s);
    }


    //? if >=1.21.11 {
    public static Identifier registerTextureFor(SkinId id) {
    //?} else {
    /*public static ResourceLocation registerTextureFor(SkinId id) {*/
    //?}
        LoadedSkin skin = id == null ? null : loadedSkins.get(id);
        if (skin == null) return null;
        if (skin.getIdentifier() != null) return skin.getIdentifier();
        registerSkinAssets(skin);
        return skin.getIdentifier();
    }

    // Helper to lookup by SkinId
    public static LoadedSkin getLoadedSkin(SkinId id) { return id == null ? null : loadedSkins.get(id); }

    public static void registerRemoteSkinStatic(String key, String geometryJson, byte[] textureData) {
        registerRemoteSkin(key, geometryJson, textureData);
    }

    public static void registerRemoteSkin(String key, String geometryJson, byte[] textureData) {
        SkinId idKey = SkinId.parse(key);
        if (loadedSkins.containsKey(idKey)) return;
        try {
            if (!validateRemoteData(key, textureData, geometryJson)) return;

            NativeImage img = NativeImage.read(new ByteArrayInputStream(textureData));
            DynamicTexture texture = new DynamicTexture(() -> "bedrock_skin_remote", img);
            String safeKey = StringUtils.sanitize(key);
            var id = createIdentifier("bedrockskins", "skins/remote/" + safeKey);

            Minecraft.getInstance().getTextureManager().register(id, texture);

            LoadedSkin ls = new LoadedSkin(
                "Remote",
                "Remote",
                key,
                JsonParser.parseString(geometryJson).getAsJsonObject(),
                AssetSource.Remote.INSTANCE
            );
            ls.identifier = id;
            loadedSkins.put(idKey, ls);
            System.out.println("Registered remote skin: " + key);
        } catch (Exception e) {
            System.out.println("Failed to register remote skin " + key + ": " + e);
        }
    }

    // --- Loading Logic ---

    private static void loadVanillaGeometry(ResourceManager manager) {
        try {
            var id = createIdentifier("bedrockskins", "skin_packs/vanilla/geometry.json");
            manager.getResource(id).ifPresent(res -> {
                try (InputStream is = res.open(); InputStreamReader r = new InputStreamReader(is)) {
                    vanillaGeometryJson = JsonParser.parseReader(r).getAsJsonObject();
                    System.out.println("SkinPackLoader: Loaded vanilla geometry fallback.");
                } catch (Exception e) { System.out.println("SkinPackLoader: ERROR loading vanilla geometry: " + e.getMessage()); }
            });
        } catch (Exception e) {
            System.out.println("SkinPackLoader: ERROR loading vanilla geometry: " + e.getMessage());
        }
    }

    private static void loadExternalPack(File packDir) {
        File skinsFile = new File(packDir, "skins.json");
        if (!skinsFile.exists()) return;

        try {
            JsonObject geometryJson = null;
            File geoFile = new File(packDir, "geometry.json");
            if (geoFile.exists()) geometryJson = JsonParser.parseReader(new FileReader(geoFile)).getAsJsonObject();

            SkinPackManifest manifest = gson.fromJson(new FileReader(skinsFile), SkinPackManifest.class);
            loadExternalTranslations(packDir);

            // Store packType by packId (serializeName), default to 'skin_pack' except for Favorites and Standard
            if (manifest.getSerializeName() != null) {
                String packId = "skinpack." + manifest.getSerializeName();
                String packType = manifest.getPackType();
                if (packType == null || packType.isEmpty()) {
                    if (!"Favorites".equals(manifest.getSerializeName()) && !"Standard".equals(manifest.getSerializeName())) {
                        packType = "skin_pack";
                    }
                }
                if (packType != null) {
                    packTypesByPackId.put(packId, packType);
                }
            }

            for (SkinEntry entry : manifest.getSkins()) {
                JsonObject geometry = resolveGeometry(entry.getGeometry(), geometryJson);
                if (geometry == null) continue;

                File textureFile = new File(packDir, entry.getTexture());
                File capeFile = entry.getCape() != null ? new File(packDir, entry.getCape()) : null;
                if (capeFile != null && !capeFile.exists()) capeFile = null;

                if (textureFile.exists()) {
                    SkinId id = SkinId.of(manifest.getSerializeName(), entry.getLocalizationName());
                    loadedSkins.put(id, new LoadedSkin(
                        manifest.getSerializeName(),
                        manifest.getLocalizationName(),
                        entry.getLocalizationName(),
                        geometry,
                        new AssetSource.File(textureFile.getAbsolutePath()),
                        capeFile != null ? new AssetSource.File(capeFile.getAbsolutePath()) : null
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadInternalPacks(ResourceManager manager) {
        System.out.println("SkinPackLoader: Scanning resources...");
        manager.listResources("skin_packs", idt -> idt.getPath().endsWith("skins.json")).forEach((id, resource) -> {
            try {
                var geoId = createIdentifier(id.getNamespace(), id.getPath().replace("skins.json", "geometry.json"));
                JsonObject geoJson = manager.getResource(geoId).map(res -> {
                    try (InputStream is = res.open(); InputStreamReader r = new InputStreamReader(is)) {
                        return JsonParser.parseReader(r).getAsJsonObject();
                    } catch (Exception e) { return null; }
                }).orElse(null);

                SkinPackManifest manifest;
                try (InputStream ris = resource.open(); InputStreamReader rr = new InputStreamReader(ris)) {
                    manifest = gson.fromJson(rr, SkinPackManifest.class);
                }
                // Store packType by packId (serializeName) for internal packs, default to 'skin_pack' except for Favorites and Standard
                if (manifest.getSerializeName() != null) {
                    String packId = "skinpack." + manifest.getSerializeName();
                    String packType = manifest.getPackType();
                    if (packType == null || packType.isEmpty()) {
                        if (!"Favorites".equals(manifest.getSerializeName()) && !"Standard".equals(manifest.getSerializeName())) {
                            packType = "skin_pack";
                        }
                    }
                    if (packType != null) {
                        packTypesByPackId.put(packId, packType);
                    }
                }
                String packPath = id.getPath().substring(0, id.getPath().lastIndexOf('/'));

                loadInternalTranslations(manager, id.getNamespace(), packPath);

                for (SkinEntry entry : manifest.getSkins()) {
                    JsonObject geometry = resolveGeometry(entry.getGeometry(), geoJson);
                    if (geometry == null) continue;
                    var textureId = createIdentifier(id.getNamespace(), (packPath + "/" + entry.getTexture()).toLowerCase(Locale.ROOT));

                    if (manager.getResource(textureId).isPresent()) {
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
                            capeId != null ? new AssetSource.Resource(capeId) : null
                        ));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // --- Helpers: Geometry & Assets ---

    private static JsonObject resolveGeometry(String name, JsonObject localGeo) {
        JsonObject raw = findGeometryNode(localGeo, name);
        if (raw == null) {
            // Try vanilla geometry as fallback
            if (vanillaGeometryJson != null) {
                raw = findGeometryNode(vanillaGeometryJson, name);
            } else {
                System.out.println("SkinPackLoader: Warning - vanilla geometry not loaded yet when resolving: " + name);
            }
        }
        if (raw == null) {
            System.out.println("SkinPackLoader: Failed to resolve geometry: " + name);
            return null;
        }
        return wrapGeometry(raw.deepCopy(), name);
    }

    private static JsonObject findGeometryNode(JsonObject json, String name) {
        if (json == null) return null;
        if (json.has(name)) return json.getAsJsonObject(name);

        JsonArray arr = json.getAsJsonArray("minecraft:geometry");
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                try {
                    JsonObject geo = arr.get(i).getAsJsonObject();
                    if (geo.getAsJsonObject("description").get("identifier").getAsString().equals(name)) return geo;
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
            int texW = 64;
            int texH = 64;
            try {
                texW = content.has("texturewidth") ? content.get("texturewidth").getAsInt() : content.has("texture_width") ? content.get("texture_width").getAsInt() : 64;
                texH = content.has("textureheight") ? content.get("textureheight").getAsInt() : content.has("texture_height") ? content.get("texture_height").getAsInt() : 64;
            } catch (Exception ignored) {}
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

        // Texture
        NativeImage img = loadNativeImage(skin.getTexture());
        if (img != null) {
            var id = createIdentifier("bedrockskins", "skins/" + skin.getSafePackName() + "/" + skin.getSafeSkinName());
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "bedrock_skin", img));
            skin.identifier = id;
            System.out.println("Registered texture: " + id);
        }

        // Cape
        if (skin.capeIdentifier == null && skin.getCape() != null) {
            NativeImage capeImg = loadNativeImage(skin.getCape());
            if (capeImg != null) {
                var id = createIdentifier("bedrockskins", "capes/" + skin.getSafePackName() + "/" + skin.getSafeSkinName());
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "bedrock_cape", capeImg));
                skin.capeIdentifier = id;
                System.out.println("Registered texture: " + id);
            }
        }
    }

    private static NativeImage loadNativeImage(AssetSource source) {
        try {
            if (source instanceof AssetSource.Resource) {
                Resource res = Minecraft.getInstance().getResourceManager().getResource(((AssetSource.Resource) source).getId()).orElse(null);
                if (res == null) return null;
                try (InputStream is = res.open()) {
                    return NativeImage.read(is);
                }
            } else if (source instanceof AssetSource.File) {
                try (InputStream is = new FileInputStream(new File(((AssetSource.File) source).getPath()))) {
                    return NativeImage.read(is);
                }
            } else if (source instanceof AssetSource.Zip) {
                AssetSource.Zip zs = (AssetSource.Zip) source;
                try (ZipFile zf = new ZipFile(zs.getZipPath())) {
                    ZipEntry ze = zf.getEntry(zs.getInternalPath());
                    if (ze == null) return null;
                    try (InputStream is = zf.getInputStream(ze)) {
                        return NativeImage.read(is);
                    }
                }
            } else {
                return null; // Remote already pre-loaded
            }
        } catch (Exception e) {
            System.out.println("Failed to load image (" + source + "): " + e);
            return null;
        }
    }

    private static void loadSkinsFromResourcePackZip(File pack) {
        try (ZipFile zf = new ZipFile(pack)) {
            // Find all skins.json files under assets/bedrockskins/skin_packs/<packName>/skins.json
            Enumeration<? extends ZipEntry> entries = zf.entries();
            Set<String> packDirs = new HashSet<>();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (name.startsWith("assets/bedrockskins/skin_packs/") && name.endsWith("/skins.json")) {
                    String dir = name.substring(0, name.lastIndexOf('/'));
                    packDirs.add(dir);
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
                        manifest = gson.fromJson(r, SkinPackManifest.class);
                    }

                    loadExternalTranslationsFromZip(zf, dir);

                    if (manifest.getSerializeName() != null) {
                        String packId = "skinpack." + manifest.getSerializeName();
                        String packType = manifest.getPackType();
                        if (packType == null || packType.isEmpty()) {
                            if (!"Favorites".equals(manifest.getSerializeName()) && !"Standard".equals(manifest.getSerializeName())) {
                                packType = "skin_pack";
                            }
                        }
                        if (packType != null) packTypesByPackId.put(packId, packType);
                    }

                    for (SkinEntry entry : manifest.getSkins()) {
                        JsonObject geometry = resolveGeometry(entry.getGeometry(), geometryJson);
                        if (geometry == null) continue;

                        String texPath = (dir + "/" + entry.getTexture()).toLowerCase(Locale.ROOT);
                        ZipEntry texEntry = zf.getEntry(texPath);
                        String capePath = entry.getCape() != null ? (dir + "/" + entry.getCape()).toLowerCase(Locale.ROOT) : null;
                        ZipEntry capeEntry = capePath != null ? zf.getEntry(capePath) : null;

                        if (texEntry != null) {
                            SkinId id = SkinId.of(manifest.getSerializeName(), entry.getLocalizationName());
                            loadedSkins.put(id, new LoadedSkin(
                                manifest.getSerializeName(),
                                manifest.getLocalizationName(),
                                entry.getLocalizationName(),
                                geometry,
                                new AssetSource.Zip(pack.getAbsolutePath(), texPath),
                                capeEntry != null ? new AssetSource.Zip(pack.getAbsolutePath(), capePath) : null
                            ));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error loading skin pack from zip: " + e);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to scan resource pack zip " + pack + ": " + e);
        }
    }

    private static void loadExternalTranslationsFromZip(ZipFile zf, String dir) {
        // texts/<lang>.lang under dir
        try {
            for (String lang : new String[] {"en_us"}) {
                String base = dir + "/texts/" + lang + ".lang";
                ZipEntry te = zf.getEntry(base);
                if (te != null) {
                    try (InputStream is = zf.getInputStream(te)) {
                        parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new HashMap<>()));
                    }
                }
            }
        } catch (Exception e) { System.out.println("Error loading translations from zip: " + e); }
    }

    // --- Helpers: Translations & Misc ---

    private static void loadExternalTranslations(File packDir) {
        File texts = new File(packDir, "texts");
        File[] files = texts.listFiles((dir, name) -> name.endsWith(".lang"));
        if (files == null) return;
        for (File file : files) {
            try (InputStream is = new FileInputStream(file)) {
                parseTranslationStream(is, translations.computeIfAbsent(file.getName().replaceFirst("\\.[^.]+$", "").toLowerCase(Locale.ROOT), k -> new HashMap<>()));
            } catch (Exception e) { System.out.println("Error loading translation " + file.getName() + ": " + e); }
        }
    }

    private static void loadInternalTranslations(ResourceManager manager, String namespace, String packPath) {
        String clientLang = "en_us";
        try { clientLang = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT); } catch (Exception ignored) {}
        List<String> langs = Arrays.asList(clientLang, "en_us");
        for (String lang : new LinkedHashSet<>(langs)) {
            var id = createIdentifier(namespace, packPath + "/texts/" + lang + ".lang");
            manager.getResource(id).ifPresent(res -> {
                try (InputStream is = res.open()) {
                    parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new HashMap<>()));
                } catch (Exception e) { System.out.println("Error loading internal translation " + id + ": " + e); }
            });
        }
    }

    private static void parseTranslationStream(InputStream input, Map<String, String> map) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            reader.mark(1);
            int first = reader.read();
            if (first != 0xFEFF) reader.reset(); // Skip BOM if present

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim().toLowerCase(Locale.ROOT);
                    String val = parts[1].split("\\t#")[0].trim();
                    map.put(key, val);
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing translation stream: " + e);
        }
    }

    private static void loadPackOrder(ResourceManager manager) {
        try {
            manager.getResource(createIdentifier("bedrockskins", "order_overrides.json")).ifPresent(res -> {
                try (InputStream is = res.open()) {
                    String[] arr = gson.fromJson(new InputStreamReader(is), String[].class);
                    packOrder = Arrays.asList(arr);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private static boolean validateRemoteData(String key, byte[] data, String geo) {
        if (data.length > 512 * 1024) return false;
        if (data.length < 4 || data[0] != (byte)0x89 || data[1] != (byte)0x50 || data[2] != (byte)0x4E) return false;
        if (geo.length() > 100_000) return false;
        return true;
    }
}
