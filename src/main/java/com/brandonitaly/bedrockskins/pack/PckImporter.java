package com.brandonitaly.bedrockskins.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.CRC32;

public class PckImporter {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PCK_ASSET_SKIN = 0;
    private static final int PCK_ASSET_CAPE = 1;
    private static final int PCK_ASSET_SKIN_DATA_FILE = 11;

    public static boolean importPck(File pckFile, File outputDir) {
        try {
            byte[] bytes = Files.readAllBytes(pckFile.toPath());
            PckFileParser.PckArchive root = PckFileParser.parse(bytes);

            List<PckFileParser.PckAsset> allAssets = collectAllPckAssets(root);
            if (allAssets.isEmpty()) return false;

            List<PckFileParser.PckAsset> skins = new ArrayList<>();
            List<PckFileParser.PckAsset> generalAssets = new ArrayList<>();
            Map<String, PckFileParser.PckAsset> capesByName = new HashMap<>();
            Map<String, PckFileParser.PckAsset> capesByBaseName = new HashMap<>();

            for (PckFileParser.PckAsset asset : allAssets) {
                if (asset == null || asset.data() == null) continue;
                String normPath = normalizePckPath(asset.filename());
                String baseName = StringUtils.stripExtension(fileNameOnly(normPath));

                if (isLikelyCapeAsset(asset, normPath, baseName)) {
                    capesByName.put(normPath, asset);
                    capesByBaseName.put(fileNameOnly(normPath), asset);
                } else if (isLikelySkinAsset(asset, normPath, baseName)) {
                    skins.add(asset);
                } else {
                    generalAssets.add(asset);
                }
            }

            if (skins.isEmpty()) return false;

            if (!outputDir.exists() && !outputDir.mkdirs()) return false;
            File textsDir = new File(outputDir, "texts");
            if (!textsDir.exists()) textsDir.mkdirs();

            Map<String, Map<String, String>> pckTranslations = PckLocalizationSupport.loadPckLocalisations(allAssets);
            String fileBaseName = StringUtils.stripExtension(pckFile.getName());
            if (fileBaseName.equalsIgnoreCase("TexturePack") || fileBaseName.equalsIgnoreCase("skins")) {
                File parent = pckFile.getParentFile();
                if (parent != null) {
                    fileBaseName = parent.getName();
                }
            }
            String serializeName = StringUtils.sanitize(fileBaseName);
            if (serializeName.isEmpty()) serializeName = "legacy_console";

            String safePackTranslationKey = "skinpack." + serializeName;

            JsonObject manifest = new JsonObject();
            JsonArray skinsArray = new JsonArray();
            manifest.addProperty("serialize_name", serializeName);
            manifest.addProperty("localization_name", serializeName);
            manifest.add("skins", skinsArray);

            JsonObject masterGeometryFile = new JsonObject();
            JsonArray masterGeometryArray = new JsonArray();
            masterGeometryFile.addProperty("format_version", "1.12.0");
            masterGeometryFile.add("minecraft:geometry", masterGeometryArray);

            record SkinLocData(PckFileParser.PckAsset asset, String safeSkinTranslationKey, String skinDisplayToken, String skinKey) {}
            List<SkinLocData> skinLocs = new ArrayList<>();

            for (PckFileParser.PckAsset asset : skins) {
                String skinKey = StringUtils.stripExtension(fileNameOnly(normalizePckPath(asset.filename())));
                if (skinKey.isEmpty()) continue;

                String safeSkinTranslationKey = "skin." + serializeName + "." + skinKey;
                String skinDisplayToken = StringUtils.firstNonBlank(asset.getFirstProperty("DISPLAYNAMEID", "IDS_DISPLAY_NAME", "LOC_KEY", "DISPLAYNAME"), skinKey);
                skinLocs.add(new SkinLocData(asset, safeSkinTranslationKey, skinDisplayToken, skinKey));

                // Geometry mapping
                Long animMask = PckModelConverter.parseAnimMask(asset);
                boolean slim = PckModelConverter.isSlim(animMask);
                boolean upsideDown = PckModelConverter.isUpsideDown(animMask);
                String baseGeometryName = slim ? "geometry.humanoid.customSlim" : "geometry.humanoid.custom";
                
                JsonObject wrappedGeo = SkinPackLoader.resolveGeometry(baseGeometryName, null);
                JsonObject modifiedGeoWrapper = PckModelConverter.applyPckDataToGeometry(asset, wrappedGeo, animMask);
                String uniqueGeoId = "geometry." + serializeName + "." + skinKey;

                if (modifiedGeoWrapper != null) {
                    JsonObject innerGeo = modifiedGeoWrapper.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                    innerGeo.getAsJsonObject("description").addProperty("identifier", uniqueGeoId);
                    masterGeometryArray.add(innerGeo);
                } else {
                    uniqueGeoId = baseGeometryName; // Fallback to vanilla
                }

                // Cape writing
                String capeFileName = null;
                String capePath = asset.getFirstProperty("CAPEPATH");
                PckFileParser.PckAsset capeAsset = null;
                
                if (capePath != null && !capePath.isBlank()) {
                    capeAsset = resolveCape(capesByName, capesByBaseName, capePath);
                } else {
                    capeAsset = resolveCape(capesByName, capesByBaseName, inferCapeNameFromSkin(skinKey));
                }
                
                if (capeAsset != null) {
                    capeFileName = fileNameOnly(normalizePckPath(capeAsset.filename()));
                    Files.write(new File(outputDir, capeFileName).toPath(), capeAsset.data());
                }

                // Skin resizing/writing
                String skinFileName = skinKey + ".png";
                java.nio.file.Path skinFilePath = new File(outputDir, skinFileName).toPath();
                byte[] skinData = asset.data();
                boolean skinSaved = false;
                
                try (NativeImage image = NativeImage.read(new ByteArrayInputStream(skinData))) {
                    int width = image.getWidth();
                    int height = image.getHeight();

                    // If it is a legacy 64x32 aspect ratio, scale it up to 64x64
                    if (width == height * 2 && (width == 64 || width == 128)) {
                        int s = width / 64; 
                        try (NativeImage converted = new NativeImage(width, width, true)) {
                            converted.copyFrom(image);
                            converted.fillRect(0, 32 * s, width, 32 * s, 0);
                            
                            int[] srcX = {4, 8, 0, 4, 8, 12, 44, 48, 40, 44, 48, 52};
                            int[] srcY = {16, 16, 20, 20, 20, 20, 16, 16, 20, 20, 20, 20};
                            int[] dstX = {16, 32, 24, 16, 8, 16, -8, -8, 0, -8, -16, -8};
                            int[] w = {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4};
                            int[] h = {4, 4, 12, 12, 12, 12, 4, 4, 12, 12, 12, 12};
                            
                            for (int i = 0; i < srcX.length; i++) {
                                converted.copyRect(srcX[i] * s, srcY[i] * s, dstX[i] * s, 32 * s, w[i] * s, h[i] * s, true, false);
                            }
                            
                            converted.writeToFile(skinFilePath);
                            skinSaved = true;
                        }
                    }
                } catch (Exception ignored) {}
                
                // If we didn't convert and save the image, save the raw bytes now
                if (!skinSaved) Files.write(skinFilePath, skinData);

                JsonObject skinEntry = new JsonObject();
                skinEntry.addProperty("localization_name", skinKey);
                skinEntry.addProperty("geometry", uniqueGeoId);
                skinEntry.addProperty("texture", skinFileName);
                skinEntry.addProperty("type", "free");
                if (capeFileName != null) skinEntry.addProperty("cape", capeFileName);
                
                if (upsideDown) {
                    JsonObject animations = new JsonObject();
                    animations.addProperty("humanoid_base_pose", "animation.player.base_pose.upside_down");
                    skinEntry.add("animations", animations);
                }
                
                Long gf = PckModelConverter.parseGameFlags(asset);
                if (gf != null && (gf & 1L) != 0L) skinEntry.addProperty("unfair", true);
                
                skinsArray.add(skinEntry);
            }

            Files.writeString(new File(outputDir, "skins.json").toPath(), GSON.toJson(manifest), StandardCharsets.UTF_8);
            if (masterGeometryArray.size() > 0) {
                Files.writeString(new File(outputDir, "geometry.json").toPath(), GSON.toJson(masterGeometryFile), StandardCharsets.UTF_8);
            }

            Set<String> languages = new TreeSet<>(pckTranslations.keySet());
            languages.add("en_us");

            for (String lang : languages) {
                StringBuilder langBuilder = new StringBuilder();
                String packDisplayToken = StringUtils.firstNonBlank(PckLocalizationSupport.findPackDisplayToken(pckTranslations, lang), StringUtils.firstNonBlank(getPackDisplayName(generalAssets), fileBaseName));
                String packDisplayName = StringUtils.firstNonBlank(PckLocalizationSupport.resolvePckLocalizedToken(packDisplayToken, pckTranslations, lang), fileBaseName);
                langBuilder.append(safePackTranslationKey).append("=").append(packDisplayName).append("\n");

                for (SkinLocData loc : skinLocs) {
                    String skinDisplayName = StringUtils.firstNonBlank(PckLocalizationSupport.resolvePckLocalizedToken(loc.skinDisplayToken(), pckTranslations, lang), loc.skinKey());
                    langBuilder.append(loc.safeSkinTranslationKey()).append("=").append(skinDisplayName).append("\n");

                    String skinThemeToken = PckLocalizationSupport.deriveSkinThemeToken(loc.asset(), loc.skinDisplayToken(), loc.skinKey(), pckTranslations, lang);
                    String resolvedTheme = PckLocalizationSupport.resolvePckLocalizedToken(skinThemeToken, pckTranslations, lang);
                    if (resolvedTheme != null && !resolvedTheme.isBlank() && !resolvedTheme.equalsIgnoreCase(PckLocalizationSupport.cleanLocText(skinThemeToken))) {
                        langBuilder.append(loc.safeSkinTranslationKey()).append(".description=").append(resolvedTheme).append("\n");
                    }
                }

                Files.writeString(new File(textsDir, lang + ".lang").toPath(), langBuilder.toString(), StandardCharsets.UTF_8);
            }

            JsonArray langArray = new JsonArray();
            for (String lang : languages) {
                langArray.add(lang);
            }
            Files.writeString(new File(textsDir, "languages.json").toPath(), GSON.toJson(langArray), StandardCharsets.UTF_8);

            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to import PCK file {}", pckFile.getName(), e);
            return false;
        }
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
        return asset.getFirstProperty("ANIM", "DISPLAYNAME", "DISPLAYNAMEID", "LOC_KEY") != null;
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
        return generalAssets.stream()
                .map(asset -> asset.getFirstProperty("IDS_DISPLAY_NAME", "DISPLAYNAMEID"))
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static long fastHash(byte[] data) {
        if (data == null || data.length == 0) return 0;
        CRC32 crc = new CRC32();
        crc.update(data, 0, Math.min(data.length, 1024));
        return ((long) data.length << 32) | crc.getValue();
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

    private static PckFileParser.PckAsset resolveCape(Map<String, PckFileParser.PckAsset> capesByName, Map<String, PckFileParser.PckAsset> capesByBaseName, String capePath) {
        if (capePath == null) return null;
        String normalized = normalizePckPath(capePath);
        PckFileParser.PckAsset cape = capesByName.get(normalized);
        if (cape != null) return cape;
        return capesByBaseName.get(fileNameOnly(normalized));
    }
}