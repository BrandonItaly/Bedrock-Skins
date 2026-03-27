package com.brandonitaly.bedrockskins.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class PckModelConverter {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final int PCK_ANIM_FLAG_STATIC_ARMS = 0;
    private static final int PCK_ANIM_FLAG_ZOMBIE_ARMS = 1;
    private static final int PCK_ANIM_FLAG_STATIC_LEGS = 2;
    private static final int PCK_ANIM_FLAG_SYNCED_LEGS = 5;
    private static final int PCK_ANIM_FLAG_SYNCED_ARMS = 6;
    private static final int PCK_ANIM_FLAG_ALL_ARMOR_DISABLED = 8;
    private static final int PCK_ANIM_FLAG_HEAD_DISABLED = 10;
    private static final int PCK_ANIM_FLAG_RIGHT_ARM_DISABLED = 11;
    private static final int PCK_ANIM_FLAG_LEFT_ARM_DISABLED = 12;
    private static final int PCK_ANIM_FLAG_BODY_DISABLED = 13;
    private static final int PCK_ANIM_FLAG_RIGHT_LEG_DISABLED = 14;
    private static final int PCK_ANIM_FLAG_LEFT_LEG_DISABLED = 15;
    private static final int PCK_ANIM_FLAG_HEAD_OVERLAY_DISABLED = 16;
    private static final int PCK_ANIM_FLAG_SLIM = 19;
    private static final int PCK_ANIM_FLAG_LEFT_ARM_OVERLAY_DISABLED = 20;
    private static final int PCK_ANIM_FLAG_RIGHT_ARM_OVERLAY_DISABLED = 21;
    private static final int PCK_ANIM_FLAG_LEFT_LEG_OVERLAY_DISABLED = 22;
    private static final int PCK_ANIM_FLAG_RIGHT_LEG_OVERLAY_DISABLED = 23;
    private static final int PCK_ANIM_FLAG_BODY_OVERLAY_DISABLED = 24;
    private static final int PCK_ANIM_FLAG_FORCE_HEAD_ARMOR = 25;
    private static final int PCK_ANIM_FLAG_FORCE_RIGHT_ARM_ARMOR = 26;
    private static final int PCK_ANIM_FLAG_FORCE_LEFT_ARM_ARMOR = 27;
    private static final int PCK_ANIM_FLAG_FORCE_BODY_ARMOR = 28;
    private static final int PCK_ANIM_FLAG_FORCE_RIGHT_LEG_ARMOR = 29;
    private static final int PCK_ANIM_FLAG_FORCE_LEFT_LEG_ARMOR = 30;
    private static final int PCK_ANIM_FLAG_DINNERBONE = 31;

    private static final float[] TRANS_ARM0 = { -5.0f, 2.0f, 0.0f };
    private static final float[] TRANS_ARM1 = { 5.0f, 2.0f, 0.0f };
    private static final float[] TRANS_LEG0 = { -1.9f, 12.0f, 0.0f };
    private static final float[] TRANS_LEG1 = { 1.9f, 12.0f, 0.0f };
    private static final float[] TRANS_ZERO = { 0.0f, 0.0f, 0.0f };

    private static final float[] PIVOT_HEAD_BODY = { 0.0f, 24.0f, 0.0f };
    private static final float[] PIVOT_RIGHT_ARM = { -5.0f, 22.0f, 0.0f };
    private static final float[] PIVOT_LEFT_ARM = { 5.0f, 22.0f, 0.0f };
    private static final float[] PIVOT_RIGHT_LEG = { -1.9f, 12.0f, 0.0f };
    private static final float[] PIVOT_LEFT_LEG = { 1.9f, 12.0f, 0.0f };

    private PckModelConverter() {}

    static Long parseAnimMask(PckFileParser.PckAsset asset) {
        String anim = asset.getFirstProperty("ANIM");
        if (anim == null || anim.isBlank()) return null;

        String cleaned = anim.trim();
        try {
            if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
                return Long.parseUnsignedLong(cleaned.substring(2), 16);
            }
            return Long.parseUnsignedLong(cleaned);
        } catch (Exception ignored) {
            return null;
        }
    }

    static boolean isAnimFlagSet(Long mask, int bit) {
        return mask != null && (mask & (1L << bit)) != 0;
    }

    static boolean isSlim(Long mask) { return isAnimFlagSet(mask, PCK_ANIM_FLAG_SLIM); }
    static boolean isUpsideDown(Long mask) { return isAnimFlagSet(mask, PCK_ANIM_FLAG_DINNERBONE); }

    static Long parseGameFlags(PckFileParser.PckAsset asset) {
        String gf = asset.getFirstProperty("GAME_FLAGS", "GAMEFLAGS");
        if (gf == null || gf.isBlank()) return null;

        String cleaned = gf.trim();
        try {
            if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
                return Long.parseUnsignedLong(cleaned.substring(2), 16);
            }
            return Long.parseUnsignedLong(cleaned);
        } catch (Exception ignored) {
            return null;
        }
    }

    static JsonObject applyPckDataToGeometry(PckFileParser.PckAsset asset, JsonObject baseWrappedGeometry, Long animMask) {
        if (baseWrappedGeometry == null) return null;

        List<PckBox> boxes = asset != null ? parsePckBoxes(asset) : List.of();
        Map<String, Float> offsets = asset != null ? parsePckOffsets(asset) : Map.of();

        if (boxes.isEmpty() && offsets.isEmpty() && animMask == null) return baseWrappedGeometry;

        JsonObject geometry = baseWrappedGeometry.deepCopy();
        JsonObject node = firstGeometryNode(geometry);
        if (node == null) return baseWrappedGeometry;

        if (animMask != null) applyAnimationFlagsToGeometryFields(node, animMask);

        JsonArray bones = node.getAsJsonArray("bones");
        if (bones == null) {
            bones = new JsonArray();
            node.add("bones", bones);
        }

        // Delete Vanilla Cubes & Apply Pivot Offsets
        if (animMask != null || !offsets.isEmpty()) {
            boolean disableAllArmor = animMask != null && isAnimFlagSet(animMask, PCK_ANIM_FLAG_ALL_ARMOR_DISABLED);

            for (JsonElement el : bones) {
                if (!el.isJsonObject()) continue;
                JsonObject bone = el.getAsJsonObject();
                if (!bone.has("name")) continue;

                String rawBoneName = bone.get("name").getAsString();
                String lowerBoneName = rawBoneName.toLowerCase(Locale.ROOT);

                // Handle deletions
                if (animMask != null) {
                    boolean shouldClear = switch (lowerBoneName) {
                        case "head" -> isAnimFlagSet(animMask, PCK_ANIM_FLAG_HEAD_DISABLED);
                        case "body" -> isAnimFlagSet(animMask, PCK_ANIM_FLAG_BODY_DISABLED);
                        case "rightarm" -> isAnimFlagSet(animMask, PCK_ANIM_FLAG_RIGHT_ARM_DISABLED);
                        case "leftarm" -> isAnimFlagSet(animMask, PCK_ANIM_FLAG_LEFT_ARM_DISABLED);
                        case "rightleg" -> isAnimFlagSet(animMask, PCK_ANIM_FLAG_RIGHT_LEG_DISABLED);
                        case "leftleg" -> isAnimFlagSet(animMask, PCK_ANIM_FLAG_LEFT_LEG_DISABLED);
                        case "hat" -> disableAllArmor || isAnimFlagSet(animMask, PCK_ANIM_FLAG_HEAD_OVERLAY_DISABLED);
                        case "jacket" -> disableAllArmor || isAnimFlagSet(animMask, PCK_ANIM_FLAG_BODY_OVERLAY_DISABLED);
                        case "rightsleeve" -> disableAllArmor || isAnimFlagSet(animMask, PCK_ANIM_FLAG_RIGHT_ARM_OVERLAY_DISABLED);
                        case "leftsleeve" -> disableAllArmor || isAnimFlagSet(animMask, PCK_ANIM_FLAG_LEFT_ARM_OVERLAY_DISABLED);
                        case "rightpants" -> disableAllArmor || isAnimFlagSet(animMask, PCK_ANIM_FLAG_RIGHT_LEG_OVERLAY_DISABLED);
                        case "leftpants" -> disableAllArmor || isAnimFlagSet(animMask, PCK_ANIM_FLAG_LEFT_LEG_OVERLAY_DISABLED);
                        default -> false;
                    };

                    if (shouldClear) {
                        JsonArray cubes = bone.getAsJsonArray("cubes");
                        if (cubes != null && !cubes.isEmpty()) {
                            for (int i = cubes.size() - 1; i >= 0; i--) {
                                JsonElement cubeEl = cubes.get(i);
                                if (cubeEl.isJsonObject() && isVanillaCubeForBone(cubeEl.getAsJsonObject(), lowerBoneName)) {
                                    cubes.remove(i);
                                }
                            }
                        }
                    }
                }

                // Handle offsets
                if (!offsets.isEmpty()) {
                    String baseType = baseTypeForBoneName(lowerBoneName);
                    if (baseType != null) {
                        Float offsetY = offsets.get(baseType);

                        if (offsetY != null && Math.abs(offsetY) > 0.0001f) {
                            applyOffsetToBoneCubesY(bone, offsetY);
                            applyPivotOffsetY(bone, lowerBoneName, offsetY);
                        } else if ("BODY".equals(baseType)) {
                            Float chestOffset = offsets.get("CHEST");
                            if (chestOffset != null && Math.abs(chestOffset) > 0.0001f) {
                                applyPivotOffsetY(bone, lowerBoneName, chestOffset);
                            }
                        }
                    }
                }
            }
        }

        // Add PCK Cubes
        for (PckBox box : boxes) {
            String baseType = baseTypeForBoxType(box.type());
            float offsetY = offsets.getOrDefault(baseType, 0.0f);
            float[] origin = toBedrockOrigin(box);
            origin[1] -= offsetY;

            JsonObject bone = findOrCreateBone(bones, boneNameForBoxType(box.type()));
            JsonArray cubes = bone.getAsJsonArray("cubes");
            if (cubes == null) {
                cubes = new JsonArray();
                bone.add("cubes", cubes);
            }

            JsonObject cube = new JsonObject();
            cube.add("origin", createJsonArray(origin[0], origin[1], origin[2]));
            cube.add("size", createJsonArray(box.sizeX(), box.sizeY(), box.sizeZ()));
            cube.add("uv", createJsonArray(box.uvX(), box.uvY()));
            cube.addProperty("inflate", (double) (box.scale() + defaultOverlayInflate(box.type())));
            cube.addProperty("mirror", box.mirror());
            cubes.add(cube);
        }

        return geometry;
    }

    private static void applyAnimationFlagsToGeometryFields(JsonObject geometry, Long animMask) {
        geometry.addProperty("animationArmsOutFront", isAnimFlagSet(animMask, PCK_ANIM_FLAG_ZOMBIE_ARMS));
        geometry.addProperty("animationStationaryLegs", isAnimFlagSet(animMask, PCK_ANIM_FLAG_STATIC_LEGS));
        geometry.addProperty("animationSingleLegAnimation", isAnimFlagSet(animMask, PCK_ANIM_FLAG_SYNCED_LEGS));
        geometry.addProperty("animationSingleArmAnimation", isAnimFlagSet(animMask, PCK_ANIM_FLAG_SYNCED_ARMS) || isAnimFlagSet(animMask, PCK_ANIM_FLAG_STATIC_ARMS));
        geometry.addProperty("animationDontShowArmor", isAnimFlagSet(animMask, PCK_ANIM_FLAG_ALL_ARMOR_DISABLED));
        geometry.addProperty("animationHeadDisabled", isAnimFlagSet(animMask, PCK_ANIM_FLAG_HEAD_DISABLED));
        geometry.addProperty("animationBodyDisabled", isAnimFlagSet(animMask, PCK_ANIM_FLAG_BODY_DISABLED));
        geometry.addProperty("animationRightArmDisabled", isAnimFlagSet(animMask, PCK_ANIM_FLAG_RIGHT_ARM_DISABLED));
        geometry.addProperty("animationLeftArmDisabled", isAnimFlagSet(animMask, PCK_ANIM_FLAG_LEFT_ARM_DISABLED));
        geometry.addProperty("animationRightLegDisabled", isAnimFlagSet(animMask, PCK_ANIM_FLAG_RIGHT_LEG_DISABLED));
        geometry.addProperty("animationLeftLegDisabled", isAnimFlagSet(animMask, PCK_ANIM_FLAG_LEFT_LEG_DISABLED));
        geometry.addProperty("animationForceHeadArmor", isAnimFlagSet(animMask, PCK_ANIM_FLAG_FORCE_HEAD_ARMOR));
        geometry.addProperty("animationForceBodyArmor", isAnimFlagSet(animMask, PCK_ANIM_FLAG_FORCE_BODY_ARMOR));
        geometry.addProperty("animationForceRightArmArmor", isAnimFlagSet(animMask, PCK_ANIM_FLAG_FORCE_RIGHT_ARM_ARMOR));
        geometry.addProperty("animationForceLeftArmArmor", isAnimFlagSet(animMask, PCK_ANIM_FLAG_FORCE_LEFT_ARM_ARMOR));
        geometry.addProperty("animationForceRightLegArmor", isAnimFlagSet(animMask, PCK_ANIM_FLAG_FORCE_RIGHT_LEG_ARMOR));
        geometry.addProperty("animationForceLeftLegArmor", isAnimFlagSet(animMask, PCK_ANIM_FLAG_FORCE_LEFT_LEG_ARMOR));
    }

    private static List<PckBox> parsePckBoxes(PckFileParser.PckAsset asset) {
        if (asset == null || asset.properties() == null) return List.of();

        List<PckBox> out = new ArrayList<>();
        for (Map.Entry<String, String> property : asset.properties()) {
            if (property != null && "BOX".equalsIgnoreCase(property.getKey())) {
                PckBox parsed = parsePckBox(property.getValue());
                if (parsed != null) out.add(parsed);
            }
        }
        return out;
    }

    private static PckBox parsePckBox(String value) {
        if (value == null || value.isBlank()) return null;

        String[] t = WHITESPACE.split(value.trim());
        if (t.length < 9) return null;

        try {
            String type = normalizeBoxType(t[0]);
            if (type == null) return null;

            float posX = parseFloatToken(t[1]);
            float posY = parseFloatToken(t[2]);
            float posZ = parseFloatToken(t[3]);
            float sizeX = parseFloatToken(t[4]);
            float sizeY = parseFloatToken(t[5]);
            float sizeZ = parseFloatToken(t[6]);
            float uvX = parseFloatToken(t[7]);
            float uvY = parseFloatToken(t[8]);

            boolean mirror = t.length > 10 && ("1".equals(t[10]) || "true".equalsIgnoreCase(t[10]));
            float scale = t.length > 11 ? parseFloatToken(t[11]) : 0.0f;

            return new PckBox(type, posX, posY, posZ, sizeX, sizeY, sizeZ, uvX, uvY, mirror, scale);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Float> parsePckOffsets(PckFileParser.PckAsset asset) {
        if (asset == null || asset.properties() == null) return Map.of();

        Map<String, Float> out = new HashMap<>();
        for (Map.Entry<String, String> property : asset.properties()) {
            if (property == null || !"OFFSET".equalsIgnoreCase(property.getKey())) continue;

            String[] t = property.getValue() == null ? new String[0] : WHITESPACE.split(property.getValue().trim());
            if (t.length < 3) continue;

            String type = normalizeBaseType(t[0]);
            if (type != null) {
                try {
                    out.put(type, parseFloatToken(t[2]));
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    private static String baseTypeForBoneName(String lowerBoneName) {
        return switch (lowerBoneName) {
            case "head", "hat" -> "HEAD";
            case "body", "jacket" -> "BODY";
            case "rightarm", "rightsleeve" -> "ARM0";
            case "leftarm", "leftsleeve" -> "ARM1";
            case "rightleg", "rightpants" -> "LEG0";
            case "leftleg", "leftpants" -> "LEG1";
            default -> null;
        };
    }

    private static void applyPivotOffsetY(JsonObject bone, String lowerBoneName, float offsetY) {
        JsonArray pivot = bone.getAsJsonArray("pivot");
        float[] defaults = defaultPivotForBone(lowerBoneName);
        float x = defaults[0], y = defaults[1], z = defaults[2];

        if (pivot != null && pivot.size() >= 3) {
            x = pivot.get(0).getAsFloat();
            y = pivot.get(1).getAsFloat();
            z = pivot.get(2).getAsFloat();
        }

        bone.add("pivot", createJsonArray(x, y - offsetY, z));
    }

    private static void applyOffsetToBoneCubesY(JsonObject bone, float offsetY) {
        JsonArray cubes = bone.getAsJsonArray("cubes");
        if (cubes == null || cubes.isEmpty()) return;

        for (JsonElement cubeEl : cubes) {
            if (!cubeEl.isJsonObject()) continue;
            JsonObject cube = cubeEl.getAsJsonObject();
            JsonArray origin = cube.getAsJsonArray("origin");
            if (origin == null || origin.size() < 3) continue;

            float x = origin.get(0).getAsFloat();
            float y = origin.get(1).getAsFloat();
            float z = origin.get(2).getAsFloat();
            cube.add("origin", createJsonArray(x, y - offsetY, z));
        }
    }

    private static boolean isVanillaCubeForBone(JsonObject cube, String lowerBoneName) {
        JsonArray size = cube.getAsJsonArray("size");
        JsonArray uv = cube.getAsJsonArray("uv");
        
        if (size == null || size.size() < 3 || uv == null || uv.size() < 2) return false;

        return switch (lowerBoneName) {
            case "head" -> cubeSignatureEquals(size, uv, 8, 8, 8, 0, 0);
            case "hat" -> cubeSignatureEquals(size, uv, 8, 8, 8, 32, 0);
            case "body" -> cubeSignatureEquals(size, uv, 8, 12, 4, 16, 16);
            case "jacket" -> cubeSignatureEquals(size, uv, 8, 12, 4, 16, 32);
            case "rightarm" -> cubeSignatureEquals(size, uv, 4, 12, 4, 40, 16) || cubeSignatureEquals(size, uv, 3, 12, 4, 40, 16);
            case "leftarm" -> cubeSignatureEquals(size, uv, 4, 12, 4, 32, 48) || cubeSignatureEquals(size, uv, 3, 12, 4, 32, 48);
            case "rightsleeve" -> cubeSignatureEquals(size, uv, 4, 12, 4, 40, 32) || cubeSignatureEquals(size, uv, 3, 12, 4, 40, 32);
            case "leftsleeve" -> cubeSignatureEquals(size, uv, 4, 12, 4, 48, 48) || cubeSignatureEquals(size, uv, 3, 12, 4, 48, 48);
            case "rightleg" -> cubeSignatureEquals(size, uv, 4, 12, 4, 0, 16);
            case "leftleg" -> cubeSignatureEquals(size, uv, 4, 12, 4, 16, 48);
            case "rightpants" -> cubeSignatureEquals(size, uv, 4, 12, 4, 0, 32);
            case "leftpants" -> cubeSignatureEquals(size, uv, 4, 12, 4, 0, 48);
            default -> false;
        };
    }

    private static float[] toBedrockOrigin(PckBox box) {
        float[] trans = partTranslation(baseTypeForBoxType(box.type()));
        return new float[] { 
            box.posX() + trans[0], 
            24.0f - box.posY() - box.sizeY() - trans[1], 
            box.posZ() - trans[2] 
        };
    }

    private static float[] partTranslation(String baseType) {
        return switch (baseType) {
            case "ARM0" -> TRANS_ARM0;
            case "ARM1" -> TRANS_ARM1;
            case "LEG0" -> TRANS_LEG0;
            case "LEG1" -> TRANS_LEG1;
            default -> TRANS_ZERO;
        };
    }

    private static JsonObject findOrCreateBone(JsonArray bones, String boneName) {
        for (JsonElement el : bones) {
            if (!el.isJsonObject()) continue;
            JsonObject bone = el.getAsJsonObject();
            if (boneName.equalsIgnoreCase(bone.get("name").getAsString())) return bone;
        }

        JsonObject bone = new JsonObject();
        bone.addProperty("name", boneName);
        float[] p = defaultPivotForBone(boneName.toLowerCase(Locale.ROOT));
        bone.add("pivot", createJsonArray(p[0], p[1], p[2]));
        bones.add(bone);
        return bone;
    }

    private static JsonObject firstGeometryNode(JsonObject wrappedGeometry) {
        if (wrappedGeometry == null) return null;
        JsonArray arr = wrappedGeometry.getAsJsonArray("minecraft:geometry");
        
        if (arr == null || arr.isEmpty() || !arr.get(0).isJsonObject()) return null;
        return arr.get(0).getAsJsonObject();
    }

    private static float[] defaultPivotForBone(String lowerBoneName) {
        return switch (lowerBoneName) {
            case "head", "hat", "body", "jacket" -> PIVOT_HEAD_BODY;
            case "rightarm", "rightsleeve" -> PIVOT_RIGHT_ARM;
            case "leftarm", "leftsleeve" -> PIVOT_LEFT_ARM;
            case "rightleg", "rightpants" -> PIVOT_RIGHT_LEG;
            case "leftleg", "leftpants" -> PIVOT_LEFT_LEG;
            default -> TRANS_ZERO;
        };
    }

    private static JsonArray createJsonArray(float x, float y) {
        JsonArray arr = new JsonArray();
        arr.add(x); arr.add(y);
        return arr;
    }

    private static JsonArray createJsonArray(float x, float y, float z) {
        JsonArray arr = new JsonArray();
        arr.add(x); arr.add(y); arr.add(z);
        return arr;
    }

    private static String normalizeBoxType(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase(Locale.ROOT);
        return switch (up) {
            case "HEAD", "BODY", "ARM0", "ARM1", "LEG0", "LEG1", "HEADWEAR", "JACKET", "SLEEVE0", "SLEEVE1", "PANTS0", "PANTS1" -> up;
            default -> null;
        };
    }

    private static String normalizeBaseType(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase(Locale.ROOT);
        return switch (up) {
            case "HEAD", "BODY", "ARM0", "ARM1", "LEG0", "LEG1", "CHEST" -> up;
            default -> null;
        };
    }

    private static String baseTypeForBoxType(String type) {
        return switch (type) {
            case "HEADWEAR" -> "HEAD";
            case "JACKET" -> "BODY";
            case "SLEEVE0" -> "ARM0";
            case "SLEEVE1" -> "ARM1";
            case "PANTS0" -> "LEG0";
            case "PANTS1" -> "LEG1";
            default -> type;
        };
    }

    private static String boneNameForBoxType(String type) {
        return switch (type) {
            case "HEAD" -> "head";
            case "BODY" -> "body";
            case "ARM0" -> "rightArm";
            case "ARM1" -> "leftArm";
            case "LEG0" -> "rightLeg";
            case "LEG1" -> "leftLeg";
            case "HEADWEAR" -> "hat";
            case "JACKET" -> "jacket";
            case "SLEEVE0" -> "rightSleeve";
            case "SLEEVE1" -> "leftSleeve";
            case "PANTS0" -> "rightPants";
            case "PANTS1" -> "leftPants";
            default -> "body";
        };
    }

    private static float defaultOverlayInflate(String type) {
        return switch (type) {
            case "HEADWEAR" -> 0.5f;
            case "JACKET", "SLEEVE0", "SLEEVE1", "PANTS0", "PANTS1" -> 0.25f;
            default -> 0.0f;
        };
    }

    private static boolean cubeSignatureEquals(JsonArray size, JsonArray uv, float sx, float sy, float sz, float ux, float uy) {
        return near(size.get(0).getAsFloat(), sx) && near(size.get(1).getAsFloat(), sy) && near(size.get(2).getAsFloat(), sz)
            && near(uv.get(0).getAsFloat(), ux) && near(uv.get(1).getAsFloat(), uy);
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }

    private static float parseFloatToken(String token) {
        return Float.parseFloat(token.indexOf(',') >= 0 ? token.replace(',', '.') : token);
    }

    private record PckBox(
        String type, float posX, float posY, float posZ,
        float sizeX, float sizeY, float sizeZ,
        float uvX, float uvY, boolean mirror, float scale
    ) {}
}