package com.brandonitaly.bedrockskins.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.CRC32;

public class PckImporter {

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

            if (skins.isEmpty()) return false;

            // Prepare Folders
            if (!outputDir.exists() && !outputDir.mkdirs()) return false;
            File textsDir = new File(outputDir, "texts");
            if (!textsDir.exists()) textsDir.mkdirs();

            // Pack Metadata
            String currentLang = "en_us"; // Default export to en_us
            Map<String, Map<String, String>> pckTranslations = PckLocalizationSupport.loadPckLocalisations(allAssets);
            String fileBaseName = stripExtension(pckFile.getName());
            String serializeName = StringUtils.sanitize(fileBaseName);
            if (serializeName.isEmpty()) serializeName = "legacy_console";

            String packDisplayToken = StringUtils.firstNonBlank(PckLocalizationSupport.findPackDisplayToken(pckTranslations, currentLang), StringUtils.firstNonBlank(getPackDisplayName(generalAssets), fileBaseName));
            String packDisplayName = StringUtils.firstNonBlank(PckLocalizationSupport.resolvePckLocalizedToken(packDisplayToken, pckTranslations, currentLang), fileBaseName);
            
            // Standardize Translation Keys
            String safePackTranslationKey = "skinpack." + serializeName;

            // Master JSONs
            JsonObject manifest = new JsonObject();
            JsonArray skinsArray = new JsonArray();
            manifest.addProperty("serialize_name", serializeName);
            manifest.addProperty("localization_name", serializeName);
            manifest.add("skins", skinsArray);

            JsonObject masterGeometryFile = new JsonObject();
            JsonArray masterGeometryArray = new JsonArray();
            masterGeometryFile.addProperty("format_version", "1.12.0");
            masterGeometryFile.add("minecraft:geometry", masterGeometryArray);

            StringBuilder langBuilder = new StringBuilder();
            langBuilder.append(safePackTranslationKey).append("=").append(packDisplayName).append("\n");

            for (PckFileParser.PckAsset asset : skins) {
                String skinKey = stripExtension(fileNameOnly(normalizePckPath(asset.filename())));
                if (skinKey.isEmpty()) continue;

                String safeSkinTranslationKey = "skin." + serializeName + "." + skinKey;

                String skinDisplayToken = StringUtils.firstNonBlank(asset.getFirstProperty("DISPLAYNAMEID", "IDS_DISPLAY_NAME", "LOC_KEY"), StringUtils.firstNonBlank(asset.getFirstProperty("DISPLAYNAME"), skinKey));
                String skinDisplayName = StringUtils.firstNonBlank(PckLocalizationSupport.resolvePckLocalizedToken(skinDisplayToken, pckTranslations, currentLang), skinKey);
                langBuilder.append(safeSkinTranslationKey).append("=").append(skinDisplayName).append("\n");

                String skinThemeToken = PckLocalizationSupport.deriveSkinThemeToken(asset, skinDisplayToken, skinKey, pckTranslations, currentLang);
                String resolvedTheme = PckLocalizationSupport.resolvePckLocalizedToken(skinThemeToken, pckTranslations, currentLang);
                if (resolvedTheme != null && !resolvedTheme.isBlank() && !resolvedTheme.equalsIgnoreCase(PckLocalizationSupport.cleanLocText(skinThemeToken))) {
                    langBuilder.append(safeSkinTranslationKey).append(".description=").append(resolvedTheme).append("\n");
                }

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
                byte[] finalSkinData = convertToSquare(asset.data());
                Files.write(new File(outputDir, skinFileName).toPath(), finalSkinData);

                // Manifest entry
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
                if (gf != null && (gf & 1L) != 0L) {
                    skinEntry.addProperty("unfair", true);
                }
                
                skinsArray.add(skinEntry);
            }

            Files.writeString(new File(outputDir, "skins.json").toPath(), GSON.toJson(manifest), StandardCharsets.UTF_8);
            if (masterGeometryArray.size() > 0) {
                Files.writeString(new File(outputDir, "geometry.json").toPath(), GSON.toJson(masterGeometryFile), StandardCharsets.UTF_8);
            }
            Files.writeString(new File(textsDir, "en_us.lang").toPath(), langBuilder.toString(), StandardCharsets.UTF_8);

            return true;
        } catch (Exception e) {
            System.err.println("Failed to import PCK file: " + pckFile.getName() + " - " + e.getMessage());
            return false;
        }
    }

    // --- Dynamic Skin Conversion (Standard Java API) ---
    
    private static byte[] convertToSquare(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) return imageData;
            int width = image.getWidth();
            int height = image.getHeight();

            // If it is already square, just return original data
            if (width == height) {
                return imageData; 
            }

            // Standard legacy dimension conversion (e.g. 64x32 -> 64x64 or 128x64 -> 128x128)
            if (width == height * 2) {
                int s = width / 64; // Scale multiplier (1 for 64x32, 2 for 128x64)
                BufferedImage converted = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = converted.createGraphics();
                
                // Copy original image into the top half
                g.drawImage(image, 0, 0, null);
                g.dispose();

                // Copy and mirror left arm and left leg from right arm and right leg
                // Params: src, dst, srcX, srcY, deltaX, deltaY, width, height, flipX, flipY
                copyRect(image, converted, 4*s, 16*s, 16*s, 32*s, 4*s, 4*s, true, false); // Left Leg Top
                copyRect(image, converted, 8*s, 16*s, 16*s, 32*s, 4*s, 4*s, true, false); // Left Leg Bottom
                copyRect(image, converted, 0*s, 20*s, 24*s, 32*s, 4*s, 12*s, true, false); // Left Leg Right
                copyRect(image, converted, 4*s, 20*s, 16*s, 32*s, 4*s, 12*s, true, false); // Left Leg Front
                copyRect(image, converted, 8*s, 20*s, 8*s, 32*s, 4*s, 12*s, true, false);  // Left Leg Left
                copyRect(image, converted, 12*s, 20*s, 16*s, 32*s, 4*s, 12*s, true, false); // Left Leg Back
                
                copyRect(image, converted, 44*s, 16*s, -8*s, 32*s, 4*s, 4*s, true, false); // Left Arm Top
                copyRect(image, converted, 48*s, 16*s, -8*s, 32*s, 4*s, 4*s, true, false); // Left Arm Bottom
                copyRect(image, converted, 40*s, 20*s, 0*s, 32*s, 4*s, 12*s, true, false); // Left Arm Right
                copyRect(image, converted, 44*s, 20*s, -8*s, 32*s, 4*s, 12*s, true, false); // Left Arm Front
                copyRect(image, converted, 48*s, 20*s, -16*s, 32*s, 4*s, 12*s, true, false); // Left Arm Left
                copyRect(image, converted, 52*s, 20*s, -8*s, 32*s, 4*s, 12*s, true, false); // Left Arm Back

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(converted, "png", baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            System.err.println("Failed to resize skin: " + e.getMessage());
        }
        return imageData; // Fallback to original bytes
    }

    private static void copyRect(BufferedImage src, BufferedImage dst, int srcX, int srcY, int dx, int dy, int width, int height, boolean flipX, boolean flipY) {
        int dstX = srcX + dx;
        int dstY = srcY + dy;
        int sx1 = flipX ? srcX + width : srcX;
        int sy1 = flipY ? srcY + height : srcY;
        int sx2 = flipX ? srcX : srcX + width;
        int sy2 = flipY ? srcY : srcY + height;

        Graphics2D g = dst.createGraphics();
        g.drawImage(src, dstX, dstY, dstX + width, dstY + height, sx1, sy1, sx2, sy2, null);
        g.dispose();
    }

    // --- Extracted Helpers ---

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
        for (PckFileParser.PckAsset asset : generalAssets) {
            String fromIdsDisplayName = asset.getFirstProperty("IDS_DISPLAY_NAME", "DISPLAYNAMEID");
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

    public static String stripExtension(String name) {
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

    private static PckFileParser.PckAsset resolveCape(Map<String, PckFileParser.PckAsset> capesByName, Map<String, PckFileParser.PckAsset> capesByBaseName, String capePath) {
        if (capePath == null) return null;
        String normalized = normalizePckPath(capePath);
        PckFileParser.PckAsset cape = capesByName.get(normalized);
        if (cape != null) return cape;
        return capesByBaseName.get(fileNameOnly(normalized));
    }
}