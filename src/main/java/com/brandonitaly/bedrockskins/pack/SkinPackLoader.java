package com.brandonitaly.bedrockskins.pack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinPackLoader {
    private SkinPackLoader() {}

    private static JsonObject vanillaGeometryJson = null;
    private static final Gson gson = new Gson();

    public static final Map<String, LoadedSkin> loadedSkins = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final File skinPacksDir = new File("skin_packs");
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    public static List<String> packOrder = Collections.emptyList();

    // --- Public API ---

    public static String getTranslation(String key) {
        String currentLang = "en_us";
        try {
            currentLang = MinecraftClient.getInstance().getLanguageManager().getLanguage();
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

        if (skinPacksDir.exists()) {
            File[] children = skinPacksDir.listFiles();
            if (children != null) {
                for (File f : children) {
                    if (f.isDirectory()) loadExternalPack(f);
                }
            }
        }

        MinecraftClient client = null;
        try { client = MinecraftClient.getInstance(); } catch (Exception e) { return; }
        if (client == null) return;
        ResourceManager manager = client.getResourceManager();

        loadVanillaGeometry(manager);
        loadInternalPacks(manager);
        loadPackOrder(manager);
    }

    public static void registerTextures() {
        System.out.println("SkinPackLoader: Registering all textures...");
        for (LoadedSkin s : loadedSkins.values()) registerSkinAssets(s);
    }

    public static Identifier registerTextureFor(String key) {
        LoadedSkin skin = loadedSkins.get(key);
        if (skin == null) return null;
        if (skin.getIdentifier() != null) return skin.getIdentifier();
        registerSkinAssets(skin);
        return skin.getIdentifier();
    }

    public static void registerRemoteSkinStatic(String key, String geometryJson, byte[] textureData) {
        registerRemoteSkin(key, geometryJson, textureData);
    }

    public static void registerRemoteSkin(String key, String geometryJson, byte[] textureData) {
        if (loadedSkins.containsKey(key)) return;
        try {
            if (!validateRemoteData(key, textureData, geometryJson)) return;

            NativeImage img = NativeImage.read(new ByteArrayInputStream(textureData));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "bedrock_skin_remote", img);
            String safeKey = StringUtils.sanitize(key);
            Identifier id = Identifier.of("bedrockskins", "skins/remote/" + safeKey);

            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

            LoadedSkin ls = new LoadedSkin(
                "Remote",
                "Remote",
                key,
                JsonParser.parseString(geometryJson).getAsJsonObject(),
                AssetSource.Remote.INSTANCE
            );
            ls.identifier = id;
            loadedSkins.put(key, ls);
            System.out.println("Registered remote skin: " + key);
        } catch (Exception e) {
            System.out.println("Failed to register remote skin " + key + ": " + e);
        }
    }

    // --- Loading Logic ---

    private static void loadVanillaGeometry(ResourceManager manager) {
        try {
            Identifier id = Identifier.of("bedrockskins", "skin_packs/vanilla/geometry.json");
            manager.getResource(id).ifPresent(res -> {
                try (InputStream is = res.getInputStream(); InputStreamReader r = new InputStreamReader(is)) {
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

            for (SkinEntry entry : manifest.getSkins()) {
                JsonObject geometry = resolveGeometry(entry.getGeometry(), geometryJson);
                if (geometry == null) continue;

                File textureFile = new File(packDir, entry.getTexture());
                File capeFile = entry.getCape() != null ? new File(packDir, entry.getCape()) : null;
                if (capeFile != null && !capeFile.exists()) capeFile = null;

                if (textureFile.exists()) {
                    String key = manifest.getLocalizationName() + ":" + entry.getLocalizationName();
                    loadedSkins.put(key, new LoadedSkin(
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
        manager.findResources("skin_packs", idt -> idt.getPath().endsWith("skins.json")).forEach((id, resource) -> {
            try {
                Identifier geoId = Identifier.of(id.getNamespace(), id.getPath().replace("skins.json", "geometry.json"));
                JsonObject geoJson = manager.getResource(geoId).map(res -> {
                    try (InputStream is = res.getInputStream(); InputStreamReader r = new InputStreamReader(is)) {
                        return JsonParser.parseReader(r).getAsJsonObject();
                    } catch (Exception e) { return null; }
                }).orElse(null);

                SkinPackManifest manifest;
                try (InputStream ris = resource.getInputStream(); InputStreamReader rr = new InputStreamReader(ris)) {
                    manifest = gson.fromJson(rr, SkinPackManifest.class);
                }
                String packPath = id.getPath().substring(0, id.getPath().lastIndexOf('/'));

                loadInternalTranslations(manager, id.getNamespace(), packPath);

                for (SkinEntry entry : manifest.getSkins()) {
                    JsonObject geometry = resolveGeometry(entry.getGeometry(), geoJson);
                    if (geometry == null) continue;
                    Identifier textureId = Identifier.of(id.getNamespace(), (packPath + "/" + entry.getTexture()).toLowerCase(Locale.ROOT));

                    if (manager.getResource(textureId).isPresent()) {
                        Identifier capeId = null;
                        if (entry.getCape() != null) {
                            Identifier candidate = Identifier.of(id.getNamespace(), (packPath + "/" + entry.getCape()).toLowerCase(Locale.ROOT));
                            if (manager.getResource(candidate).isPresent()) capeId = candidate;
                        }

                        String key = manifest.getLocalizationName() + ":" + entry.getLocalizationName();
                        loadedSkins.put(key, new LoadedSkin(
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
        if (raw == null) raw = findGeometryNode(vanillaGeometryJson, name);
        if (raw == null) return null;
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
            Identifier id = Identifier.of("bedrockskins", "skins/" + skin.getSafePackName() + "/" + skin.getSafeSkinName());
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "bedrock_skin", img));
            skin.identifier = id;
            System.out.println("Registered texture: " + id);
        }

        // Cape
        if (skin.capeIdentifier == null && skin.getCape() != null) {
            NativeImage capeImg = loadNativeImage(skin.getCape());
            if (capeImg != null) {
                Identifier id = Identifier.of("bedrockskins", "capes/" + skin.getSafePackName() + "/" + skin.getSafeSkinName());
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "bedrock_cape", capeImg));
                skin.capeIdentifier = id;
                System.out.println("Registered texture: " + id);
            }
        }
    }

    private static NativeImage loadNativeImage(AssetSource source) {
        try {
            if (source instanceof AssetSource.Resource) {
                Resource res = MinecraftClient.getInstance().getResourceManager().getResource(((AssetSource.Resource) source).getId()).orElse(null);
                if (res == null) return null;
                try (InputStream is = res.getInputStream()) {
                    return NativeImage.read(is);
                }
            } else if (source instanceof AssetSource.File) {
                try (InputStream is = new FileInputStream(new File(((AssetSource.File) source).getPath()))) {
                    return NativeImage.read(is);
                }
            } else {
                return null; // Remote already pre-loaded
            }
        } catch (Exception e) {
            System.out.println("Failed to load image (" + source + "): " + e);
            return null;
        }
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
        try { clientLang = MinecraftClient.getInstance().getLanguageManager().getLanguage().toLowerCase(Locale.ROOT); } catch (Exception ignored) {}
        List<String> langs = Arrays.asList(clientLang, "en_us");
        for (String lang : new LinkedHashSet<>(langs)) {
            Identifier id = Identifier.of(namespace, packPath + "/texts/" + lang + ".lang");
            manager.getResource(id).ifPresent(res -> {
                try (InputStream is = res.getInputStream()) {
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
            manager.getResource(Identifier.of("bedrockskins", "order_overrides.json")).ifPresent(res -> {
                try (InputStream is = res.getInputStream()) {
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
