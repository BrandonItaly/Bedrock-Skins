package com.brandonitaly.bedrockskins.pack;

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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public final class SkinPackLoader {
    public static final Map<String, String> packTypesByPackId = new ConcurrentHashMap<>();
    public static final Map<String, Identifier> packIconsByPackId = new ConcurrentHashMap<>();
    public static final Map<SkinId, LoadedSkin> loadedSkins = Collections.synchronizedMap(new LinkedHashMap<>());
    public static volatile List<String> packOrder = Collections.emptyList();

    private static final Codec<List<String>> PACK_ORDER_CODEC = Codec.list(Codec.STRING);
    private static final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();
    private static final Set<Identifier> dynamicPackIcons = ConcurrentHashMap.newKeySet();
    public static JsonObject vanillaGeometryJson = null;

    private SkinPackLoader() {}

    private static Identifier createIdentifier(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private static Identifier parseAssetPath(String path) {
        if (path == null || path.isEmpty()) return null;
        int colonIndex = path.indexOf(':');
        if (colonIndex > 0 && colonIndex < path.length() - 1) {
            try {
                return createIdentifier(path.substring(0, colonIndex), path.substring(colonIndex + 1));
            } catch (Exception ignored) {}
        }
        return null;
    }

    // --- Public API ---

    public static String getTranslation(String key) {
        String normalizedKey = PckLocalizationSupport.cleanLocText(key).toLowerCase(Locale.ROOT);
        
        String result = getLangValue(getClientLanguage(), normalizedKey);
        if (result != null) return result;

        result = getLangValue("en_us", normalizedKey);
        if (result != null) return result;

        for (Map<String, String> map : translations.values()) {
            String val = map.get(normalizedKey);
            if (val != null) return val;
        }
        return null;
    }

    private static String getLangValue(String lang, String key) {
        Map<String, String> map = translations.get(lang);
        return map != null ? map.get(key) : null;
    }

    public static void loadPacks() {
        loadedSkins.values().removeIf(skin -> !(skin.texture instanceof AssetSource.Remote));
        translations.clear();
        packTypesByPackId.clear();
        clearPackIcons();

        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        loadVanillaGeometry(manager);

        File currentSkinPacksDir = getSkinPacksDir();
        if (currentSkinPacksDir.exists()) {
            File[] children = currentSkinPacksDir.listFiles();
            if (children != null) {
                // Convert .pck files to standard folders
                for (File f : children) {
                    if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".pck")) {
                        File outputDir = new File(currentSkinPacksDir, PckImporter.stripExtension(f.getName()));
                        if (PckImporter.importPck(f, outputDir)) f.delete();
                    }
                }
                for (File f : Objects.requireNonNull(currentSkinPacksDir.listFiles())) {
                    if (f.isDirectory()) loadExternalPack(f);
                }
            }
        }

        loadInternalPacks(manager);
        loadPackOrder(manager);
    }

    public static void registerTextures() {
        synchronized (loadedSkins) {
            loadedSkins.values().forEach(SkinPackLoader::registerSkinAssets);
        }
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
        if (skin != null && skin.identifier == null) registerSkinAssets(skin);
    }

    public static LoadedSkin getLoadedSkin(SkinId id) { 
        return id == null ? null : loadedSkins.get(id); 
    }

    public static void registerRemoteSkin(String key, String geometryJson, byte[] textureData) {
        SkinId idKey = SkinId.parse(key);
        if (loadedSkins.containsKey(idKey) || !validateRemoteData(textureData, geometryJson)) return;
        
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(textureData));
            DynamicTexture texture = new DynamicTexture(() -> "bedrock_skin_remote", img);
            Identifier id = createIdentifier("bedrockskins", "skins/remote/" + StringUtils.sanitize(key));

            Minecraft.getInstance().getTextureManager().register(id, texture);

            LoadedSkin ls = new LoadedSkin("Remote", "Remote", key,
                JsonParser.parseString(geometryJson).getAsJsonObject(), AssetSource.Remote.INSTANCE);
            ls.identifier = id;
            loadedSkins.put(idKey, ls);
        } catch (Exception e) {
            System.err.println("Failed to register remote skin " + key + ": " + e.getMessage());
        }
    }

    // --- Loading Logic ---

    private static void loadVanillaGeometry(ResourceManager manager) {
        manager.getResource(createIdentifier("bedrockskins", "skin_packs/vanilla/geometry.json")).ifPresent(res -> {
            try (Reader r = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                vanillaGeometryJson = JsonParser.parseReader(r).getAsJsonObject();
            } catch (Exception e) { 
                System.err.println("ERROR loading vanilla geometry: " + e.getMessage()); 
            }
        });
    }

    private static void loadExternalPack(File packDir) {
        File skinsFile = new File(packDir, "skins.json");
        if (!skinsFile.exists()) return;

        try {
            JsonObject geometryJson = loadJsonOrNull(new File(packDir, "geometry.json"));
            SkinPackManifest manifest;
            try (Reader reader = new InputStreamReader(new FileInputStream(skinsFile), StandardCharsets.UTF_8)) {
                manifest = decodeManifest(reader, skinsFile.getAbsolutePath());
            }
            if (manifest == null) return;

            loadExternalTranslations(packDir);
            registerPackType(manifest);
            registerDynamicPackIcon(packIdFor(manifest.serializeName()), findPackIconFile(packDir));

            for (SkinEntry entry : manifest.skins()) {
                JsonObject geometry = resolveGeometry(entry.geometry(), geometryJson);
                if (geometry == null || entry.texture() == null) continue;

                AssetSource textureSource = resolveExternalAsset(entry.texture(), packDir);
                if (textureSource == null) continue;

                SkinId id = SkinId.of(manifest.serializeName(), entry.localizationName());
                LoadedSkin ls = new LoadedSkin(
                    manifest.serializeName(), manifest.localizationName(), entry.localizationName(),
                    geometry, textureSource, resolveExternalAsset(entry.cape(), packDir),
                    hasUpsideDownAnimation(entry)
                );
                
                try { ls.unfair = entry.unfair(); } catch (Exception ignored) {}
                loadedSkins.put(id, ls);
            }
        } catch (Exception e) {
            System.err.println("Error loading external pack from " + packDir.getName() + ": " + e);
        }
    }

    private static void loadInternalPacks(ResourceManager manager) {
        manager.listResources("skin_packs", idt -> idt.getPath().endsWith("skins.json")).forEach((id, resource) -> {
            try {
                String packPath = id.getPath().substring(0, id.getPath().lastIndexOf('/'));
                Identifier geoId = createIdentifier(id.getNamespace(), packPath + "/geometry.json");
                
                JsonObject geoJson = manager.getResource(geoId).map(res -> {
                    try (Reader r = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                        return JsonParser.parseReader(r).getAsJsonObject();
                    } catch (Exception e) { return null; }
                }).orElse(null);

                SkinPackManifest manifest;
                try (Reader rr = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    manifest = decodeManifest(rr, id.toString());
                }
                if (manifest == null) return;
                
                registerPackType(manifest);
                loadInternalTranslations(manager, id.getNamespace(), packPath);
                registerResourcePackIcon(packIdFor(manifest.serializeName()), findResourcePackIcon(id.getNamespace(), packPath, manager));

                for (SkinEntry entry : manifest.skins()) {
                    JsonObject geometry = resolveGeometry(entry.geometry(), geoJson);
                    if (geometry == null) continue;
                    
                    Identifier textureId = resolveInternalAsset(entry.texture(), id.getNamespace(), packPath, manager);
                    if (textureId == null) continue;

                    SkinId skinId = SkinId.of(manifest.serializeName(), entry.localizationName());
                    Identifier capeId = resolveInternalAsset(entry.cape(), id.getNamespace(), packPath, manager);

                    LoadedSkin ls = new LoadedSkin(
                        manifest.serializeName(), manifest.localizationName(), entry.localizationName(),
                        geometry, new AssetSource.Resource(textureId),
                        capeId != null ? new AssetSource.Resource(capeId) : null,
                        hasUpsideDownAnimation(entry)
                    );
                    try { ls.unfair = entry.unfair(); } catch (Exception ignored) {}
                    loadedSkins.put(skinId, ls);
                }
            } catch (Exception e) {
                System.err.println("Error loading internal pack " + id + ": " + e);
            }
        });
    }

    // --- Helpers: Geometry & Assets ---

    private static AssetSource resolveExternalAsset(String path, File packDir) {
        if (path == null) return null;
        Identifier resourceId = parseAssetPath(path);
        if (resourceId != null) return new AssetSource.Resource(resourceId);
        
        File file = new File(packDir, path.toLowerCase(Locale.ROOT));
        return file.exists() ? new AssetSource.File(file.getAbsolutePath()) : null;
    }

    private static Identifier resolveInternalAsset(String path, String namespace, String packPath, ResourceManager manager) {
        if (path == null) return null;
        Identifier id = parseAssetPath(path);
        if (id == null) id = createIdentifier(namespace, (packPath + "/" + path).toLowerCase(Locale.ROOT));
        return manager.getResource(id).isPresent() ? id : null;
    }

    public static JsonObject resolveGeometry(String name, JsonObject localGeo) {
        JsonObject raw = findGeometryNode(localGeo, name);
        if (raw == null) raw = findGeometryNode(vanillaGeometryJson, name);
        return raw != null ? wrapGeometry(raw.deepCopy(), name) : null;
    }

    private static JsonObject findGeometryNode(JsonObject json, String name) {
        if (json == null) return null;
        if (json.has(name)) return json.getAsJsonObject(name);

        JsonArray arr = json.getAsJsonArray("minecraft:geometry");
        if (arr != null) {
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject geo = el.getAsJsonObject();
                    if (geo.has("description") && geo.getAsJsonObject("description").has("identifier")) {
                        if (name.equals(geo.getAsJsonObject("description").get("identifier").getAsString())) return geo;
                    }
                }
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
            desc.addProperty("texture_width", content.has("texturewidth") ? content.get("texturewidth").getAsInt() : content.has("texture_width") ? content.get("texture_width").getAsInt() : 64);
            desc.addProperty("texture_height", content.has("textureheight") ? content.get("textureheight").getAsInt() : content.has("texture_height") ? content.get("texture_height").getAsInt() : 64);
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

        NativeImage img = loadNativeImage(skin.texture);
        if (img != null) {
            skin.identifier = createIdentifier("bedrockskins", "skins/" + skin.safePackName + "/" + skin.safeSkinName);
            tm.register(skin.identifier, new DynamicTexture(() -> "bedrock_skin", img));
        }

        if (skin.capeIdentifier == null && skin.cape != null) {
            NativeImage capeImg = loadNativeImage(skin.cape);
            if (capeImg != null) {
                skin.capeIdentifier = createIdentifier("bedrockskins", "capes/" + skin.safePackName + "/" + skin.safeSkinName);
                tm.register(skin.capeIdentifier, new DynamicTexture(() -> "bedrock_cape", capeImg));
            }
        }
    }

    private static NativeImage loadNativeImage(AssetSource source) {
        byte[] data = ExternalAssetUtil.loadTextureData(source, Minecraft.getInstance());
        try {
            return data.length > 0 ? NativeImage.read(new ByteArrayInputStream(data)) : null;
        } catch (IOException e) { return null; }
    }

    private static boolean usesDefaultHumanoidGeometry(LoadedSkin skin) {
        try {
            String identifier = skin.geometryData.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                    .getAsJsonObject("description").get("identifier").getAsString();
            return "geometry.humanoid.custom".equals(identifier) || "geometry.humanoid.customSlim".equals(identifier);
        } catch (Exception ignored) { return false; }
    }

    // --- Helpers: Translations & Misc ---

    private static void loadExternalTranslations(File packDir) {
        File[] files = new File(packDir, "texts").listFiles((dir, name) -> name.endsWith(".lang"));
        if (files != null) {
            for (File file : files) {
                try (InputStream is = new FileInputStream(file)) {
                    String lang = file.getName().substring(0, file.getName().lastIndexOf('.')).toLowerCase(Locale.ROOT);
                    parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new ConcurrentHashMap<>()));
                } catch (Exception ignored) {}
            }
        }
    }

    private static void loadInternalTranslations(ResourceManager manager, String namespace, String packPath) {
        for (String lang : new LinkedHashSet<>(Arrays.asList(getClientLanguage(), "en_us"))) {
            manager.getResource(createIdentifier(namespace, packPath + "/texts/" + lang + ".lang")).ifPresent(res -> {
                try (InputStream is = res.open()) {
                    parseTranslationStream(is, translations.computeIfAbsent(lang, k -> new ConcurrentHashMap<>()));
                } catch (Exception ignored) {}
            });
        }
    }

    private static void parseTranslationStream(InputStream input, Map<String, String> map) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
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
        manager.getResource(createIdentifier("bedrockskins", "order_overrides.json")).ifPresent(res -> {
            try (Reader r = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                packOrder = PACK_ORDER_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(r))
                    .resultOrPartial(msg -> {}).orElse(Collections.emptyList());
            } catch (Exception ignored) {}
        });
    }

    private static SkinPackManifest decodeManifest(Reader reader, String source) {
        try {
            return SkinPackManifest.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                .resultOrPartial(msg -> System.err.println("Failed decoding manifest " + source + ": " + msg))
                .orElse(null);
        } catch (Exception e) { return null; }
    }
    
    private static void registerPackType(SkinPackManifest manifest) {
        String serializeName = manifest.serializeName();
        if (serializeName == null) return;

        String packType = manifest.packType();
        if ((packType == null || packType.isEmpty()) && !"Favorites".equals(serializeName) && !"Standard".equals(serializeName)) {
            packType = "skin_pack";
        }
        
        if (packType != null && !packType.isEmpty()) {
            packTypesByPackId.put(packIdFor(serializeName), packType);
        }
    }

    private static String packIdFor(String serializeName) {
        return serializeName == null ? null : "skinpack." + serializeName;
    }

    private static void clearPackIcons() {
        var tm = Minecraft.getInstance().getTextureManager();
        dynamicPackIcons.forEach(tm::release);
        dynamicPackIcons.clear();
        packIconsByPackId.clear();
    }

    private static void registerResourcePackIcon(String packId, Identifier icon) {
        if (packId != null && icon != null) packIconsByPackId.putIfAbsent(packId, icon);
    }

    private static void registerDynamicPackIcon(String packId, File file) {
        if (packId != null && file != null && file.exists() && !packIconsByPackId.containsKey(packId)) {
            registerDynamicPackIcon(packId, new AssetSource.File(file.getAbsolutePath()));
        }
    }

    private static File findPackIconFile(File packDir) {
        if (packDir == null) return null;
        File icon = new File(packDir, "pack.png");
        return icon.exists() ? icon : (icon = new File(packDir, "pack_icon.png")).exists() ? icon : null;
    }

    private static Identifier findResourcePackIcon(String namespace, String packPath, ResourceManager manager) {
        Identifier icon = createIdentifier(namespace, packPath + "/pack.png");
        if (manager.getResource(icon).isPresent()) return icon;
        icon = createIdentifier(namespace, packPath + "/pack_icon.png");
        return manager.getResource(icon).isPresent() ? icon : null;
    }

    private static void registerDynamicPackIcon(String packId, AssetSource source) {
        NativeImage image = loadNativeImage(source);
        if (image == null) return;

        Identifier id = createIdentifier("bedrockskins", "pack_icons/" + StringUtils.sanitize(packId));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "bedrock_pack_icon", image));
        dynamicPackIcons.add(id);
        packIconsByPackId.put(packId, id);
    }

    private static File getSkinPacksDir() {
        return new File(Minecraft.getInstance().gameDirectory, "skin_packs"); 
    }

    private static String getClientLanguage() {
        try {
            return Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) { return "en_us"; }
    }

    private static boolean validateRemoteData(byte[] data, String geo) {
        return data.length <= 512 * 1024 && data.length >= 4 
            && data[0] == (byte)0x89 && data[1] == (byte)0x50 && data[2] == (byte)0x4E 
            && geo.length() <= 100_000;
    }

    private static boolean hasUpsideDownAnimation(SkinEntry entry) {
        return entry != null && entry.animations() != null 
            && "animation.player.base_pose.upside_down".equals(entry.animations().get("humanoid_base_pose"));
    }

    private static JsonObject loadJsonOrNull(File file) {
        if (!file.exists()) return null;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) { return null; }
    }
}