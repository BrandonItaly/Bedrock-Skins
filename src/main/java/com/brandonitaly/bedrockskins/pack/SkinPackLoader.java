package com.brandonitaly.bedrockskins.pack;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.util.ExternalAssetUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public final class SkinPackLoader {
    public static final Map<String, String> packTypesByPackId = new HashMap<>();
    public static final Map<SkinId, LoadedSkin> loadedSkins = Collections.synchronizedMap(new LinkedHashMap<>());
    public static List<String> packOrder = Collections.emptyList();

    private static final Codec<List<String>> PACK_ORDER_CODEC = Codec.list(Codec.STRING);
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    private static JsonObject vanillaGeometryJson = null;

    private static final int PCK_ASSET_SKIN = 0;
    private static final int PCK_ASSET_CAPE = 1;
    private static final int PCK_ASSET_SKIN_DATA_FILE = 11;

    private SkinPackLoader() {}

    private static Identifier createIdentifier(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    // --- Public API ---

    public static String getTranslation(String key) {
        String normalizedKey = PckLocalizationSupport.cleanLocText(key).toLowerCase(Locale.ROOT);
        String currentLang = getClientLanguage();
        
        Map<String, String> currentLangMap = translations.get(currentLang);
        if (currentLangMap != null && currentLangMap.containsKey(normalizedKey)) return currentLangMap.get(normalizedKey);

        Map<String, String> enMap = translations.get("en_us");
        if (enMap != null && enMap.containsKey(normalizedKey)) return enMap.get(normalizedKey);

        for (Map<String, String> map : translations.values()) {
            if (map.containsKey(normalizedKey)) return map.get(normalizedKey);
        }

        return null;
    }

    public static void loadPacks() {
        synchronized (loadedSkins) {
            loadedSkins.values().removeIf(skin -> !(skin.getTexture() instanceof AssetSource.Remote));
        }

        translations.clear();
        packTypesByPackId.clear();

        Minecraft client = Minecraft.getInstance();
        ResourceManager manager = client.getResourceManager();
        loadVanillaGeometry(manager);

        File currentSkinPacksDir = getSkinPacksDir();
        if (currentSkinPacksDir.exists()) {
            File[] children = currentSkinPacksDir.listFiles();
            if (children != null) {
                for (File f : children) {
                    if (f.isDirectory()) loadExternalPack(f);
                    else if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".pck")) loadExternalPck(f);
                }
            }
        }

        if (BedrockSkinsConfig.isScanResourcePacksForSkinsEnabled()) {
            File resourcepacksDir = getResourcepacksDir();
            if (resourcepacksDir != null) {
                Set<String> enabledPacks = new HashSet<>();
                try {
                    client.getResourcePackRepository().getSelectedIds().forEach(id ->
                            enabledPacks.add(id.startsWith("file/") ? id.substring(5) : id)
                    );
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
                        } else if (pack.isFile() && pack.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                            loadSkinsFromResourcePackZip(pack);
                        }
                    }
                }
            }
        }
        
        if (BedrockSkinsConfig.isEnableBuiltInSkinPacksEnabled()) loadInternalPacks(manager);
        loadPackOrder(manager);
    }

    public static void registerTextures() {
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

    public static void registerTextureFor(SkinId id) {
        LoadedSkin skin = getLoadedSkin(id);
        if (skin != null && skin.getIdentifier() == null) registerSkinAssets(skin);
    }

    public static LoadedSkin getLoadedSkin(SkinId id) { 
        return id == null ? null : loadedSkins.get(id); 
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
        } catch (Exception e) {
            System.err.println("Failed to register remote skin " + key + ": " + e.getMessage());
        }
    }

    // --- Loading Logic ---

    private static void loadVanillaGeometry(ResourceManager manager) {
        var id = createIdentifier("bedrockskins", "skin_packs/vanilla/geometry.json");
        manager.getResource(id).ifPresent(res -> {
            try (InputStream is = res.open(); InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                vanillaGeometryJson = JsonParser.parseReader(r).getAsJsonObject();
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
            SkinPackManifest manifest = decodeManifest(new InputStreamReader(new FileInputStream(skinsFile), StandardCharsets.UTF_8), skinsFile.getAbsolutePath());
            if (manifest == null) return;

            loadExternalTranslations(packDir);
            registerPackType(manifest);

            for (SkinEntry entry : manifest.getSkins()) {
                JsonObject geometry = resolveGeometry(entry.getGeometry(), geometryJson);
                if (geometry == null || entry.getTexture() == null) continue;

                File textureFile = new File(packDir, entry.getTexture().toLowerCase(Locale.ROOT));
                if (!textureFile.exists()) continue;

                File capeFile = entry.getCape() != null ? new File(packDir, entry.getCape().toLowerCase(Locale.ROOT)) : null;
                if (capeFile != null && !capeFile.exists()) capeFile = null;

                SkinId id = SkinId.of(manifest.getSerializeName(), entry.getLocalizationName());
                loadedSkins.put(id, new LoadedSkin(
                    manifest.getSerializeName(), manifest.getLocalizationName(), entry.getLocalizationName(),
                    geometry, new AssetSource.File(textureFile.getAbsolutePath()),
                    capeFile != null ? new AssetSource.File(capeFile.getAbsolutePath()) : null,
                    hasUpsideDownAnimation(entry)
                ));
            }
        } catch (Exception e) {
            System.err.println("SkinPackLoader: Error loading external pack from " + packDir.getName() + ": " + e);
        }
    }

    private static void loadExternalPck(File pckFile) {
        try {
            byte[] bytes = Files.readAllBytes(pckFile.toPath());
            PckFileParser.PckArchive root = PckFileParser.parse(bytes);

            List<PckFileParser.PckAsset> allAssets = collectAllPckAssets(root);
            if (allAssets.isEmpty()) return;

            List<PckFileParser.PckAsset> skins = new ArrayList<>();
            List<PckFileParser.PckAsset> generalAssets = new ArrayList<>();
            Map<String, PckFileParser.PckAsset> capesByName = new HashMap<>();
            Map<String, PckFileParser.PckAsset> capesByBaseName = new HashMap<>();

            for (PckFileParser.PckAsset asset : allAssets) {
                if (asset == null || asset.data() == null) continue;
                
                String normPath = normalizePckPath(asset.filename());
                String baseName = stripExtension(fileNameOnly(normPath));

                if (isLikelyCapeAsset(asset, normPath, baseName)) {
                    capesByName.put(normPath, asset);
                    capesByBaseName.put(fileNameOnly(normPath), asset);
                } else if (isLikelySkinAsset(asset, normPath, baseName)) {
                    skins.add(asset);
                } else {
                    generalAssets.add(asset);
                }
            }

            String currentLang = getClientLanguage();
            Map<String, Map<String, String>> pckTranslations = PckLocalizationSupport.loadPckLocalisations(allAssets);
            String fileBaseName = stripExtension(pckFile.getName());
            String serializeName = StringUtils.sanitize(fileBaseName);
            if (serializeName.isEmpty()) serializeName = "legacy_console";

            String packDisplayToken = firstNonBlank(PckLocalizationSupport.findPackDisplayToken(pckTranslations, currentLang), firstNonBlank(getPackDisplayName(generalAssets), fileBaseName)
            );
            String packDisplayName = firstNonBlank(PckLocalizationSupport.resolvePckLocalizedToken(packDisplayToken, pckTranslations, currentLang), fileBaseName);

            packTypesByPackId.put("skinpack." + serializeName, "skin_pack");

            for (PckFileParser.PckAsset asset : skins) {
                String skinKey = stripExtension(fileNameOnly(normalizePckPath(asset.filename())));
                if (skinKey.isEmpty()) continue;

                String skinDisplayToken = firstNonBlank(getFirstPropertyValue(asset, "DISPLAYNAMEID", "IDS_DISPLAY_NAME", "LOC_KEY"), firstNonBlank(getFirstPropertyValue(asset, "DISPLAYNAME"), skinKey));
                String skinDisplayName = firstNonBlank(PckLocalizationSupport.resolvePckLocalizedToken(skinDisplayToken, pckTranslations, currentLang), skinKey);

                String skinThemeToken = PckLocalizationSupport.deriveSkinThemeToken(asset, skinDisplayToken, skinKey, pckTranslations, currentLang);
                String skinTheme = PckLocalizationSupport.resolvePckLocalizedToken(skinThemeToken, pckTranslations, currentLang);
                if (skinThemeToken != null && skinTheme != null && skinTheme.equalsIgnoreCase(PckLocalizationSupport.cleanLocText(skinThemeToken))) {
                    skinTheme = null;
                }

                Long animMask = PckModelConverter.parseAnimMask(asset);
                boolean slim = PckModelConverter.isSlim(animMask);
                boolean upsideDown = PckModelConverter.isUpsideDown(animMask);
                String geometryName = slim ? "geometry.humanoid.customSlim" : "geometry.humanoid.custom";

                JsonObject geometry = PckModelConverter.applyPckDataToGeometry(asset, resolveGeometry(geometryName, null), animMask);
                if (geometry == null) continue;

                AssetSource capeSource = null;
                String capePath = getFirstPropertyValue(asset, "CAPEPATH");
                if (capePath != null && !capePath.isBlank()) {
                    PckFileParser.PckAsset capeAsset = resolveCape(capesByName, capesByBaseName, capePath);
                    if (capeAsset != null) capeSource = new AssetSource.Bytes(capeAsset.data(), pckFile.getName() + ":" + capeAsset.filename());
                } else {
                    String inferredCapeName = inferCapeNameFromSkin(skinKey);
                    if (inferredCapeName != null) {
                        PckFileParser.PckAsset capeAsset = resolveCape(capesByName, capesByBaseName, inferredCapeName);
                        if (capeAsset != null) capeSource = new AssetSource.Bytes(capeAsset.data(), pckFile.getName() + ":" + capeAsset.filename());
                    }
                }

                SkinId id = SkinId.of(serializeName, skinKey);
                String safeSkinTranslationKey = StringUtils.sanitize("skin." + packDisplayName + "." + skinKey);
                String safePackTranslationKey = StringUtils.sanitize("skinpack." + packDisplayName);

                PckLocalizationSupport.copyLocalizedValueToTranslations(packDisplayToken, safePackTranslationKey, packDisplayName, pckTranslations, translations);
                PckLocalizationSupport.copyLocalizedValueToTranslations(skinDisplayToken, safeSkinTranslationKey, skinDisplayName, pckTranslations, translations);
                
                LoadedSkin loadedSkin = new LoadedSkin(
                    serializeName, packDisplayName, skinKey, geometry,
                    new AssetSource.Bytes(asset.data(), pckFile.getName() + ":" + asset.filename()),
                    capeSource, upsideDown
                );
                
                if (skinTheme != null && !skinTheme.isBlank()) {
                    PckLocalizationSupport.copyLocalizedValueToTranslations(skinThemeToken, loadedSkin.getSafeSkinName() + ".description", skinTheme, pckTranslations, translations);
                }

                loadedSkins.put(id, loadedSkin);
            }
        } catch (Exception e) {
            System.err.println("SkinPackLoader: Error loading PCK skin pack " + pckFile.getName() + ": " + e.getMessage());
        }
    }

    private static void loadInternalPacks(ResourceManager manager) {
        manager.listResources("skin_packs", idt -> idt.getPath().endsWith("skins.json")).forEach((id, resource) -> {
            try {
                String packPath = id.getPath().substring(0, id.getPath().lastIndexOf('/'));
                var geoId = createIdentifier(id.getNamespace(), packPath + "/geometry.json");
                
                JsonObject geoJson = manager.getResource(geoId).map(res -> {
                    try (InputStream is = res.open(); InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        return JsonParser.parseReader(r).getAsJsonObject();
                    } catch (Exception e) { return null; }
                }).orElse(null);

                SkinPackManifest manifest;
                try (InputStream ris = resource.open(); InputStreamReader rr = new InputStreamReader(ris, StandardCharsets.UTF_8)) {
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

                    Identifier capeId = null;
                    if (entry.getCape() != null) {
                        var candidate = createIdentifier(id.getNamespace(), (packPath + "/" + entry.getCape()).toLowerCase(Locale.ROOT));
                        if (manager.getResource(candidate).isPresent()) capeId = candidate;
                    }

                    SkinId skinId = SkinId.of(manifest.getSerializeName(), entry.getLocalizationName());
                    loadedSkins.put(skinId, new LoadedSkin(
                        manifest.getSerializeName(), manifest.getLocalizationName(), entry.getLocalizationName(),
                        geometry, new AssetSource.Resource(textureId),
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
                        try (InputStream is = zf.getInputStream(geoEntry); InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                            geometryJson = JsonParser.parseReader(r).getAsJsonObject();
                        }
                    }

                    SkinPackManifest manifest;
                    try (InputStream is = zf.getInputStream(skinsEntry); InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
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
                            manifest.getSerializeName(), manifest.getLocalizationName(), entry.getLocalizationName(),
                            geometry, new AssetSource.Zip(pack.getAbsolutePath(), texPath),
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
        img = convertLegacySkinIfNeeded(img, skin);
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
        byte[] data = ExternalAssetUtil.loadTextureData(source, Minecraft.getInstance());
        if (data.length == 0) return null;
        
        try {
            return NativeImage.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            return null;
        }
    }

    private static NativeImage convertLegacySkinIfNeeded(NativeImage image, LoadedSkin skin) {
        if (image == null || skin == null || !usesDefaultHumanoidGeometry(skin)) return image;

        int width = image.getWidth();
        int height = image.getHeight();
        if (width != 64 || height != 32) return image;

        NativeImage converted = new NativeImage(64, 64, true);
        try {
            converted.copyFrom(image);
            converted.fillRect(0, 32, 64, 32, 0);

            converted.copyRect(4, 16, 16, 32, 4, 4, true, false);
            converted.copyRect(8, 16, 16, 32, 4, 4, true, false);
            converted.copyRect(0, 20, 24, 32, 4, 12, true, false);
            converted.copyRect(4, 20, 16, 32, 4, 12, true, false);
            converted.copyRect(8, 20, 8, 32, 4, 12, true, false);
            converted.copyRect(12, 20, 16, 32, 4, 12, true, false);
            converted.copyRect(44, 16, -8, 32, 4, 4, true, false);
            converted.copyRect(48, 16, -8, 32, 4, 4, true, false);
            converted.copyRect(40, 20, 0, 32, 4, 12, true, false);
            converted.copyRect(44, 20, -8, 32, 4, 12, true, false);
            converted.copyRect(48, 20, -16, 32, 4, 12, true, false);
            converted.copyRect(52, 20, -8, 32, 4, 12, true, false);

            image.close();
            return converted;
        } catch (Exception e) {
            converted.close();
            return image;
        }
    }

    private static boolean usesDefaultHumanoidGeometry(LoadedSkin skin) {
        try {
            JsonObject data = skin.getGeometryData();
            if (data == null) return false;

            JsonArray arr = data.getAsJsonArray("minecraft:geometry");
            if (arr == null || arr.isEmpty()) return false;

            JsonObject geo = arr.get(0).getAsJsonObject();
            JsonObject desc = geo.getAsJsonObject("description");
            if (desc == null || !desc.has("identifier")) return false;

            String identifier = desc.get("identifier").getAsString();
            return "geometry.humanoid.custom".equals(identifier) || "geometry.humanoid.customSlim".equals(identifier);
        } catch (Exception ignored) {
            return false;
        }
    }

    // --- Helpers: Translations & Misc ---

    private static void loadExternalTranslations(File packDir) {
        File[] files = new File(packDir, "texts").listFiles((dir, name) -> name.endsWith(".lang"));
        if (files == null) return;
        
        for (File file : files) {
            try (InputStream is = new FileInputStream(file)) {
                String lang = file.getName().substring(0, file.getName().lastIndexOf('.')).toLowerCase(Locale.ROOT);
                parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new HashMap<>()));
            } catch (Exception ignored) {}
        }
    }

    private static void loadInternalTranslations(ResourceManager manager, String namespace, String packPath) {
        List<String> langs = Arrays.asList(getClientLanguage(), "en_us");
        for (String lang : new LinkedHashSet<>(langs)) {
            manager.getResource(createIdentifier(namespace, packPath + "/texts/" + lang + ".lang")).ifPresent(res -> {
                try (InputStream is = res.open()) {
                    parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new HashMap<>()));
                } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
    }

    private static void parseTranslationStream(InputStream input, Map<String, String> map) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            reader.mark(1);
            if (reader.read() != 0xFEFF) reader.reset();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    map.put(parts[0].trim().toLowerCase(Locale.ROOT), parts[1].split("\\t#")[0].trim());
                }
            }
        } catch (Exception ignored) {}
    }

    private static void loadPackOrder(ResourceManager manager) {
        try {
            manager.getResource(createIdentifier("bedrockskins", "order_overrides.json")).ifPresent(res -> {
                try (InputStream is = res.open(); InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    packOrder = PACK_ORDER_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(r))
                        .resultOrPartial(msg -> {})
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

    private static File getSkinPacksDir() {
        try {
            return new File(Minecraft.getInstance().gameDirectory, "skin_packs");
        } catch (Exception ignored) {}
        return new File("skin_packs"); 
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
        return data.length <= 512 * 1024 && data.length >= 4 
            && data[0] == (byte)0x89 && data[1] == (byte)0x50 && data[2] == (byte)0x4E 
            && geo.length() <= 100_000;
    }

    private static boolean hasUpsideDownAnimation(SkinEntry entry) {
        return entry != null && entry.getAnimations() != null 
            && "animation.player.base_pose.upside_down".equals(entry.getAnimations().get("humanoid_base_pose"));
    }

    private static PckFileParser.PckAsset resolveCape(Map<String, PckFileParser.PckAsset> capesByName, Map<String, PckFileParser.PckAsset> capesByBaseName, String capePath) {
        String normalized = normalizePckPath(capePath);
        PckFileParser.PckAsset cape = capesByName.get(normalized);
        if (cape != null) return cape;

        return capesByBaseName.get(fileNameOnly(normalized));
    }

    private static List<PckFileParser.PckAsset> collectAllPckAssets(PckFileParser.PckArchive root) {
        if (root == null || root.assets() == null) return List.of();

        List<PckFileParser.PckAsset> out = new ArrayList<>();
        Set<Long> visitedPayloads = new HashSet<>();
        Deque<Map.Entry<PckFileParser.PckArchive, Integer>> stack = new ArrayDeque<>();
        stack.push(Map.entry(root, 0));

        while (!stack.isEmpty()) {
            Map.Entry<PckFileParser.PckArchive, Integer> node = stack.pop();
            PckFileParser.PckArchive archive = node.getKey();
            int depth = node.getValue();
            
            if (archive.assets() == null) continue;

            for (PckFileParser.PckAsset asset : archive.assets()) {
                out.add(asset);

                if (depth >= 4 || !isLikelyNestedPck(asset)) continue;

                long hash = fastHash(asset.data());
                if (!visitedPayloads.add(hash)) continue;

                try {
                    PckFileParser.PckArchive nested = PckFileParser.parse(asset.data());
                    stack.push(Map.entry(nested, depth + 1));
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    private static boolean isLikelyNestedPck(PckFileParser.PckAsset asset) {
        if (asset == null || asset.data() == null || asset.data().length < 16) return false;
        if (asset.type() == PCK_ASSET_SKIN_DATA_FILE) return true;
        
        String name = normalizePckPath(asset.filename());
        return name.endsWith(".pck") || name.contains("skins.pck");
    }

    private static boolean isLikelySkinAsset(PckFileParser.PckAsset asset, String normPath, String baseName) {
        if (!normPath.endsWith(".png")) return false;
        if (asset.type() == PCK_ASSET_SKIN) return true;

        if (baseName.startsWith("dlcskin") || baseName.startsWith("skin")) return true;

        return getFirstPropertyValue(asset, "ANIM", "DISPLAYNAME", "DISPLAYNAMEID", "LOC_KEY") != null;
    }

    private static boolean isLikelyCapeAsset(PckFileParser.PckAsset asset, String normPath, String baseName) {
        if (!normPath.endsWith(".png")) return false;
        if (asset.type() == PCK_ASSET_CAPE) return true;

        return baseName.startsWith("dlccape") || baseName.startsWith("cape");
    }

    private static String inferCapeNameFromSkin(String skinKey) {
        if (skinKey == null || skinKey.isBlank()) return null;
        String lower = skinKey.toLowerCase(Locale.ROOT);
        return lower.startsWith("dlcskin") ? "dlccape" + skinKey.substring(7) + ".png" : null;
    }

    private static String getPackDisplayName(List<PckFileParser.PckAsset> generalAssets) {
        if (generalAssets == null || generalAssets.isEmpty()) return null;

        for (PckFileParser.PckAsset asset : generalAssets) {
            String fromIdsDisplayName = getFirstPropertyValue(asset, "IDS_DISPLAY_NAME", "DISPLAYNAMEID");
            if (fromIdsDisplayName != null && !fromIdsDisplayName.isBlank()) return fromIdsDisplayName;
        }
        return null;
    }

    private static long fastHash(byte[] data) {
        if (data == null || data.length == 0) return 0;
        CRC32 crc = new CRC32();
        crc.update(data, 0, Math.min(data.length, 1024));
        return ((long) data.length << 32) | crc.getValue();
    }

    private static String getFirstPropertyValue(PckFileParser.PckAsset asset, String... keys) {
        if (asset == null || asset.properties() == null || keys == null) return null;

        for (String key : keys) {
            if (key == null) continue;
            for (Map.Entry<String, String> property : asset.properties()) {
                if (key.equalsIgnoreCase(property.getKey())) return property.getValue();
            }
        }
        return null;
    }

    private static String stripExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(0, idx) : name;
    }

    private static String fileNameOnly(String path) {
        if (path == null) return "";
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static String normalizePckPath(String path) {
        if (path == null) return "";
        if (path.indexOf('\\') >= 0) path = path.replace('\\', '/');
        
        path = path.trim();
        int start = 0;
        while (start < path.length() && path.charAt(start) == '/') start++;
        
        return (start > 0 ? path.substring(start) : path).toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : (fallback == null ? "" : fallback);
    }
    
    private static JsonObject loadJsonOrNull(File file) {
        if (!file.exists()) return null;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}